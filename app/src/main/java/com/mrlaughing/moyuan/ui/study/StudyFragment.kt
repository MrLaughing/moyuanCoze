package com.mrlaughing.moyuan.ui.study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.util.formatMinutes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * 书案统计 Fragment
 * 
 * 功能：
 * - 展示今日阅读、连续天数、累计阅读、已读书目
 * - 每周阅读柱状图（支持前后翻周）
 * - 最近阅读书目列表
 */
@AndroidEntryPoint
class StudyFragment : Fragment() {

    private val viewModel: StudyViewModel by viewModels()

    private lateinit var dateText: TextView
    private lateinit var todayReadText: TextView
    private lateinit var streakText: TextView
    private lateinit var totalReadText: TextView
    private lateinit var booksReadText: TextView
    private lateinit var catalogProgressText: TextView
    private lateinit var weekOverview: WeekOverviewView
    private lateinit var weekRangeText: TextView
    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton
    private lateinit var bookRecyclerView: RecyclerView
    private lateinit var emptyBooksText: TextView
    private lateinit var bookAdapter: BookListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_study, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 绑定数据卡片
        dateText = view.findViewById(R.id.text_date)
        todayReadText = view.findViewById(R.id.text_today_read)
        streakText = view.findViewById(R.id.text_streak)
        totalReadText = view.findViewById(R.id.text_total_read)
        booksReadText = view.findViewById(R.id.text_books_read)
        catalogProgressText = view.findViewById(R.id.text_catalog_progress)

        // 周柱状图
        weekOverview = view.findViewById(R.id.week_overview)

        // 周导航
        weekRangeText = view.findViewById(R.id.text_week_range)
        btnPrevWeek = view.findViewById(R.id.btn_prev_week)
        btnNextWeek = view.findViewById(R.id.btn_next_week)

        // 周导航点击事件
        btnPrevWeek.setOnClickListener {
            viewModel.previousWeek()
        }
        btnNextWeek.setOnClickListener {
            viewModel.nextWeek()
        }

        // 最近阅读书目列表
        bookRecyclerView = view.findViewById(R.id.recycler_books)
        emptyBooksText = view.findViewById(R.id.text_empty_books)
        bookAdapter = BookListAdapter()
        bookRecyclerView.layoutManager = LinearLayoutManager(context)
        bookRecyclerView.adapter = bookAdapter
        bookRecyclerView.itemAnimator = null

        // 设置当前日期
        val dateFormat = SimpleDateFormat("yyyy年M月", Locale.CHINESE)
        dateText.text = dateFormat.format(Date())

        // 观察数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: StudyUiState) {
        todayReadText.text = state.todayReadMinutes.formatMinutes()
        streakText.text = "${state.streakDays}天"
        totalReadText.text = state.totalReadMinutes.formatMinutes()
        booksReadText.text = "${state.booksRead}本"
        catalogProgressText.text = if (state.totalPlantCount > 0) {
            val percent = (state.unlockedCount * 100f / state.totalPlantCount).toInt()
            "${state.unlockedCount}/${state.totalPlantCount} ($percent%)"
        } else {
            "0/0"
        }

        // 更新周范围显示
        weekRangeText.text = state.weekRangeLabel
        weekOverview.setRecords(state.weeklyRecords)

        // 更新上一周/下一周按钮状态
        btnPrevWeek.isEnabled = state.canGoToPreviousWeek
        btnNextWeek.isEnabled = state.canGoToNextWeek
        btnPrevWeek.alpha = if (state.canGoToPreviousWeek) 1.0f else 0.3f
        btnNextWeek.alpha = if (state.canGoToNextWeek) 1.0f else 0.3f

        val hasBooks = state.recentBooks.isNotEmpty()
        bookRecyclerView.visibility = if (hasBooks) View.VISIBLE else View.GONE
        emptyBooksText.visibility = if (hasBooks) View.GONE else View.VISIBLE
        bookAdapter.submitList(state.recentBooks)
    }
}
