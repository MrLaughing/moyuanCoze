package com.mrlaughing.moyuan.sync

/**
 * 同步相关数据类（兼容旧代码引用）
 * 新版 SyncWorker 不再使用这些类，保留以避免编译错误
 */
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
