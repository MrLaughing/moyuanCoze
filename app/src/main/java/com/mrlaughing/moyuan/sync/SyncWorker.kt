package com.mrlaughing.moyuan.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import com.mrlaughing.moyuan.data.mapper.EntityMapper
import com.mrlaughing.moyuan.data.remote.dto.ShelfBook
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import com.mrlaughing.moyuan.data.repository.WeatherRepository
import com.mrlaughing.moyuan.engine.DailyReadInput
import com.mrlaughing.moyuan.engine.GardenEngine
import com.mrlaughing.moyuan.engine.GardenUpdateResult
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext, SyncWorkerEntryPoint::class.java
            )
            val wereadRepository = entryPoint.wereadRepository()
            val gardenRepository = entryPoint.gardenRepository()
            val plantRepository = entryPoint.plantRepository()
            val readStatsRepository = entryPoint.readStatsRepository()
            val weatherRepository = entryPoint.weatherRepository()

            // 拉取阅读数据（weekly 获取今日数据，overall 获取总览）
            val weeklyResult = wereadRepository.fetchReadDataWeekly()
            val overallResult = wereadRepository.fetchReadDataOverall()
            val weeklyData = weeklyResult.getOrNull()
            val overallData = overallResult.getOrNull()

            if (overallData == null || !overallData.isSuccess) {
                val errMsg = overallResult.exceptionOrNull()?.message ?: "unknown"
                return Result.failure(workDataOf("error" to "fetch_failed: $errMsg"))
            }

            // 拉取书架
            val shelfData = wereadRepository.fetchShelf().getOrNull()

            // 计算今日阅读秒数
            val todayReadSeconds = weeklyData?.getTodayReadSeconds() ?: 0L
            val totalReadSeconds = overallData.totalReadTime

            // 获取书架书籍并按最近阅读时间降序排序
            val shelfBooks = shelfData?.books
                ?.sortedByDescending { it.readUpdateTime }
                ?: emptyList()

            // 构建同步数据
            val wereadSyncData = WereadSyncData(
                totalReadTime = totalReadSeconds,
                todayReadTime = todayReadSeconds,
                streakDays = 0,
                books = shelfBooks.map { it.toWereadBookData() },
                syncTimestamp = System.currentTimeMillis()
            )

            val snapshotManager = SnapshotManager.getInstance(applicationContext)
            if (!snapshotManager.hasSnapshot()) {
                snapshotManager.createBaseSnapshot(wereadSyncData)
            }

            val increment = snapshotManager.calculateIncrement(
                wereadSyncData,
                snapshotManager.loadBaseSnapshot()
            )

            // 写入数据库
            saveToDatabase(readStatsRepository, increment, todayReadSeconds)

            // 触发花园引擎（先获取真实天气）
            val season = com.mrlaughing.moyuan.engine.season.SeasonEngine.getSeason(LocalDate.now())
            val isNight = com.mrlaughing.moyuan.engine.season.SeasonEngine.isNightHour(java.time.LocalTime.now().hour)
            val realWeather = weatherRepository.fetchWeather(season, isNight)

            val gardenUpdateResult = triggerGardenEngine(
                gardenRepository, plantRepository, todayReadSeconds, shelfBooks.size, increment, realWeather
            )

            gardenUpdateResult?.let {
                gardenRepository.updateWeather(it.weather.name, LocalDate.now().toString())
            }

            gardenRepository.observeMeta().first()?.let { meta ->
                gardenRepository.updateMeta(meta.copy(lastSyncDate = LocalDate.now().toString()))
            }

            notifyUIRefresh()
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "unknown")))
        }
    }

    private fun ShelfBook.toWereadBookData() = WereadBookData(
        bookId = bookId,
        title = title,
        author = author,
        totalReadTime = 0,
        lastReadDate = if (readUpdateTime > 0) {
            java.time.Instant.ofEpochSecond(readUpdateTime)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
        } else LocalDate.now().toString(),
        coverUrl = cover
    )

    private suspend fun saveToDatabase(
        readStatsRepository: ReadStatsRepository,
        increment: IncrementData,
        todayReadSeconds: Long
    ) {
        val today = LocalDate.now().toString()
        val todayMinutes = (todayReadSeconds / 60).toInt()
        val isNightRead = LocalTime.now().hour >= 22 || LocalTime.now().hour < 6

        readStatsRepository.upsertDailyRecord(DailyRecordEntity(
            date = today,
            readMinutes = todayMinutes,
            hasNightRead = isNightRead,
            newBookCount = increment.books.size,
            syncedAt = System.currentTimeMillis()
        ))

        // 按最近阅读时间排序后取前10本保存
        val recentBooks = increment.books
            .sortedByDescending { it.lastReadDate }
            .take(10)

        recentBooks.forEach { book ->
            readStatsRepository.upsertBookTracking(BookTrackingEntity(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                progressPercent = 0,
                readMinutes = (book.totalReadTime / 60).toInt(),
                startDate = book.lastReadDate,
                lastReadDate = book.lastReadDate
            ))
        }
    }

    private suspend fun triggerGardenEngine(
        gardenRepository: GardenRepository,
        plantRepository: PlantRepository,
        todayReadSeconds: Long,
        booksCount: Int,
        increment: IncrementData,
        weather: com.mrlaughing.moyuan.data.model.Weather?
    ): GardenUpdateResult? {
        val today = LocalDate.now()
        val todayMinutes = (todayReadSeconds / 60).toInt()
        val isNightRead = LocalTime.now().hour >= 22 || LocalTime.now().hour < 6

        val gardenState = gardenRepository.observeGardenState().first()
        val metaEntity = gardenState.meta ?: return null
        val plantEntities = gardenState.plants

        val engineMeta = EntityMapper.toEngineMeta(metaEntity)
        val enginePlants = plantEntities.map { EntityMapper.toEnginePlant(it) }

        val dailyInput = DailyReadInput(
            date = today,
            minutesRead = todayMinutes,
            booksReadToday = booksCount,
            isNightRead = isNightRead
        )

        val updateResult = GardenEngine.recalculate(
            meta = engineMeta,
            plantStates = enginePlants,
            dailyInput = dailyInput,
            today = today,
            weather = weather
        )

        gardenRepository.updateMeta(EntityMapper.toDbMeta(updateResult.meta, metaEntity))
        val updatedPlants = updateResult.plants.mapIndexed { index, enginePlant ->
            val existingId = if (index < plantEntities.size) plantEntities[index].id else 0
            EntityMapper.toDbPlant(enginePlant, existingId)
        }
        plantRepository.updatePlantAfterRecalculate(updatedPlants)

        return updateResult
    }

    private fun notifyUIRefresh() {
        val intent = android.content.Intent("com.mrlaughing.moyuan.ACTION_SYNC_COMPLETE")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun wereadRepository(): WereadRepository
    fun gardenRepository(): GardenRepository
    fun plantRepository(): PlantRepository
    fun readStatsRepository(): ReadStatsRepository
    fun weatherRepository(): WeatherRepository
}

data class WereadSyncData(
    val totalReadTime: Long,
    val todayReadTime: Long,
    val streakDays: Int,
    val books: List<WereadBookData>,
    val syncTimestamp: Long
)

data class WereadBookData(
    val bookId: String,
    val title: String,
    val author: String,
    val totalReadTime: Long,
    val lastReadDate: String,
    val coverUrl: String?
)

data class IncrementData(
    val deltaReadTime: Long,
    val todayReadTime: Long,
    val streakDays: Int,
    val books: List<WereadBookData>,
    val date: String
)
