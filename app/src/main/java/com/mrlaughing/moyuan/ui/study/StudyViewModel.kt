package com.mrlaughing.moyuan.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 书案 ViewModel
 *
 * 数据来源：
 * - totalReadMinutes / booksRead：从 GardenMeta 读取（由同步直接填充微信读书历史数据）
 * - todayReadMinutes / weeklyRecords / monthlyRecords：从 daily_record 读取
 * - streakDays：从 GardenMeta 读取
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val readStatsRepository: ReadStatsRepository,
    private val gardenRepository: GardenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    init {
        loadStats()
        observeMonthlyRecords()
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                readStatsRepository.observeReadStats(),
                readStatsRepository.observeWeeklyRecords(),
                readStatsRepository.observeRecentBooks(),
                gardenRepository.observeMeta()
            ) { stats, weeklyRecords, recentBooks, meta ->

                val weekly = weeklyRecords.map { entity ->
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
                        lastReadDate = entity.lastReadDate
                    )
                }

                // 今日阅读分钟数（从今天的记录获取）
                val today = LocalDate.now().toString()
                val todayRecord = weeklyRecords.find { it.date == today }
                val todayMinutes = todayRecord?.readMinutes ?: 0

                // 连续天数从 meta 获取
                val streakDays = meta?.streakDays ?: 0

                // 总阅读分钟和已读书目从 meta 获取（由同步直接写入微信读书历史数据）
                val totalReadMinutes = meta?.accumulatedMinutes ?: 0
                val booksRead = meta?.booksRead ?: 0

                StudyUiState(
                    todayReadMinutes = todayMinutes,
                    streakDays = streakDays,
                    totalReadMinutes = totalReadMinutes,
                    booksRead = booksRead,
                    weeklyRecords = weekly,
                    recentBooks = books
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * 观察当月记录，用于月历热力图
     */
    private fun observeMonthlyRecords() {
        viewModelScope.launch {
            _currentMonth.collect { yearMonth ->
                readStatsRepository.observeMonthlyRecords(yearMonth).collect { records ->
                    val monthlyMap = records.associate { entity ->
                        entity.date to entity.readMinutes
                    }
                    _uiState.value = _uiState.value.copy(
                        monthlyRecords = monthlyMap,
                        currentMonth = yearMonth
                    )
                }
            }
        }
    }

    fun setCurrentMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    fun refresh() {
        loadStats()
        observeMonthlyRecords()
    }
}

data class StudyUiState(
    val todayReadMinutes: Int = 0,
    val streakDays: Int = 0,
    val totalReadMinutes: Int = 0,
    val booksRead: Int = 0,
    val weeklyRecords: List<DailyRecord> = emptyList(),
    val recentBooks: List<BookItem> = emptyList(),
    val monthlyRecords: Map<String, Int> = emptyMap(), // date -> minutes
    val currentMonth: YearMonth = YearMonth.now()
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
