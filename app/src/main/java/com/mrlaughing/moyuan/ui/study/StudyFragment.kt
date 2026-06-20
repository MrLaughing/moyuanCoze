package com.mrlaughing.moyuan.ui.study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

/**
 * 阅读统计 Fragment
 */
@AndroidEntryPoint
class StudyFragment : Fragment() {

    private val viewModel: StudyViewModel by viewModels()

    private lateinit var todayReadText: TextView
    private lateinit var streakText: TextView
    private lateinit var totalReadText: TextView
    private lateinit var booksReadText: TextView
    private lateinit var weekOverview: WeekOverviewView
    private lateinit var bookRecyclerView: RecyclerView
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
        todayReadText = view.findViewById(R.id.text_today_read)
        streakText = view.findViewById(R.id.text_streak)
        totalReadText = view.findViewById(R.id.text_total_read)
        booksReadText = view.findViewById(R.id.text_books_read)

        // 周概览
        weekOverview = view.findViewById(R.id.week_overview)

        // 最近阅读书目列表
        bookRecyclerView = view.findViewById(R.id.recycler_books)
        bookAdapter = BookListAdapter()
        bookRecyclerView.layoutManager = LinearLayoutManager(context)
        bookRecyclerView.adapter = bookAdapter
        bookRecyclerView.itemAnimator = null

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
        streakText.text = "${state.streakDays} 天"
        totalReadText.text = state.totalReadMinutes.formatMinutes()
        booksReadText.text = "${state.booksRead} 本"

        weekOverview.setRecords(state.weeklyRecords)
        bookAdapter.submitList(state.recentBooks)
    }
}
