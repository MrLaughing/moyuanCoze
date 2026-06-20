package com.mrlaughing.moyuan.sync

import android.content.Context
import android.content.SharedPreferences

/**
 * 快照管理器：管理安装基准快照和增量计算
 */
class SnapshotManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("snapshot_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        @Volatile
        private var instance: SnapshotManager? = null

        fun getInstance(context: Context): SnapshotManager {
            return instance ?: synchronized(this) {
                instance ?: SnapshotManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 创建安装基准快照
     * 首次同步时调用，记录当前微信读书的全部数据作为基准
     */
    fun createBaseSnapshot(wereadData: WereadSyncData) {
        prefs.edit()
            .putLong("base_total_read_time", wereadData.totalReadTime)
            .putInt("base_streak_days", wereadData.streakDays)
            .putLong("base_sync_timestamp", wereadData.syncTimestamp)
            .putBoolean("has_snapshot", true)
            .apply()

        // 保存基准书目数据为 JSON
        val booksJson = wereadData.books.joinToString(";") { book ->
            "${book.bookId}|${book.title}|${book.author}|${book.totalReadTime}|${book.lastReadDate}"
        }
        prefs.edit().putString("base_books", booksJson).apply()
    }

    /**
     * 计算增量：当前数据 - 基准快照
     */
    fun calculateIncrement(current: WereadSyncData, base: WereadSyncData?): IncrementData {
        val baseTotal = base?.totalReadTime ?: 0L
        val deltaReadTime = maxOf(0L, current.totalReadTime - baseTotal)

        return IncrementData(
            deltaReadTime = deltaReadTime,
            todayReadTime = current.todayReadTime,
            streakDays = current.streakDays,
            books = current.books,
            date = java.time.LocalDate.now().toString()
        )
    }

    /**
     * 加载基准快照
     */
    fun loadBaseSnapshot(): WereadSyncData? {
        if (!hasSnapshot()) return null

        val totalReadTime = prefs.getLong("base_total_read_time", 0L)
        val streakDays = prefs.getInt("base_streak_days", 0)
        val syncTimestamp = prefs.getLong("base_sync_timestamp", 0L)
        val booksRaw = prefs.getString("base_books", "") ?: ""

        val books = if (booksRaw.isNotEmpty()) {
            booksRaw.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 5) {
                    WereadBookData(
                        bookId = parts[0],
                        title = parts[1],
                        author = parts[2],
                        totalReadTime = parts[3].toLongOrNull() ?: 0L,
                        lastReadDate = parts[4],
                        coverUrl = null
                    )
                } else null
            }
        } else emptyList()

        return WereadSyncData(
            totalReadTime = totalReadTime,
            todayReadTime = 0L,
            streakDays = streakDays,
            books = books,
            syncTimestamp = syncTimestamp
        )
    }

    /**
     * 是否已有基准快照
     */
    fun hasSnapshot(): Boolean = prefs.getBoolean("has_snapshot", false)

    /**
     * 清除快照（调试用）
     */
    fun clearSnapshot() {
        prefs.edit().clear().apply()
    }
}
