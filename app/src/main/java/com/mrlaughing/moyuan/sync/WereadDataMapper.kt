package com.mrlaughing.moyuan.sync

import java.time.LocalDate

/**
 * 微信读书数据映射器：DTO → 领域模型
 */
class WereadDataMapper {

    /**
     * 增量数据 → 阅读统计
     */
    fun toReadStats(increment: IncrementData): ReadStatsResult {
        val totalMinutes = (increment.deltaReadTime / 60).toInt()
        val todayMinutes = (increment.todayReadTime / 60).toInt()
        return ReadStatsResult(
            todayReadMinutes = todayMinutes,
            streakDays = increment.streakDays,
            totalReadMinutes = totalMinutes,
            date = increment.date
        )
    }

    /**
     * 微信读书书目列表 → 书目追踪实体列表
     */
    fun toBookTrackingEntities(books: List<WereadBookData>): List<BookTrackingResult> {
        return books.map { book ->
            BookTrackingResult(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                totalReadMinutes = (book.totalReadTime / 60).toInt(),
                lastReadDate = book.lastReadDate,
                coverUrl = book.coverUrl
            )
        }
    }

    /**
     * 增量数据 → 每日记录实体
     */
    fun toDailyRecordEntity(increment: IncrementData): DailyRecordResult {
        return DailyRecordResult(
            date = increment.date,
            readMinutes = (increment.deltaReadTime / 60).toInt(),
            booksCount = increment.books.size,
            streakDays = increment.streakDays
        )
    }
}

// ── 领域模型（简化版，实际项目应放在 domain 包下） ──

data class ReadStatsResult(
    val todayReadMinutes: Int,
    val streakDays: Int,
    val totalReadMinutes: Int,
    val date: String
)

data class BookTrackingResult(
    val bookId: String,
    val title: String,
    val author: String,
    val totalReadMinutes: Int,
    val lastReadDate: String,
    val coverUrl: String?
)

data class DailyRecordResult(
    val date: String,
    val readMinutes: Int,
    val booksCount: Int,
    val streakDays: Int
)
