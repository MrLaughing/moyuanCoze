package com.mrlaughing.moyuan.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import com.mrlaughing.moyuan.data.mapper.EntityMapper
import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.PlantPath
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 同步 Worker
 *
 * 直接使用微信读书 API 返回的历史数据填充本地数据库，
 * 不再做增量计算。基准日期设为 2026-01-01。
 *
 * 数据流：
 * 1. overall → 总阅读时间、总阅读天数 → GardenMeta.accumulatedMinutes
 * 2. weekly  → 本周每天阅读时长 → daily_record 表
 * 3. shelf   → 书架书籍 → book_tracking 表
 * 4. 天气API → GardenMeta.currentWeather
 * 5. 花园引擎 → 植物解锁/生长/枯萎计算
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val KEY_ERROR_MSG = "error_msg"
    }

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

            // 预检：确认 Token 存在
            if (!wereadRepository.isAuthorized()) {
                Log.e(TAG, "同步失败：未授权，缺少微信读书Token")
                return Result.failure(workDataOf(KEY_ERROR_MSG to "未授权：请先在个人中心输入微信读书API Key"))
            }

            Log.d(TAG, "=== 开始同步 ===")

            // 1. 拉取 overall 数据
            val overallResult = wereadRepository.fetchReadDataOverall()
            if (overallResult.isFailure) {
                val err = overallResult.exceptionOrNull()?.message ?: "unknown"
                Log.e(TAG, "拉取总览数据失败: $err")
                return Result.failure(workDataOf(KEY_ERROR_MSG to "总览数据获取失败: $err"))
            }
            val overallData = overallResult.getOrNull()
            if (overallData == null || !overallData.isSuccess) {
                val errMsg = overallData?.errMsg ?: "unknown"
                Log.e(TAG, "总览数据API错误: $errMsg")
                return Result.failure(workDataOf(KEY_ERROR_MSG to "总览数据错误: $errMsg"))
            }

            val totalReadSeconds = overallData.totalReadTime
            val totalReadMinutes = (totalReadSeconds / 60).toInt()
            val totalReadDays = overallData.readDays
            Log.d(TAG, "总览: totalReadTime=${totalReadSeconds}s (${totalReadMinutes}min), readDays=${totalReadDays}")

            // 2. 拉取 weekly 数据
            val weeklyResult = wereadRepository.fetchReadDataWeekly()
            val weeklyData = weeklyResult.getOrNull()
            if (weeklyResult.isFailure) {
                Log.w(TAG, "拉取周数据失败: ${weeklyResult.exceptionOrNull()?.message}")
            }
            val todayReadSeconds = weeklyData?.getTodayReadSeconds() ?: 0L
            val todayReadMinutes = (todayReadSeconds / 60).toInt()
            Log.d(TAG, "今日阅读: ${todayReadSeconds}s (${todayReadMinutes}min)")

            // 3. 拉取书架
            val shelfResult = wereadRepository.fetchShelf()
            val shelfData = shelfResult.getOrNull()
            if (shelfResult.isFailure) {
                Log.w(TAG, "拉取书架失败: ${shelfResult.exceptionOrNull()?.message}")
            }
            val shelfBooks = shelfData?.books
                ?.sortedByDescending { it.readUpdateTime }
                ?: emptyList()
            Log.d(TAG, "书架: ${shelfBooks.size}本书")

            // 4. 写入每日记录（从 weekly readTimes 填充本周数据）
            saveWeeklyDailyRecords(readStatsRepository, weeklyData, todayReadMinutes)

            // 5. 写入书目追踪（取前10本）
            saveBookTracking(readStatsRepository, shelfBooks)

            // 6. 更新 GardenMeta（直接用历史数据，不做增量）
            val meta = gardenRepository.observeMeta().first()
            if (meta != null) {
                val isNightRead = LocalTime.now().hour >= 22 || LocalTime.now().hour < 6
                val hasReadToday = todayReadMinutes > 0
                val yesterdayStr = LocalDate.now().minusDays(1).toString()
                val lastSyncYesterday = meta.lastSyncDate == yesterdayStr
                val newStreakDays = when {
                    hasReadToday && (meta.lastSyncDate == LocalDate.now().toString()) -> meta.streakDays
                    hasReadToday && lastSyncYesterday -> meta.streakDays + 1
                    hasReadToday -> 1
                    else -> 0
                }

                gardenRepository.updateMeta(meta.copy(
                    accumulatedMinutes = totalReadMinutes,
                    todayReadMinutes = todayReadMinutes,
                    streakDays = newStreakDays,
                    maxStreakDays = maxOf(meta.maxStreakDays, newStreakDays),
                    booksRead = shelfBooks.size,
                    nightReadDays = if (isNightRead) meta.nightReadDays + 1 else meta.nightReadDays,
                    lastSyncDate = LocalDate.now().toString()
                ))
                Log.d(TAG, "GardenMeta已更新: accumulatedMinutes=$totalReadMinutes, booksRead=${shelfBooks.size}, streakDays=$newStreakDays")
            }

            // 7. 获取天气
            val season = com.mrlaughing.moyuan.engine.season.SeasonEngine.getSeason(LocalDate.now())
            val isNight = com.mrlaughing.moyuan.engine.season.SeasonEngine.isNightHour(java.time.LocalTime.now().hour)
            val realWeather = weatherRepository.fetchWeather(season, isNight)
            Log.d(TAG, "天气: ${realWeather.name}")

            // 8. 触发花园引擎（用今日阅读数据驱动植物状态）
            val gardenUpdateResult = triggerGardenEngine(
                gardenRepository, plantRepository, todayReadMinutes, shelfBooks.size, realWeather
            )
            gardenUpdateResult?.let {
                gardenRepository.updateWeather(it.weather.name, LocalDate.now().toString())
            }

            // 9. 首次同步时，用历史数据初始化已解锁植物的积累分钟数
            initializePlantAccumulatedMinutes(plantRepository, totalReadMinutes)

            notifyUIRefresh()
            Log.d(TAG, "=== 同步完成 ===")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步异常", e)
            Result.failure(workDataOf(KEY_ERROR_MSG to (e.message ?: "未知异常")))
        }
    }

    /**
     * 从 weekly readTimes 填充本周每天的阅读记录
     */
    private suspend fun saveWeeklyDailyRecords(
        readStatsRepository: ReadStatsRepository,
        weeklyData: com.mrlaughing.moyuan.data.remote.dto.ReadDataResponse?,
        todayReadMinutes: Int
    ) {
        val zoneId = ZoneId.systemDefault()
        val readTimes = weeklyData?.readTimes ?: emptyMap()

        // 从 readTimes 解析每天的数据
        val dailyData = mutableMapOf<LocalDate, Long>()
        for ((key, seconds) in readTimes) {
            val epochSecond = key.toLongOrNull() ?: continue
            val date = Instant.ofEpochSecond(epochSecond)
                .atZone(zoneId).toLocalDate()
            dailyData[date] = seconds
        }

        // 填充本周每一天
        val today = LocalDate.now()
        for (i in 0..6L) {
            val date = today.minusDays(i)
            val seconds = dailyData[date] ?: 0L
            val minutes = (seconds / 60).toInt()
            val isNightRead = i == 0L && (LocalTime.now().hour >= 22 || LocalTime.now().hour < 6)

            readStatsRepository.upsertDailyRecord(DailyRecordEntity(
                date = date.toString(),
                readMinutes = minutes,
                hasNightRead = isNightRead,
                newBookCount = 0,
                syncedAt = System.currentTimeMillis()
            ))
        }
        Log.d(TAG, "已填充本周每日记录: ${dailyData.size}天有数据, 今日${todayReadMinutes}分钟")
    }

    /**
     * 保存书架书籍到 book_tracking
     */
    private suspend fun saveBookTracking(
        readStatsRepository: ReadStatsRepository,
        shelfBooks: List<ShelfBook>
    ) {
        val recentBooks = shelfBooks.take(10)
        recentBooks.forEach { book ->
            val lastReadDate = if (book.readUpdateTime > 0) {
                Instant.ofEpochSecond(book.readUpdateTime)
                    .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            } else {
                LocalDate.now().toString()
            }
            readStatsRepository.upsertBookTracking(BookTrackingEntity(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                progressPercent = 0,
                readMinutes = 0,
                startDate = lastReadDate,
                lastReadDate = lastReadDate
            ))
        }
        Log.d(TAG, "已保存${recentBooks.size}本书目追踪")
    }

    private suspend fun triggerGardenEngine(
        gardenRepository: GardenRepository,
        plantRepository: PlantRepository,
        todayReadMinutes: Int,
        booksCount: Int,
        weather: com.mrlaughing.moyuan.data.model.Weather?
    ): GardenUpdateResult? {
        val today = LocalDate.now()
        val isNightRead = LocalTime.now().hour >= 22 || LocalTime.now().hour < 6

        val gardenState = gardenRepository.observeGardenState().first()
        val metaEntity = gardenState.meta ?: return null
        val plantEntities = gardenState.plants

        val engineMeta = EntityMapper.toEngineMeta(metaEntity)
        val enginePlants = plantEntities.map { EntityMapper.toEnginePlant(it) }

        val dailyInput = DailyReadInput(
            date = today,
            minutesRead = todayReadMinutes,
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

    /**
     * 首次同步时，用历史数据初始化已解锁植物的积累分钟数
     * 
     * 策略：对于首次同步（accumulatedMinutes == 0 的已解锁植物），
     * 根据总阅读时间和植物解锁阈值估算每株植物的积累分钟数。
     * 
     * 估算逻辑：
     * - 积墨路径：accumulatedMinutes ≈ totalReadMinutes - unlockThreshold
     * - 其他路径：accumulatedMinutes ≈ totalReadMinutes（因为每日阅读对所有植物灌溉）
     */
    private suspend fun initializePlantAccumulatedMinutes(
        plantRepository: PlantRepository,
        totalReadMinutes: Int
    ) {
        val plants = plantRepository.observePlants().first()
        var anyInitialized = false
        
        val updatedPlants = plants.map { plant ->
            // 只处理已解锁但 accumulatedMinutes 为 0 或很低的植物（首次同步标记）
            if (!plant.unlockDate.isNullOrEmpty() && plant.accumulatedMinutes < 60) {
                anyInitialized = true
                val plantDef = PlantDefinitions.getById(plant.plantId)
                val estimatedMinutes = if (plantDef != null && plantDef.path == PlantPath.JIMO) {
                    // 积墨路径：从解锁后开始积累
                    maxOf(0, totalReadMinutes - plantDef.unlockThreshold)
                } else {
                    // 其他路径：估算为总阅读时间的均等份额
                    totalReadMinutes
                }
                // 限制最大值避免超出合理范围
                val clampedMinutes = estimatedMinutes.coerceAtMost(totalReadMinutes)
                plant.copy(accumulatedMinutes = clampedMinutes, level = GrowthLevel.fromMinutes(clampedMinutes).level)
            } else {
                plant
            }
        }
        
        if (anyInitialized) {
            plantRepository.updatePlantAfterRecalculate(updatedPlants)
            Log.d(TAG, "已用历史数据初始化植物积累分钟数")
        }
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
