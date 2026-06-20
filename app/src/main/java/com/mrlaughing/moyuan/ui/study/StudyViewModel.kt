package com.mrlaughing.moyuan.ui.study

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import com.mrlaughing.moyuan.util.formatMinutes
import javax.inject.Inject

/**
 * 阅读 ViewModel
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    // 注入 ReadStatsRepository（待实现）
    // private val readStatsRepository: ReadStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // TODO: 从 Repository 加载真实数据
            _uiState.value = StudyUiState(
                todayReadMinutes = 45,
                streakDays = 7,
                totalReadMinutes = 3200,
                booksRead = 12,
                weeklyRecords = generateMockWeeklyRecords(),
                recentBooks = generateMockBooks()
            )
        }
    }

    private fun generateMockWeeklyRecords(): List<DailyRecord> {
        val today = LocalDate.now()
        return (0 until 7).map { i ->
            val date = today.minusDays(i.toLong())
            DailyRecord(
                date = date,
                readMinutes = (10..120).random(),
                hasRead = true
            )
        }.reversed()
    }

    private fun generateMockBooks(): List<BookItem> {
        return listOf(
            BookItem("百年孤独", 185),
            BookItem("三体", 320),
            BookItem("人类简史", 210),
            BookItem("小王子", 45),
            BookItem("活着", 90)
        )
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
