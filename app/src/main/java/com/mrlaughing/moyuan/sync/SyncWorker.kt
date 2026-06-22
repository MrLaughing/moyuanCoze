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
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.data.remote.dto.ShelfBook
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import com.mrlaughing.moyuan.data.repository.WeatherRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
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
import java.time.YearMonth
import java.time.ZoneId

/**
 * 同步 Worker
 *
 * 完整数据流：
 * 1. overall → 总阅读时间、总阅读天数 → GardenMeta.accumulatedMinutes
 * 2. weekly  → 本周每天阅读时长 → daily_record 表
 * 3. monthly → 历史每月阅读数据 → 补算 daily_record（source=backfill）
 * 4. shelf   → 书架书籍 → book_tracking 表
 * 5. 天气API → 历史天气 → daily_record.weather + GardenEngine.recalculate()
 * 6. 花园引擎 → 植物解锁/生长/枯萎计算
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val KEY_ERROR_MSG = "error_msg"
        
        // 补算起始日期
        private val BACKFILL_START_DATE = LocalDate.of(2026, 1, 1)
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

            // 5. 写入书目追踪（先写DB，UI可立即显示数据）（取前10本）
            saveBookTracking(readStatsRepository, shelfBooks, overallData?.readLongest ?: emptyList())

            // 6. 更新 GardenMeta 的非引擎字段（累计分钟由 API 总量覆盖，不依赖引擎增量计算）
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
                    // 注意：不在这里设置 accumulatedMinutes——引擎会做增量累加导致双重计算
                    // 引擎执行后，由 triggerGardenEngine 用 API 总量覆盖（见下文）
                    todayReadMinutes = todayReadMinutes,
                    streakDays = newStreakDays,
                    maxStreakDays = maxOf(meta.maxStreakDays, newStreakDays),
                    booksRead = shelfBooks.count { it.readUpdateTime > 0 },
                    nightReadDays = if (isNightRead) meta.nightReadDays + 1 else meta.nightReadDays,
                    lastSyncDate = LocalDate.now().toString()
                ))
                Log.d(TAG, "GardenMeta已更新: todayReadMinutes=$todayReadMinutes, booksRead=${shelfBooks.size}, streakDays=$newStreakDays")
            }

            // 7. 获取当前天气
            val season = com.mrlaughing.moyuan.engine.season.SeasonEngine.getSeason(LocalDate.now())
            val isNight = com.mrlaughing.moyuan.engine.season.SeasonEngine.isNightHour(java.time.LocalTime.now().hour)
            val realWeather = weatherRepository.fetchWeather(season, isNight)
            Log.d(TAG, "天气: ${realWeather.name}")

            // 8. 触发花园引擎（用今日阅读数据驱动植物状态）
            //   引擎内部会做增量累加 accumulatedMinutes += todayReadMinutes
            //   由于 API 总量已包含今日数据，我们在引擎内部用 API 总量覆盖
            val gardenUpdateResult = triggerGardenEngine(
                gardenRepository, plantRepository, todayReadMinutes, shelfBooks.size, realWeather,
                apiTotalMinutes = totalReadMinutes  // 传入API总量，引擎内部用于修正
            )
            gardenUpdateResult?.let {
                gardenRepository.updateWeather(it.weather.name, LocalDate.now().toString())
            }

            // 注：不再需要"修正accumulatedMinutes双重计算"步骤
            // 因为 triggerGardenEngine 内部已用 API 总量覆盖了引擎增量结果
            // 9. 首次同步时，用历史数据初始化已解锁植物的积累分钟数
            initializePlantAccumulatedMinutes(plantRepository, totalReadMinutes)

            // 10. 历史数据精确补算（放在最末尾执行，不阻塞主流程）
            try {
                performBackfill(
                    wereadRepository = wereadRepository,
                    readStatsRepository = readStatsRepository,
                    weatherRepository = weatherRepository,
                    gardenRepository = gardenRepository,
                    plantRepository = plantRepository,
                    totalReadMinutes = totalReadMinutes
                )
            } catch (e: Exception) {
                Log.w(TAG, "补算失败（不影响主流程）: ${e.message}")
            }

            notifyUIRefresh()
            Log.d(TAG, "=== 同步完成 ===")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步异常", e)
            Result.failure(workDataOf(KEY_ERROR_MSG to (e.message ?: "未知异常")))
        }
    }

    /**
     * 历史天气精确补算
     * 
     * 流程：
     * 1. 确定补算范围：从 installDate 或 2026-01-01 到 lastSyncDate 前一天
     * 2. 逐月调用微信读书 API 获取历史阅读数据
     * 3. 获取历史天气数据
     * 4. 逐日写入 DailyRecordEntity（source=backfill）
     * 5. 逐日调用 GardenEngine.recalculate() 精确计算植物状态
     */
    private suspend fun performBackfill(
        wereadRepository: WereadRepository,
        readStatsRepository: ReadStatsRepository,
        weatherRepository: WeatherRepository,
        gardenRepository: GardenRepository,
        plantRepository: PlantRepository,
        totalReadMinutes: Int
    ): Unit {
        try {
            val meta = gardenRepository.observeMeta().first() ?: return
            
            // 确定补算起始日期
            val installDate = meta.installDate?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            }
            val backfillStart = installDate?.let { if (it.isAfter(BACKFILL_START_DATE)) it else BACKFILL_START_DATE }
                ?: BACKFILL_START_DATE
            
            // 确定补算结束日期（昨天）
            val today = LocalDate.now()
            val backfillEnd = today.minusDays(1)
            
            if (backfillStart.isAfter(backfillEnd)) {
                Log.d(TAG, "无需补算：已同步到昨天")
                return
            }

            Log.d(TAG, "开始补算: $backfillStart ~ $backfillEnd")

            // 获取花园状态用于逐日重算
            val gardenState = gardenRepository.observeGardenState().first()
            val metaEntity = gardenState.meta ?: return
            var engineMeta = EntityMapper.toEngineMeta(metaEntity)
            var enginePlants = gardenState.plants.map { EntityMapper.toEnginePlant(it) }

            // 逐月获取历史阅读数据
            var currentDate = backfillStart
            while (!currentDate.isAfter(backfillEnd)) {
                val yearMonth = YearMonth.from(currentDate)
                
                // 调用 API 获取该月数据
                val baseTime = yearMonth.atDay(15)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond()
                
                val monthlyResult = wereadRepository.fetchReadDataMonthly(baseTime)
                if (monthlyResult.isSuccess) {
                    val monthlyReadTimes = monthlyResult.getOrNull()?.readTimes
                    
                    // 获取该月历史天气（按月获取避免API限制）
                    val monthStart = yearMonth.atDay(1)
                    val monthEnd = yearMonth.atEndOfMonth().coerceAtMost(backfillEnd)
                    val weatherMap = weatherRepository.fetchHistoricalWeather(monthStart, monthEnd)
                    
                    // 处理该月每一天
                    var day = yearMonth.atDay(1)
                    while (!day.isAfter(monthEnd)) {
                        // 检查是否已存在 sync 来源的记录
                        val existingRecord = readStatsRepository.observeRecordByDate(day.toString()).first()
                        if (existingRecord == null || existingRecord.source != "sync") {
                            // 从 API 数据获取阅读分钟
                            val epochSecond = day.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                            val seconds = monthlyReadTimes?.get(epochSecond.toString()) ?: 0L
                            val minutes = (seconds / 60).toInt()
                            
                            // 获取天气
                            val weather = weatherMap[day]
                            val weatherName = weather?.name
                            
                            // 写入记录
                            readStatsRepository.upsertDailyRecord(DailyRecordEntity(
                                date = day.toString(),
                                readMinutes = minutes,
                                hasNightRead = false, // 历史数据无法准确判断
                                newBookCount = 0,
                                syncedAt = System.currentTimeMillis(),
                                source = "backfill",
                                weather = weatherName
                            ))
                            
                            // 逐日重算花园
                            val dailyInput = DailyReadInput(
                                date = day,
                                minutesRead = minutes,
                                booksReadToday = 0,
                                isNightRead = false
                            )
                            
                            val updateResult = GardenEngine.recalculate(
                                meta = engineMeta,
                                plantStates = enginePlants,
                                dailyInput = dailyInput,
                                today = day,
                                weather = weather
                            )
                            
                            // 更新状态用于下一天计算
                            engineMeta = updateResult.meta
                            enginePlants = updateResult.plants
                            
                            // 持久化植物状态（每10天保存一次，避免频繁IO）
                            if (day.dayOfMonth % 10 == 0 || day == monthEnd) {
                                val updatedPlantEntities = enginePlants.mapIndexed { index, enginePlant ->
                                    val existingId = if (index < gardenState.plants.size) gardenState.plants[index].id else 0
                                    EntityMapper.toDbPlant(enginePlant, existingId)
                                }
                                plantRepository.updatePlantAfterRecalculate(updatedPlantEntities)
                            }
                        }
                        
                        day = day.plusDays(1)
                    }
                    
                    Log.d(TAG, "已完成 $yearMonth 补算")
                }
                
                // 移动到下一个月
                currentDate = yearMonth.plusMonths(1).atDay(1)
            }

            // 最终保存补算后的花园状态
            gardenRepository.updateMeta(EntityMapper.toDbMeta(engineMeta, metaEntity))
            val finalPlantEntities = enginePlants.mapIndexed { index, enginePlant ->
                val existingId = if (index < gardenState.plants.size) gardenState.plants[index].id else 0
                EntityMapper.toDbPlant(enginePlant, existingId)
            }
            plantRepository.updatePlantAfterRecalculate(finalPlantEntities)

            Log.d(TAG, "补算完成")
        } catch (e: Exception) {
            Log.e(TAG, "补算异常", e)
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
                syncedAt = System.currentTimeMillis(),
                source = "sync"
            ))
        }
        Log.d(TAG, "已填充本周每日记录: ${dailyData.size}天有数据, 今日${todayReadMinutes}分钟")
    }

    /**
     * 保存书架书籍到 book_tracking
     * 使用 readLongest 数据获取真实阅读时长
     */
    private suspend fun saveBookTracking(
        readStatsRepository: ReadStatsRepository,
        shelfBooks: List<ShelfBook>,
        readLongest: List<com.mrlaughing.moyuan.data.remote.dto.ReadLongestBook>
    ) {
        // 构建 readLongest 的 bookId→readTime 映射（秒→分钟）
        val readTimeMap = readLongest.mapNotNull { item ->
            item.book?.bookId?.let { bid -> bid to (item.readTime / 60).toInt() }
        }.toMap()

        // 保存最近阅读的书籍（有阅读记录的优先）
        val recentBooks = shelfBooks
            .filter { it.readUpdateTime > 0 }
            .sortedByDescending { it.readUpdateTime }
            .take(20)

        recentBooks.forEach { book ->
            val lastReadDate = Instant.ofEpochSecond(book.readUpdateTime)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()

            // 优先使用 readLongest 中的真实阅读时长，否则标记为0（待后续同步补全）
            val realMinutes = readTimeMap[book.bookId] ?: 0

            readStatsRepository.upsertBookTracking(BookTrackingEntity(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                progressPercent = if (book.finishReading == 1) 100 else 0,
                readMinutes = realMinutes,
                startDate = lastReadDate,
                lastReadDate = lastReadDate
            ))
        }
        Log.d(TAG, "已保存${recentBooks.size}本书目追踪（含${readTimeMap.size}本真实阅读时长）")
    }

    private suspend fun triggerGardenEngine(
        gardenRepository: GardenRepository,
        plantRepository: PlantRepository,
        todayReadMinutes: Int,
        booksCount: Int,
        weather: Weather?,
        apiTotalMinutes: Int  // API 返回的总累计分钟数（含今日），用于覆盖引擎增量计算的结果
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

        // 引擎内部将 accumulatedMinutes += todayReadMinutes，但 API 总量已包含今日数据
        // 因此以 API 总量为 ground truth 覆盖引擎计算的累计分钟
        val correctedMeta = EntityMapper.toDbMeta(updateResult.meta, metaEntity).copy(
            accumulatedMinutes = apiTotalMinutes
        )
        gardenRepository.updateMeta(correctedMeta)
        
        val updatedPlants = updateResult.plants.mapIndexed { index, enginePlant ->
            val existingId = if (index < plantEntities.size) plantEntities[index].id else 0
            EntityMapper.toDbPlant(enginePlant, existingId)
        }
        plantRepository.updatePlantAfterRecalculate(updatedPlants)

        return updateResult
    }

    /**
     * 首次同步时，用历史数据初始化已解锁植物的积累分钟数
     */
    private suspend fun initializePlantAccumulatedMinutes(
        plantRepository: PlantRepository,
        totalReadMinutes: Int
    ) {
        val plants = plantRepository.observePlants().first()
        var anyInitialized = false
        
        val updatedPlants = plants.map { plant ->
            if (!plant.unlockDate.isNullOrEmpty() && plant.accumulatedMinutes < 60) {
                anyInitialized = true
                val plantDef = PlantDefinitions.getById(plant.plantId)
                val estimatedMinutes = if (plantDef != null && plantDef.path == PlantPath.JIMO) {
                    maxOf(0, totalReadMinutes - plantDef.unlockThreshold)
                } else {
                    totalReadMinutes
                }
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



