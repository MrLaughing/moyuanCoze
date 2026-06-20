package com.mrlaughing.moyuan.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import com.mrlaughing.moyuan.util.formatMinutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 阅读 ViewModel
 * 
 * 从 Repository 加载真实数据：
 * - ReadStatsRepository.observeReadStats() 获取阅读统计
 * - ReadStatsRepository.observeWeeklyRecords() 获取每周记录
 * - ReadStatsRepository.observeRecentBooks() 获取最近书目
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val readStatsRepository: ReadStatsRepository,
    private val gardenRepository: GardenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    /**
     * 从 Repository 加载真实阅读数据
     */
    private fun loadStats() {
        viewModelScope.launch {
            // 组合多个数据流
            combine(
                readStatsRepository.observeReadStats(),
                readStatsRepository.observeWeeklyRecords(),
                readStatsRepository.observeRecentBooks(),
                gardenRepository.observeMeta()
            ) { stats, weeklyRecords, recentBooks, meta ->
                // 构建 WeeklyRecords
                val weekly = weeklyRecords.map { entity ->
                    DailyRecord(
                        date = java.time.LocalDate.parse(entity.date),
                        readMinutes = entity.readMinutes,
                        hasRead = entity.readMinutes > 0
                    )
                }

                // 构建 RecentBooks
                val books = recentBooks.map { entity ->
                    BookItem(
                        title = entity.title,
                        totalReadMinutes = entity.readMinutes
                    )
                }

                // 今日阅读分钟数（从今天的记录获取）
                val today = LocalDate.now().toString()
                val todayRecord = weeklyRecords.find { it.date == today }
                val todayMinutes = todayRecord?.readMinutes ?: 0

                // 连续天数从 meta 获取
                val streakDays = meta?.streakDays ?: 0

                StudyUiState(
                    todayReadMinutes = todayMinutes,
                    streakDays = streakDays,
                    totalReadMinutes = stats.totalReadMinutes,
                    booksRead = stats.booksRead,
                    weeklyRecords = weekly,
                    recentBooks = books
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadStats()
    }
}

/**
 * 阅读统计 UI 状态
 */
data class StudyUiState(
    val todayReadMinutes: Int = 0,
    val streakDays: Int = 0,
    val totalReadMinutes: Int = 0,
    val booksRead: Int = 0,
    val weeklyRecords: List<DailyRecord> = emptyList(),
    val recentBooks: List<BookItem> = emptyList()
)

/**
 * 每日阅读记录
 */
data class DailyRecord(
    val date: LocalDate,
    val readMinutes: Int,
    val hasRead: Boolean
)

/**
 * 书目条目
 */
data class BookItem(
    val title: String,
    val totalReadMinutes: Int
)
