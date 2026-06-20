package com.mrlaughing.moyuan.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * 同步 Worker：拉取微信读书数据 → 计算增量 → 写DB → 触发引擎 → 通知UI
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Step 1: 拉取微信读书数据
            val wereadData = fetchWereadData()
                ?: return Result.failure(workDataOf("error" to "fetch_failed"))

            // Step 2: 检查是否有快照
            val snapshotManager = SnapshotManager.getInstance(applicationContext)
            if (!snapshotManager.hasSnapshot()) {
                // 首次同步：创建基础快照
                snapshotManager.createBaseSnapshot(wereadData)
            }

            // Step 3: 计算增量
            val increment = snapshotManager.calculateIncrement(
                wereadData,
                snapshotManager.loadBaseSnapshot()
            )

            // Step 4: 写入数据库
            val mapper = WereadDataMapper()
            val readStats = mapper.toReadStats(increment)
            val bookEntities = mapper.toBookTrackingEntities(increment.books)
            val dailyRecord = mapper.toDailyRecordEntity(increment)

            // 写入数据库（通过 Repository，此处简化为直接通知）
            // 实际项目中应注入 Repository
            saveToDatabase(readStats, bookEntities, dailyRecord)

            // Step 5: 触发花园引擎计算（等级升降、枯萎等）
            triggerGardenEngine()

            // Step 6: 通知 UI 刷新
            notifyUIRefresh()

            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "unknown")))
        }
    }

    /**
     * 拉取微信读书数据
     * 实际实现需要调用 WereadRepository 的 API
     */
    private suspend fun fetchWereadData(): WereadSyncData? {
        // TODO: 接入微信读书 API
        // WereadRepository.fetchUserData()
        return null
    }

    private suspend fun saveToDatabase(
        readStats: Any,
        bookEntities: List<Any>,
        dailyRecord: Any
    ) {
        // TODO: 通过 Room DAO 写入
    }

    private suspend fun triggerGardenEngine() {
        // TODO: 调用 GardenEngine.process()
    }

    private fun notifyUIRefresh() {
        // 发送广播通知 UI 层刷新
        val intent = android.content.Intent("com.mrlaughing.moyuan.ACTION_SYNC_COMPLETE")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }
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
