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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.Job
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
@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudyViewModel @Inject constructor(
    private val readStatsRepository: ReadStatsRepository,
    private val gardenRepository: GardenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    // 当前周的偏移量：0=本周, -1=上周, 1=下周
    private val _currentWeekOffset = MutableStateFlow(0)
    private val currentWeekOffset: StateFlow<Int> = _currentWeekOffset.asStateFlow()
    private var loadJob: Job? = null

    // 日期格式化
    private val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日")

    init {
        loadStats()
    }

    /**
     * 加载统计数据
     * 组合：阅读统计 + 花园元数据 + 周偏移 → 再用 flatMapLatest 切换周记录 + 最近书目
     * 使用 flatMapLatest 确保每次 outer 变化时自动取消旧的内层订阅，避免协程泄漏
     */
    private fun loadStats() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                readStatsRepository.observeReadStats(),
                gardenRepository.observeMeta(),
                gardenRepository.observeGardenState(),
                currentWeekOffset
            ) { stats, meta, gardenState, weekOffset ->
                val totalReadMinutes = meta?.accumulatedMinutes ?: 0
                val booksRead = meta?.booksRead ?: 0
                val streakDays = meta?.streakDays ?: 0
                val unlockedCount = gardenState.plants.count { !it.unlockDate.isNullOrEmpty() }
                val totalPlantCount = gardenState.plants.size
                val (weekStart, weekEnd) = getWeekRange(weekOffset)
                val firstRecordDate = meta?.installDate
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: LocalDate.now()
                WeekData(
                    stats,
                    meta,
                    totalReadMinutes,
                    booksRead,
                    streakDays,
                    unlockedCount,
                    totalPlantCount,
                    weekStart,
                    weekEnd,
                    firstRecordDate
                )
            }   // 防抖，避免同步时DB连续写入导致UI频繁刷新
                .distinctUntilChanged()
                .debounce(150)
                .flatMapLatest { data ->
                    val today = LocalDate.now().toString()
                    combine(
                        readStatsRepository.observeWeekRecords(data.weekStart, data.weekEnd),
                        readStatsRepository.observeRecentBooks(),
                        readStatsRepository.observeRecordByDate(today)
                    ) { records, recentBooks, todayRecord ->
                        val todayMinutes = todayRecord?.readMinutes ?: 0

                        val weekly = records.map { entity ->
                            DailyRecord(
                                date = LocalDate.parse(entity.date),
                                readMinutes = entity.readMinutes,
                                hasRead = entity.readMinutes > 0
                            )
                        }

                        val books = recentBooks.map { entity ->
                            BookItem(
                                title = entity.title,
                                totalReadMinutes = entity.readMinutes,
                                lastReadDate = entity.lastReadDate,
                                progressPercent = entity.progressPercent
                            )
                        }

                        val weekRangeLabel = "${data.weekStart.format(monthDayFormatter)} - ${data.weekEnd.format(monthDayFormatter)}"
                        val firstWeekStart = data.firstRecordDate.with(
                            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                        )
                        val canGoPrevious = data.weekStart.isAfter(firstWeekStart)
                        val canGoNext = data.weekEnd.isBefore(LocalDate.now())

                        StudyUiState(
                            todayReadMinutes = todayMinutes,
                            streakDays = data.streakDays,
                            totalReadMinutes = data.totalReadMinutes,
                            booksRead = data.booksRead,
                            unlockedCount = data.unlockedCount,
                            totalPlantCount = data.totalPlantCount,
                            weeklyRecords = weekly,
                            recentBooks = books,
                            weekRangeLabel = weekRangeLabel,
                            canGoToPreviousWeek = canGoPrevious,
                            canGoToNextWeek = canGoNext
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    /**
     * 上一周
     */
    fun previousWeek() {
        if (!_uiState.value.canGoToPreviousWeek) return
        val newOffset = _currentWeekOffset.value - 1
        _currentWeekOffset.value = newOffset
    }

    /**
     * 下一周
     */
    fun nextWeek() {
        if (_currentWeekOffset.value >= 0 || !_uiState.value.canGoToNextWeek) return
        val newOffset = _currentWeekOffset.value + 1
        _currentWeekOffset.value = newOffset
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
}

data class StudyUiState(
    val todayReadMinutes: Int = 0,
    val streakDays: Int = 0,
    val totalReadMinutes: Int = 0,
    val booksRead: Int = 0,
    val unlockedCount: Int = 0,
    val totalPlantCount: Int = 0,
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
    val lastReadDate: String? = null,
    val progressPercent: Int = 0
)

/**
 * 书案统计组合数据（含周范围，用于 flatMapLatest 切换）
 */
private data class WeekData(
    val stats: ReadStats?,
    val meta: GardenMetaEntity?,
    val totalReadMinutes: Int,
    val booksRead: Int,
    val streakDays: Int,
    val unlockedCount: Int,
    val totalPlantCount: Int,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val firstRecordDate: LocalDate
)



