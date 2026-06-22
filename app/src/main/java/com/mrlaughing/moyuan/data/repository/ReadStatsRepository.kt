package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.local.db.dao.BookTrackingDao
import com.mrlaughing.moyuan.data.local.db.dao.DailyRecordDao
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读统计数据类
 * 组合每日记录与书目追踪的核心统计
 */
data class ReadStats(
    val totalReadMinutes: Int,
    val streakDays: Int,
    val nightReadDays: Int,
    val booksRead: Int,
    val completedBooks: Int
)

/**
 * 阅读统计仓库
 * 提供阅读数据的组合观察
 */
@Singleton
class ReadStatsRepository @Inject constructor(
    private val dailyRecordDao: DailyRecordDao,
    private val bookTrackingDao: BookTrackingDao
) {

    /**
     * 观察阅读统计概览
     * 组合：总阅读分钟、夜读天数、已读书目、已读完书目
     */
    fun observeReadStats(): Flow<ReadStats> {
        return combine(
            dailyRecordDao.getTotalReadMinutes(),
            dailyRecordDao.getNightReadDayCount(),
            bookTrackingDao.getAllBooks(),
            bookTrackingDao.getCompletedBookCount()
        ) { totalMinutes, nightDays, books, completedCount ->
            ReadStats(
                totalReadMinutes = totalMinutes,
                streakDays = 0, // 连续天数由 GardenMeta 维护，此处暂不重复计算
                nightReadDays = nightDays,
                booksRead = books.size,
                completedBooks = completedCount
            )
        }
    }

    /**
     * 观察最近7天的阅读记录（从今天往前推7天）
     */
    fun observeWeeklyRecords(): Flow<List<DailyRecordEntity>> {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(6) // 包含今天共7天
        return dailyRecordDao.getRecordsBetween(
            start = weekAgo.toString(),
            end = today.toString()
        )
    }

    /**
     * 观察指定周的阅读记录
     * @param weekStart 周一日期
     * @param weekEnd 周日日期
     */
    fun observeWeekRecords(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<DailyRecordEntity>> {
        return dailyRecordDao.getRecordsBetween(
            start = weekStart.toString(),
            end = weekEnd.toString()
        )
    }

    /**
     * 观察指定月份的阅读记录
     */
    fun observeMonthlyRecords(yearMonth: YearMonth): Flow<List<DailyRecordEntity>> {
        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()
        return dailyRecordDao.getRecordsBetween(start = startDate, end = endDate)
    }

    /**
     * 观察最近阅读的书目
     */
    fun observeRecentBooks(): Flow<List<BookTrackingEntity>> {
        return bookTrackingDao.getRecentBooks(limit = 20)
    }

    /**
     * 观察指定日期的阅读记录
     */
    fun observeRecordByDate(date: String): Flow<DailyRecordEntity?> {
        return dailyRecordDao.getRecordByDate(date)
    }

    /**
     * 观察所有每日记录
     */
    fun observeAllRecords(): Flow<List<DailyRecordEntity>> {
        return dailyRecordDao.getAllRecords()
    }

    /**
     * 观察所有书目
     */
    fun observeAllBooks(): Flow<List<BookTrackingEntity>> {
        return bookTrackingDao.getAllBooks()
    }

    /**
     * 观察正在阅读的书目
     */
    fun observeReadingBooks(): Flow<List<BookTrackingEntity>> {
        return bookTrackingDao.getReadingBooks()
    }

    /**
     * 插入或更新每日记录
     */
    suspend fun upsertDailyRecord(entity: DailyRecordEntity) {
        dailyRecordDao.insertRecord(entity)
    }

    /**
     * 插入或更新书目追踪
     */
    suspend fun upsertBookTracking(entity: BookTrackingEntity) {
        bookTrackingDao.insertBook(entity)
    }
}
