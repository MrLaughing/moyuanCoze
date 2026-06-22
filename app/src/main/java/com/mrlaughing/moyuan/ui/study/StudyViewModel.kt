package com.mrlaughing.moyuan.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import com.mrlaughing.moyuan.data.repository.ReadStats
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * 书案 ViewModel
 *
 * 数据来源：
 * - totalReadMinutes / booksRead：从 GardenMeta 读取（由同步直接填充微信读书历史数据）
 * - todayReadMinutes / weeklyRecords：从 daily_record 读取
 * - streakDays：从 GardenMeta 读取
 *
 * 每周阅读支持前后翻周
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val readStatsRepository: ReadStatsRepository,
    private val gardenRepository: GardenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    // 当前周的偏移量：0=本周, -1=上周, 1=下周
    private val _currentWeekOffset = MutableStateFlow(0)
    private val currentWeekOffset: StateFlow<Int> = _currentWeekOffset.asStateFlow()

    // 日期格式化
    private val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日")

    init {
        loadStats()
    }

    /**
     * 加载统计数据
     * 组合：阅读统计 + 指定周的记录 + 最近书目 + 花园元数据
     */
    private fun loadStats() {
        viewModelScope.launch {
            combine(
                readStatsRepository.observeReadStats(),
                gardenRepository.observeMeta()
            ) { stats, meta ->
                // 从 meta 获取总阅读分钟和已读书目
                val totalReadMinutes = meta?.accumulatedMinutes ?: 0
                val booksRead = meta?.booksRead ?: 0
                val streakDays = meta?.streakDays ?: 0
                StudyStatsData(stats, meta, totalReadMinutes, booksRead, streakDays)
            }.collect { data ->
                val stats = data.stats
                val meta = data.meta
                val totalReadMinutes = data.totalReadMinutes
                val booksRead = data.booksRead
                val streakDays = data.streakDays

                // 获取当前周的起止日期
                val weekOffset = currentWeekOffset.value
                val (weekStart, weekEnd) = getWeekRange(weekOffset)

                // 观察指定周的记录
                observeWeekRecords(weekStart, weekEnd, totalReadMinutes, booksRead, streakDays, meta)
            }
        }
    }

    /**
     * 观察指定周的阅读记录
     */
    private fun observeWeekRecords(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        totalReadMinutes: Int,
        booksRead: Int,
        streakDays: Int,
        meta: GardenMetaEntity?
    ) {
        viewModelScope.launch {
            readStatsRepository.observeWeekRecords(weekStart, weekEnd).collect { records ->
                // 今日阅读分钟数（从今天的记录获取）
                val today = LocalDate.now().toString()
                val todayRecord = records.find { it.date == today }
                val todayMinutes = todayRecord?.readMinutes ?: 0

                // 转换记录
                val weekly = records.map { entity ->
                    DailyRecord(
                        date = LocalDate.parse(entity.date),
                        readMinutes = entity.readMinutes,
                        hasRead = entity.readMinutes > 0
                    )
                }

                // 获取最近书目
                viewModelScope.launch {
                    readStatsRepository.observeRecentBooks().collect { recentBooks ->
                        val books = recentBooks.map { entity ->
                            BookItem(
                                title = entity.title,
                                totalReadMinutes = entity.readMinutes,
                                lastReadDate = entity.lastReadDate
                            )
                        }

                        // 计算周范围标签
                        val weekRangeLabel = "${weekStart.format(monthDayFormatter)} - ${weekEnd.format(monthDayFormatter)}"

                        // 判断是否可以翻周
                        val canGoPrevious = weekStart.isAfter(FIRST_RECORD_DATE)
                        val canGoNext = weekEnd.isBefore(LocalDate.now())

                        _uiState.value = StudyUiState(
                            todayReadMinutes = todayMinutes,
                            streakDays = streakDays,
                            totalReadMinutes = totalReadMinutes,
                            booksRead = booksRead,
                            weeklyRecords = weekly,
                            recentBooks = books,
                            weekRangeLabel = weekRangeLabel,
                            canGoToPreviousWeek = canGoPrevious,
                            canGoToNextWeek = canGoNext
                        )
                    }
                }
            }
        }
    }

    /**
     * 上一周
     */
    fun previousWeek() {
        val newOffset = _currentWeekOffset.value - 1
        _currentWeekOffset.value = newOffset
        loadStats()
    }

    /**
     * 下一周
     */
    fun nextWeek() {
        val newOffset = _currentWeekOffset.value + 1
        _currentWeekOffset.value = newOffset
        loadStats()
    }

    /**
     * 根据偏移量计算周的起止日期
     * 周一为起始，周日为结束
     */
    private fun getWeekRange(offset: Int): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        // 获取本周一
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        // 根据偏移量计算目标周
        val targetMonday = thisMonday.plusWeeks(offset.toLong())
        val targetSunday = targetMonday.plusDays(6)
        return Pair(targetMonday, targetSunday)
    }

    fun refresh() {
        loadStats()
    }

    companion object {
        // 允许翻到的最早记录日期（2026年1月1日）
        private val FIRST_RECORD_DATE = LocalDate.of(2026, 1, 1)
    }
}

data class StudyUiState(
    val todayReadMinutes: Int = 0,
    val streakDays: Int = 0,
    val totalReadMinutes: Int = 0,
    val booksRead: Int = 0,
    val weeklyRecords: List<DailyRecord> = emptyList(),
    val recentBooks: List<BookItem> = emptyList(),
    val weekRangeLabel: String = "本周",
    val canGoToPreviousWeek: Boolean = true,
    val canGoToNextWeek: Boolean = false
)

data class DailyRecord(
    val date: LocalDate,
    val readMinutes: Int,
    val hasRead: Boolean
)

data class BookItem(
    val title: String,
    val totalReadMinutes: Int,
    val lastReadDate: String? = null
)

/**
 * 书案统计组合数据
 */
private data class StudyStatsData(
    val stats: ReadStats?,
    val meta: GardenMetaEntity?,
    val totalReadMinutes: Int,
    val booksRead: Int,
    val streakDays: Int
)



