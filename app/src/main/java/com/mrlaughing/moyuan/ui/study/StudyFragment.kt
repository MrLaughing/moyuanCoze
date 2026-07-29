package com.mrlaughing.moyuan.ui.study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.study.PreferCategoryItem
import com.mrlaughing.moyuan.data.local.study.StudyExtraSnapshot
import com.mrlaughing.moyuan.util.formatMinutes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 书案统计 Fragment · 分区卡片流
 *
 * 区块：概览数据 → 每周阅读 → 我的书架 → 阅读偏好 → 读得最久 → 阅读勋章 → 书摘拾遗 → 最近阅读
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

    // 书架
    private lateinit var sectionShelf: View
    private lateinit var shelfCountText: TextView
    private lateinit var shelfRecycler: RecyclerView
    private lateinit var shelfAdapter: ShelfBookAdapter

    // 阅读偏好
    private lateinit var sectionPrefs: View
    private lateinit var rowPrefWords: View
    private lateinit var tagCategory: TextView
    private lateinit var tagTime: TextView
    private lateinit var prefBarsContainer: LinearLayout
    private lateinit var prefAuthorsText: TextView

    // 读得最久
    private lateinit var sectionFavorites: View
    private lateinit var favoritesRecycler: RecyclerView
    private lateinit var favoriteAdapter: FavoriteBookAdapter

    // 勋章
    private lateinit var sectionMedals: View
    private lateinit var medalsRecycler: RecyclerView
    private lateinit var medalAdapter: MedalAdapter

    // 书摘
    private lateinit var sectionNotes: View
    private lateinit var noteCountText: TextView
    private lateinit var notesRecycler: RecyclerView
    private lateinit var noteAdapter: NoteAdapter

    // 最近阅读
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

        bindViews(view)
        setupRecyclers()

        // 周导航点击事件
        btnPrevWeek.setOnClickListener { viewModel.previousWeek() }
        btnNextWeek.setOnClickListener { viewModel.nextWeek() }

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

    private fun bindViews(view: View) {
        dateText = view.findViewById(R.id.text_date)
        todayReadText = view.findViewById(R.id.text_today_read)
        streakText = view.findViewById(R.id.text_streak)
        totalReadText = view.findViewById(R.id.text_total_read)
        booksReadText = view.findViewById(R.id.text_books_read)
        catalogProgressText = view.findViewById(R.id.text_catalog_progress)
        weekOverview = view.findViewById(R.id.week_overview)
        weekRangeText = view.findViewById(R.id.text_week_range)
        btnPrevWeek = view.findViewById(R.id.btn_prev_week)
        btnNextWeek = view.findViewById(R.id.btn_next_week)

        sectionShelf = view.findViewById(R.id.section_shelf)
        shelfCountText = view.findViewById(R.id.text_shelf_count)
        shelfRecycler = view.findViewById(R.id.recycler_shelf)

        sectionPrefs = view.findViewById(R.id.section_prefs)
        rowPrefWords = view.findViewById(R.id.row_pref_words)
        tagCategory = view.findViewById(R.id.text_tag_category)
        tagTime = view.findViewById(R.id.text_tag_time)
        prefBarsContainer = view.findViewById(R.id.container_pref_bars)
        prefAuthorsText = view.findViewById(R.id.text_pref_authors)

        sectionFavorites = view.findViewById(R.id.section_favorites)
        favoritesRecycler = view.findViewById(R.id.recycler_favorites)

        sectionMedals = view.findViewById(R.id.section_medals)
        medalsRecycler = view.findViewById(R.id.recycler_medals)

        sectionNotes = view.findViewById(R.id.section_notes)
        noteCountText = view.findViewById(R.id.text_note_count)
        notesRecycler = view.findViewById(R.id.recycler_notes)

        bookRecyclerView = view.findViewById(R.id.recycler_books)
        emptyBooksText = view.findViewById(R.id.text_empty_books)
    }

    private fun setupRecyclers() {
        val gap = resources.getDimensionPixelSize(R.dimen.shelf_item_gap)

        shelfAdapter = ShelfBookAdapter()
        shelfRecycler.adapter = shelfAdapter
        shelfRecycler.itemAnimator = null
        shelfRecycler.addItemDecoration(HorizontalGapDecoration(gap))

        favoriteAdapter = FavoriteBookAdapter()
        favoritesRecycler.adapter = favoriteAdapter
        favoritesRecycler.itemAnimator = null

        medalAdapter = MedalAdapter()
        medalsRecycler.adapter = medalAdapter
        medalsRecycler.itemAnimator = null
        medalsRecycler.addItemDecoration(HorizontalGapDecoration(gap))

        noteAdapter = NoteAdapter()
        notesRecycler.adapter = noteAdapter
        notesRecycler.itemAnimator = null

        bookAdapter = BookListAdapter()
        bookRecyclerView.adapter = bookAdapter
        bookRecyclerView.itemAnimator = null
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

        // 最近阅读
        val hasBooks = state.recentBooks.isNotEmpty()
        bookRecyclerView.visibility = if (hasBooks) View.VISIBLE else View.GONE
        emptyBooksText.visibility = if (hasBooks) View.GONE else View.VISIBLE
        bookAdapter.submitList(state.recentBooks)

        // 富数据区块
        renderExtra(state.extra)
    }

    private fun renderExtra(extra: StudyExtraSnapshot?) {
        // 书架
        val hasShelf = !extra?.shelfBooks.isNullOrEmpty()
        sectionShelf.visibility = if (hasShelf) View.VISIBLE else View.GONE
        if (hasShelf) {
            shelfAdapter.submitList(extra!!.shelfBooks)
            shelfCountText.text = getString(R.string.label_shelf_count, extra.shelfTotal)
        }

        // 阅读偏好
        val hasCategories = !extra?.preferCategories.isNullOrEmpty()
        val hasWords = !extra?.preferCategoryWord.isNullOrBlank() || !extra?.preferTimeWord.isNullOrBlank()
        val hasAuthors = !extra?.preferAuthors.isNullOrEmpty()
        val hasPrefs = hasCategories || hasWords || hasAuthors
        sectionPrefs.visibility = if (hasPrefs) View.VISIBLE else View.GONE
        if (hasPrefs && extra != null) {
            rowPrefWords.visibility = if (hasWords) View.VISIBLE else View.GONE
            tagCategory.visibility = if (!extra.preferCategoryWord.isNullOrBlank()) View.VISIBLE else View.GONE
            tagCategory.text = extra.preferCategoryWord
            tagTime.visibility = if (!extra.preferTimeWord.isNullOrBlank()) View.VISIBLE else View.GONE
            tagTime.text = extra.preferTimeWord

            renderPrefBars(extra.preferCategories)

            if (hasAuthors) {
                prefAuthorsText.visibility = View.VISIBLE
                prefAuthorsText.text = "常读作者：" +
                    extra.preferAuthors.joinToString(" \u00b7 ") { it.name }
            } else {
                prefAuthorsText.visibility = View.GONE
            }
        }

        // 读得最久
        val hasFavorites = !extra?.favoriteBooks.isNullOrEmpty()
        sectionFavorites.visibility = if (hasFavorites) View.VISIBLE else View.GONE
        if (hasFavorites) favoriteAdapter.submitList(extra!!.favoriteBooks)

        // 勋章
        val hasMedals = !extra?.medals.isNullOrEmpty()
        sectionMedals.visibility = if (hasMedals) View.VISIBLE else View.GONE
        if (hasMedals) medalAdapter.submitList(extra!!.medals)

        // 书摘
        val hasNotes = !extra?.notes.isNullOrEmpty()
        sectionNotes.visibility = if (hasNotes) View.VISIBLE else View.GONE
        if (hasNotes) {
            noteAdapter.submitList(extra!!.notes)
            noteCountText.text = if (extra.totalNoteCount > 0) {
                getString(R.string.label_excerpt_count, extra.totalNoteCount)
            } else ""
        }
    }

    /**
     * 渲染分类偏好条（动态填充，按最长时长归一化比例）
     */
    private fun renderPrefBars(categories: List<PreferCategoryItem>) {
        prefBarsContainer.removeAllViews()
        if (categories.isEmpty()) {
            prefBarsContainer.visibility = View.GONE
            return
        }
        prefBarsContainer.visibility = View.VISIBLE
        val maxSeconds = categories.maxOf { it.readingSeconds }.coerceAtLeast(1)
        val inflater = LayoutInflater.from(requireContext())
        categories.forEach { cat ->
            val row = inflater.inflate(R.layout.item_pref_bar, prefBarsContainer, false)
            row.findViewById<TextView>(R.id.text_pref_label).text = cat.title
            row.findViewById<TextView>(R.id.text_pref_value).text =
                (cat.readingSeconds / 60).toInt().formatMinutes()
            val fill = row.findViewById<View>(R.id.view_pref_fill)
            val track = fill.parent as FrameLayout
            val ratio = cat.readingSeconds.toFloat() / maxSeconds
            track.post {
                val lp = fill.layoutParams
                lp.width = (track.width * ratio).toInt().coerceAtLeast(
                    resources.getDimensionPixelSize(R.dimen.pref_bar_height)
                )
                fill.layoutParams = lp
            }
            prefBarsContainer.addView(row)
        }
    }

    /**
     * 横向列表间距装饰
     */
    private class HorizontalGapDecoration(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position > 0) outRect.left = gap
        }
    }
}
