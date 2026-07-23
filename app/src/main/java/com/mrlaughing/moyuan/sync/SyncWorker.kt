package com.mrlaughing.moyuan.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import com.mrlaughing.moyuan.data.mapper.EntityMapper
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.remote.dto.ShelfBook
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import com.mrlaughing.moyuan.data.repository.WeatherRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import com.mrlaughing.moyuan.engine.DailyReadInput
import com.mrlaughing.moyuan.engine.GardenEngine
import com.mrlaughing.moyuan.engine.GardenUpdateResult
import com.mrlaughing.moyuan.ui.MainActivity
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
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
 * 6. 花园引擎 → 植物解锁（随机/每阈值一株）计算
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val KEY_ERROR_MSG = "error_msg"
        const val KEY_NEW_READ_MINUTES = "new_read_minutes"
        const val KEY_NEW_PLANT_COUNT = "new_plant_count"

        // 新植物通知渠道与 ID
        private const val CHANNEL_ID_GARDEN = "channel_garden_dynamic"
        private const val NOTIFICATION_ID_NEW_PLANT = 1001

        // 补算起始日期
        private val BACKFILL_START_DATE = LocalDate.of(2026, 1, 1)
        private val syncMutex = Mutex()
    }

    override suspend fun doWork(): Result = syncMutex.withLock {
        performSync()
    }

    private suspend fun performSync(): Result {
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
                return retryOrFail(
                    message = "总览数据获取失败: $err",
                    cause = overallResult.exceptionOrNull()
                )
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

            val today = LocalDate.now()
            val metaBeforeSync = gardenRepository.observeMeta().first()
            val previousTodayMinutes = if (metaBeforeSync?.lastSyncDate == today.toString()) {
                metaBeforeSync.todayReadMinutes
            } else {
                0
            }
            val newlyObservedMinutes = (todayReadMinutes - previousTodayMinutes).coerceAtLeast(0)
            val isNightReadNow = LocalTime.now().hour >= 22 || LocalTime.now().hour < 6
            val shouldCountNight = isNightReadNow &&
                newlyObservedMinutes > 0 &&
                metaBeforeSync?.lastNightReadDate != today.toString()

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
            saveWeeklyDailyRecords(
                readStatsRepository = readStatsRepository,
                weeklyData = weeklyData,
                todayReadMinutes = todayReadMinutes,
                hasNewNightRead = shouldCountNight
            )

            // 5. 写入书目追踪（先写DB，UI可立即显示数据）（取前10本）
            saveBookTracking(readStatsRepository, shelfBooks, overallData.readLongest)

            // 5.1 获取最近书目真实阅读进度（仅前10本，避免API压力）
            fetchBookProgress(readStatsRepository, wereadRepository, shelfBooks)

            // 6. 更新 GardenMeta 的非引擎字段（累计分钟由 API 总量覆盖，不依赖引擎增量计算）
            val meta = metaBeforeSync
            if (meta != null) {
                val hasReadToday = todayReadMinutes > 0

                gardenRepository.updateMeta(meta.copy(
                    // 注意：不在这里设置 accumulatedMinutes——引擎会做增量累加导致双重计算
                    // 引擎执行后，由 triggerGardenEngine 用 API 总量覆盖（见下文）
                    // 注意：不在这里设置 streakDays 和 booksRead——引擎会重新计算，
                    // 在此设置会导致引擎结果覆盖时 Room 发射两次，UI 闪烁
                    todayReadMinutes = todayReadMinutes,
                    nightReadDays = if (shouldCountNight) meta.nightReadDays + 1 else meta.nightReadDays,
                    lastNightReadDate = if (shouldCountNight) today.toString() else meta.lastNightReadDate,
                    // 总阅读天数：以微信读书 API 返回的 readDays 为权威值（含全部历史），覆盖本地值
                    totalReadDays = totalReadDays,
                    lastSyncDate = today.toString()
                ))
                Log.d(TAG, "GardenMeta基础信息已更新: todayReadMinutes=$todayReadMinutes, hasReadToday=$hasReadToday, shouldCountNight=$shouldCountNight")
            }

            // 7. 获取并持久化当前天气，网络失败时 WeatherRepository 会回退到 CLEAR
            val currentWeather = weatherRepository.fetchWeather()
            gardenRepository.updateWeather(currentWeather.name, today.toString())

            // 8. 触发花园引擎（用今日阅读数据驱动植物解锁）
            //   引擎内部会做增量累加 accumulatedMinutes += todayReadMinutes
            //   由于 API 总量已包含今日数据，我们在引擎内部用 API 总量覆盖
            val booksReadFromShelf = shelfBooks.count { it.readUpdateTime > 0 }
            val gardenUpdateResult = triggerGardenEngine(
                gardenRepository, plantRepository, todayReadMinutes,
                apiTotalMinutes = totalReadMinutes,
                booksReadFromShelf = booksReadFromShelf  // 传递书架准确总数，引擎运行后覆盖
            )

            // 9. 历史数据精确补算（放在最末尾执行，不阻塞主流程）
            try {
                performBackfill(
                    wereadRepository = wereadRepository,
                    readStatsRepository = readStatsRepository,
                    weatherRepository = weatherRepository,
                    gardenRepository = gardenRepository,
                    apiTotalMinutes = totalReadMinutes
                )
            } catch (e: Exception) {
                Log.w(TAG, "补算失败（不影响主流程）: ${e.message}")
            }

            notifyUIRefresh(gardenUpdateResult?.newlyUnlocked.orEmpty())
            Log.d(TAG, "=== 同步完成 ===")
            Result.success(
                workDataOf(
                    KEY_NEW_READ_MINUTES to newlyObservedMinutes,
                    KEY_NEW_PLANT_COUNT to gardenUpdateResult?.newlyUnlocked.orEmpty().size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "同步异常", e)
            retryOrFail(e.message ?: "未知异常", e)
        }
    }

    private fun retryOrFail(message: String, cause: Throwable?): Result {
        val isNetworkFailure = cause is IOException
        return if (isNetworkFailure && runAttemptCount < 3) {
            Result.retry()
        } else {
            Result.failure(workDataOf(KEY_ERROR_MSG to message))
        }
    }

    /**
     * 历史天气精确补算
     *
     * 作用：补全日历/天气热力图所需的 daily_record（installDate ~ 昨天）。
     *
     * 注意：植物解锁进度不再在此处逐日累加计算——
     * 解锁以 API 返回的累计总量（apiTotalMinutes）为唯一权威来源，
     * 由 triggerGardenEngine 统一计算（见 [triggerGardenEngine]），
     * 避免与 triggerGardenEngine 重复累加导致"第二次同步一次性解锁全部"。
     */
    private suspend fun performBackfill(
        wereadRepository: WereadRepository,
        readStatsRepository: ReadStatsRepository,
        weatherRepository: WeatherRepository,
        gardenRepository: GardenRepository,
        apiTotalMinutes: Int
    ): Unit {
        try {
            val meta = gardenRepository.observeMeta().first() ?: return

            // 确定补算起始日期
            val installDate = meta.installDate.let {
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

                            // 写入记录（仅补全日历/天气热力图，不影响解锁进度）
                            readStatsRepository.upsertDailyRecord(DailyRecordEntity(
                                date = day.toString(),
                                readMinutes = minutes,
                                hasNightRead = false, // 历史数据无法准确判断
                                newBookCount = 0,
                                syncedAt = System.currentTimeMillis(),
                                source = "backfill",
                                weather = weatherName
                            ))
                        }

                        day = day.plusDays(1)
                    }

                    Log.d(TAG, "已完成 $yearMonth 补算")
                }

                // 移动到下一个月
                currentDate = yearMonth.plusMonths(1).atDay(1)
            }

            // 累计分钟以 API 总量为准（triggerGardenEngine 已据此计算解锁）
            gardenRepository.updateMeta(meta.copy(accumulatedMinutes = apiTotalMinutes))

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
        todayReadMinutes: Int,
        hasNewNightRead: Boolean
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
            val existingRecord = readStatsRepository.observeRecordByDate(date.toString()).first()
            val isNightRead = existingRecord?.hasNightRead == true || (i == 0L && hasNewNightRead)

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
     * 获取最近书籍的真实阅读进度（/book/getprogress）
     * 替换原本只能区分"读完/未读完"的粗略标记
     */
    private suspend fun fetchBookProgress(
        readStatsRepository: ReadStatsRepository,
        wereadRepository: WereadRepository,
        shelfBooks: List<ShelfBook>
    ) {
        val recentBooksWithRead = shelfBooks
            .filter { it.readUpdateTime > 0 }
            .sortedByDescending { it.readUpdateTime }
            .take(10)

        recentBooksWithRead.forEach { book ->
            try {
                val result = wereadRepository.fetchBookProgress(book.bookId)
                if (result.isSuccess) {
                    val progress = result.getOrNull()
                    val progressPercent = progress?.progress?.progress?.coerceIn(0, 100) ?: 0
                    readStatsRepository.updateBookProgress(book.bookId, progressPercent)
                    Log.d(TAG, "获取进度成功: ${book.title} → ${progressPercent}%")
                }
            } catch (e: Exception) {
                Log.w(TAG, "获取进度失败: ${book.title} - ${e.message}")
            }
        }
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
        apiTotalMinutes: Int,         // API 返回的总累计分钟数（含今日），用于覆盖引擎增量计算
        booksReadFromShelf: Int = 0   // 书架中有阅读记录的书总数，引擎运行后覆盖 booksRead
    ): GardenUpdateResult? {
        val today = LocalDate.now()
        val isNightRead = LocalTime.now().hour >= 22 || LocalTime.now().hour < 6

        val gardenState = gardenRepository.observeGardenState().first()
        val metaEntity = gardenState.meta ?: return null
        val plantEntities = gardenState.plants

        // 引擎会把今日阅读量加到累计值，因此基线必须先减去今日分钟。
        // 这样解锁判断使用的正好是 API 返回的权威总量，不会把今日数据重复计算。
        val engineMeta = EntityMapper.toEngineMeta(metaEntity).copy(
            accumulatedMinutes = (apiTotalMinutes - todayReadMinutes).coerceAtLeast(0)
        )
        val enginePlants = plantEntities.map { EntityMapper.toEnginePlant(it) }

        // booksReadToday 传 0：我们不掌握每日新书数据，引擎内部 booksRead 由下方 booksReadFromShelf 覆盖
        val dailyInput = DailyReadInput(
            date = today,
            minutesRead = todayReadMinutes,
            booksReadToday = 0,
            isNightRead = isNightRead
        )

        val updateResult = GardenEngine.recalculate(
            meta = engineMeta,
            plantStates = enginePlants,
            dailyInput = dailyInput,
            today = today
        )

        // 以 API 总量覆盖引擎的累计分钟，以书架准确书数覆盖引擎的 booksRead
        // streakDays 保留引擎计算值（它是正确的）
        val correctedMeta = EntityMapper.toDbMeta(updateResult.meta, metaEntity).copy(
            accumulatedMinutes = apiTotalMinutes,
            booksRead = booksReadFromShelf
        )
        gardenRepository.updateMeta(correctedMeta)

        val updatedPlants = updateResult.plants.mapIndexed { index, enginePlant ->
            val existing = if (index < plantEntities.size) plantEntities[index] else PlantStateEntity(plantId = enginePlant.plantId)
            EntityMapper.toDbPlant(enginePlant, existing)
        }
        plantRepository.updatePlantAfterRecalculate(updatedPlants)

        return updateResult
    }

    private fun notifyUIRefresh(newlyUnlocked: List<com.mrlaughing.moyuan.data.model.Plant> = emptyList()) {
        // 内部广播：通知前台 UI 刷新
        val intent = android.content.Intent("com.mrlaughing.moyuan.ACTION_SYNC_COMPLETE")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)

        // 有新增植物时，发出系统通知（正向反馈：发现新物种）
        if (newlyUnlocked.isNotEmpty()) {
            showNewPlantNotification(newlyUnlocked)
        }
    }

    private fun showNewPlantNotification(newlyUnlocked: List<com.mrlaughing.moyuan.data.model.Plant>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID_GARDEN,
            applicationContext.getString(R.string.notification_channel_garden),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.notification_channel_garden_description)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)

        val names = newlyUnlocked.joinToString("、") { it.name }
        val title = if (newlyUnlocked.size == 1) "发现新物种" else "发现 ${newlyUnlocked.size} 株新植物"
        val content = if (newlyUnlocked.size == 1) "「$names」已加入你的图鉴" else "「$names」已加入你的图鉴"

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_GARDEN)
            .setSmallIcon(R.drawable.ic_nav_garden)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_NEW_PLANT, notification)
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
