package com.mrlaughing.moyuan.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import com.mrlaughing.moyuan.data.mapper.EntityMapper
import com.mrlaughing.moyuan.data.remote.dto.ReadDataResponse
import com.mrlaughing.moyuan.data.remote.dto.ShelfResponse
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import com.mrlaughing.moyuan.engine.DailyReadInput
import com.mrlaughing.moyuan.engine.GardenEngine
import com.mrlaughing.moyuan.engine.GardenUpdateResult
import com.mrlaughing.moyuan.sync.SnapshotManager
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * 同步 Worker：拉取微信读书数据 → 计算增量 → 写DB → 触发引擎 → 通知UI
 *
 * 使用 Hilt EntryPoint 获取依赖，无需 @AssistedInject
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Step 1: 通过 EntryPoint 获取依赖
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext, SyncWorkerEntryPoint::class.java
            )
            val wereadRepository = entryPoint.wereadRepository()
            val gardenRepository = entryPoint.gardenRepository()
            val plantRepository = entryPoint.plantRepository()
            val readStatsRepository = entryPoint.readStatsRepository()


            // Step 2: 拉取微信读书数据
            val readData = wereadRepository.fetchReadData().getOrNull()
            val shelfData = wereadRepository.fetchShelf().getOrNull()

            if (readData == null) {
                return Result.failure(workDataOf("error" to "fetch_failed"))
            }

            // Step 3: 构建 WereadSyncData
            val wereadSyncData = buildWereadSyncData(readData, shelfData)

            // Step 4: 检查是否有快照
            val snapshotManager = SnapshotManager.getInstance(applicationContext)
            if (!snapshotManager.hasSnapshot()) {
                // 首次同步：创建基础快照
                snapshotManager.createBaseSnapshot(wereadSyncData)
            }

            // Step 5: 计算增量
            val increment = snapshotManager.calculateIncrement(
                wereadSyncData,
                snapshotManager.loadBaseSnapshot()
            )

            // Step 6: 写入数据库
            saveToDatabase(readStatsRepository, increment)

            // Step 7: 触发花园引擎计算（等级升降、枯萎等）
            val gardenUpdateResult = triggerGardenEngine(
                gardenRepository = gardenRepository,
                plantRepository = plantRepository,
                increment = increment
            )

            // Step 8: 更新天气（如果需要）
            if (gardenUpdateResult != null) {
                gardenRepository.updateWeather(
                    gardenUpdateResult.weather.name,
                    LocalDate.now().toString()
                )
            }

            // Step 9: 更新最后同步日期
            gardenRepository.observeMeta().first()?.let { meta ->
                gardenRepository.updateMeta(
                    meta.copy(lastSyncDate = LocalDate.now().toString())
                )
            }

            // Step 10: 通知 UI 刷新
            notifyUIRefresh()

            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "unknown")))
        }
    }

    /**
     * 构建 WereadSyncData
     */
    private fun buildWereadSyncData(
        readData: ReadDataResponse,
        shelfData: ShelfResponse?
    ): WereadSyncData {
        val dto = readData.data ?: return WereadSyncData(
            totalReadTime = 0, todayReadTime = 0, streakDays = 0,
            books = emptyList(), syncTimestamp = System.currentTimeMillis()
        )
        val todayMinutes = (dto.readTime / 60).toInt()
        val books = shelfData?.data?.books?.map { book ->
            WereadBookData(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                totalReadTime = book.readTime,
                lastReadDate = book.lastReadDate ?: LocalDate.now().toString(),
                coverUrl = book.cover
            )
        } ?: emptyList()

        return WereadSyncData(
            totalReadTime = dto.readTime,
            todayReadTime = todayMinutes.toLong() * 60,
            streakDays = dto.currentStreakDays,
            books = books,
            syncTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * 写入数据库
     */
    private suspend fun saveToDatabase(
        readStatsRepository: ReadStatsRepository,
        increment: IncrementData
    ) {
        val today = LocalDate.now().toString()
        val todayMinutes = (increment.todayReadTime / 60).toInt()
        val isNightRead = isNightTime()

        // 写入每日记录
        val dailyRecord = DailyRecordEntity(
            date = today,
            readMinutes = todayMinutes,
            hasNightRead = isNightRead,
            newBookCount = increment.books.size,
            syncedAt = System.currentTimeMillis()
        )
        readStatsRepository.upsertDailyRecord(dailyRecord)

        // 写入书目追踪
        increment.books.forEach { book ->
            val bookEntity = BookTrackingEntity(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                progressPercent = 0, // API 未提供进度，使用默认值
                readMinutes = (book.totalReadTime / 60).toInt(),
                startDate = book.lastReadDate,
                lastReadDate = book.lastReadDate
            )
            readStatsRepository.upsertBookTracking(bookEntity)
        }
    }

    /**
     * 触发花园引擎计算
     */
    private suspend fun triggerGardenEngine(
        gardenRepository: GardenRepository,
        plantRepository: PlantRepository,
        increment: IncrementData
    ): GardenUpdateResult? {
        val today = LocalDate.now()
        val todayMinutes = (increment.todayReadTime / 60).toInt()
        val isNightRead = isNightTime()

        // 从 DB 读取当前状态
        val gardenState = gardenRepository.observeGardenState().first()
        val metaEntity = gardenState.meta ?: return null
        val plantEntities = gardenState.plants

        // EntityMapper 转 Engine 类型
        val engineMeta = EntityMapper.toEngineMeta(metaEntity)
        val enginePlants = plantEntities.map { EntityMapper.toEnginePlant(it) }

        // 构建每日输入
        val dailyInput = DailyReadInput(
            date = today,
            minutesRead = todayMinutes,
            booksReadToday = increment.books.size,
            isNightRead = isNightRead
        )

        // 调用 GardenEngine.process()
        val updateResult = GardenEngine.recalculate(
            meta = engineMeta,
            plantStates = enginePlants,
            dailyInput = dailyInput,
            today = today
        )

        // EntityMapper 转回 DB 类型并写回
        val updatedMeta = EntityMapper.toDbMeta(updateResult.meta, metaEntity)
        gardenRepository.updateMeta(updatedMeta)

        // 更新植物状态
        val updatedPlants = updateResult.plants.mapIndexed { index, enginePlant ->
            val existingId = if (index < plantEntities.size) plantEntities[index].id else 0
            EntityMapper.toDbPlant(enginePlant, existingId)
        }
        plantRepository.updatePlantAfterRecalculate(updatedPlants)

        return updateResult
    }

    /**
     * 判断是否夜间阅读
     */
    private fun isNightTime(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= 22 || hour < 6
    }

    private fun notifyUIRefresh() {
        // 发送广播通知 UI 层刷新
        val intent = android.content.Intent("com.mrlaughing.moyuan.ACTION_SYNC_COMPLETE")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }
}

/**
 * Hilt 入口点（必须为顶层，KSP 不支持嵌套 @EntryPoint）
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun wereadRepository(): WereadRepository
    fun gardenRepository(): GardenRepository
    fun plantRepository(): PlantRepository
    fun readStatsRepository(): ReadStatsRepository
}

/**
 * 微信读书同步数据 DTO
 */
data class WereadSyncData(
    val totalReadTime: Long,          // 总阅读时长(秒)
    val todayReadTime: Long,          // 今日阅读时长(秒)
    val streakDays: Int,              // 连续天数
    val books: List<WereadBookData>,  // 书目列表
    val syncTimestamp: Long           // 同步时间戳
)

/**
 * 微信读书书目数据
 */
data class WereadBookData(
    val bookId: String,
    val title: String,
    val author: String,
    val totalReadTime: Long,          // 累计阅读时长(秒)
    val lastReadDate: String,         // 最后阅读日期
    val coverUrl: String?
)

/**
 * 增量数据
 */
data class IncrementData(
    val deltaReadTime: Long,          // 增量阅读时长(秒)
    val todayReadTime: Long,
    val streakDays: Int,
    val books: List<WereadBookData>,
    val date: String                  // 日期 YYYY-MM-DD
)
