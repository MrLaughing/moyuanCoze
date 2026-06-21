package com.mrlaughing.moyuan.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.model.AchievementDefinitions
import com.mrlaughing.moyuan.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var unlockedCountText: TextView
    private lateinit var totalPlantsText: TextView
    private lateinit var witheredCountText: TextView
    private lateinit var plantsProgress: ProgressBar
    private lateinit var achievementCountText: TextView
    private lateinit var recyclerAchievements: RecyclerView
    private lateinit var tabAll: TextView
    private lateinit var tabReading: TextView
    private lateinit var tabGrowth: TextView
    private lateinit var tabMilestone: TextView
    private lateinit var indicator: View
    private lateinit var wereadStatusText: TextView
    private lateinit var lastSyncText: TextView
    private lateinit var syncTimeText: TextView
    private lateinit var refreshModeText: TextView
    private lateinit var aboutVersionText: TextView

    private lateinit var achievementAdapter: AchievementAdapter
    private var currentCategory = AchievementDefinitions.CATEGORY_ALL

    // 隐藏的旧视图（保留兼容性）
    private lateinit var plantCountText: TextView
    private lateinit var unlockProgressText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupAchievementRecycler()
        setupTabListeners()
        setupClickListeners(view)
        observeData()
    }

    private fun initViews(view: View) {
        // 第一层：植物收集进度
        unlockedCountText = view.findViewById(R.id.text_unlocked_count)
        totalPlantsText = view.findViewById(R.id.text_total_plants)
        witheredCountText = view.findViewById(R.id.text_withered_count)
        plantsProgress = view.findViewById(R.id.progress_plants)

        // 成就列表
        achievementCountText = view.findViewById(R.id.text_achievement_count)
        recyclerAchievements = view.findViewById(R.id.recycler_achievements)
        tabAll = view.findViewById(R.id.tab_all)
        tabReading = view.findViewById(R.id.tab_reading)
        tabGrowth = view.findViewById(R.id.tab_growth)
        tabMilestone = view.findViewById(R.id.tab_milestone)
        indicator = view.findViewById(R.id.indicator)

        // 设置区
        wereadStatusText = view.findViewById(R.id.text_weread_status)
        lastSyncText = view.findViewById(R.id.text_last_sync)
        syncTimeText = view.findViewById(R.id.text_sync_time)
        refreshModeText = view.findViewById(R.id.text_refresh_mode)
        aboutVersionText = view.findViewById(R.id.text_about_version)

        // 旧视图（隐藏，保持兼容性）
        plantCountText = view.findViewById(R.id.text_plant_count)
        unlockProgressText = view.findViewById(R.id.text_unlock_progress)
    }

    private fun setupAchievementRecycler() {
        achievementAdapter = AchievementAdapter()
        recyclerAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupTabListeners() {
        val tabViews = listOf(tabAll, tabReading, tabGrowth, tabMilestone)
        
        View.OnClickListener { clickedView ->
            val category = when (clickedView.id) {
                R.id.tab_all -> AchievementDefinitions.CATEGORY_ALL
                R.id.tab_reading -> AchievementDefinitions.CATEGORY_READING
                R.id.tab_growth -> AchievementDefinitions.CATEGORY_GROWTH
                R.id.tab_milestone -> AchievementDefinitions.CATEGORY_MILESTONE
                else -> AchievementDefinitions.CATEGORY_ALL
            }
            currentCategory = category
            updateTabSelection(clickedView)
            updateAchievementList()
        }.also { listener ->
            tabAll.setOnClickListener(listener)
            tabReading.setOnClickListener(listener)
            tabGrowth.setOnClickListener(listener)
            tabMilestone.setOnClickListener(listener)
        }

        // 默认选中全部
        updateTabSelection(tabAll)
    }

    private fun updateTabSelection(selectedView: View) {
        val allTabs = listOf(tabAll, tabReading, tabGrowth, tabMilestone)
        allTabs.forEach { tab ->
            if (tab == selectedView) {
                tab.setTextColor(requireContext().getColor(R.color.ink_dark))
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
            } else {
                tab.setTextColor(requireContext().getColor(R.color.text_secondary))
                tab.background = null
            }
        }
    }

    private fun updateAchievementList() {
        val achievements = viewModel.getAchievementsByCategory(currentCategory)
        achievementAdapter.submitList(achievements)
    }

    private fun setupClickListeners(view: View) {
        // 查看图鉴
        view.findViewById<View>(R.id.layout_view_catalog)?.setOnClickListener {
            // 使用底部导航切换而非直接navigate，避免导航栈不同步导致页面点击失灵
            val bottomNav = requireActivity().findViewById<com.mrlaughing.moyuan.ui.common.EinkBottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.catalogFragment
        }

        // 立即同步按钮
        view.findViewById<View>(R.id.layout_sync_now)?.setOnClickListener {
            triggerManualSync()
        }

        // 同步时间设置
        view.findViewById<View>(R.id.layout_sync_time)?.setOnClickListener {
            showSyncTimePicker()
        }

        // 刷新模式设置
        view.findViewById<View>(R.id.layout_refresh_mode)?.setOnClickListener {
            showRefreshModeDialog()
        }

        // 微信读书卡片点击事件
        view.findViewById<View>(R.id.card_weread)?.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.wereadAuthorized) {
                showDeauthorizeConfirmDialog()
            } else {
                showTokenInputDialog()
            }
        }

        // 关于卡片点击
        view.findViewById<View>(R.id.layout_about)?.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderProfileState(state)
                    }
                }
                launch {
                    viewModel.achievements.collect { _ ->
                        updateAchievementList()
                    }
                }
                launch {
                    viewModel.unlockedCount.collect { count ->
                        achievementCountText.text = getString(R.string.label_achievements_count, count)
                    }
                }
            }
        }
    }

    private fun renderProfileState(state: ProfileUiState) {
        // 植物收集进度
        unlockedCountText.text = state.unlockedCount.toString()
        totalPlantsText.text = getString(R.string.label_plants_progress, state.unlockedCount, state.totalCount)
        plantsProgress.max = state.totalCount
        plantsProgress.progress = state.unlockedCount
        
        if (state.witheredCount > 0) {
            witheredCountText.visibility = View.VISIBLE
            witheredCountText.text = getString(R.string.label_withered_hint, state.witheredCount)
        } else {
            witheredCountText.visibility = View.GONE
        }

        // 微信读书状态
        val statusText = if (state.wereadAuthorized) getString(R.string.label_authorized) else getString(R.string.label_unauthorized)
        val statusColor = if (state.wereadAuthorized) requireContext().getColor(R.color.ink_dark) else requireContext().getColor(R.color.text_secondary)
        wereadStatusText.text = statusText
        wereadStatusText.setTextColor(statusColor)

        // 同步信息
        lastSyncText.text = getString(R.string.label_last_sync, state.lastSyncTime)
        syncTimeText.text = String.format("%02d:%02d", state.syncHour, state.syncMinute)
        refreshModeText.text = state.refreshMode
        aboutVersionText.text = "v1.0.0"

        // 旧视图（隐藏，保持兼容性）
        plantCountText.text = "植物 ${state.plantCount}/${state.totalCount}·枯萎${state.witheredCount}"
        unlockProgressText.text = "首次种植 ${state.firstPlantDate}"
    }

    /**
     * 手动触发同步
     */
    private fun triggerManualSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(requireContext()).enqueue(syncRequest)

        Toast.makeText(context, "正在同步...", Toast.LENGTH_SHORT).show()

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(syncRequest.id)
            .observe(viewLifecycleOwner) { workInfo ->
                when {
                    workInfo.state == WorkInfo.State.SUCCEEDED -> {
                        Toast.makeText(context, "同步完成", Toast.LENGTH_SHORT).show()
                    }
                    workInfo.state == WorkInfo.State.FAILED -> {
                        val errorMsg = workInfo.outputData.getString(SyncWorker.KEY_ERROR_MSG)
                            ?: "未知错误"
                        Toast.makeText(context, "同步失败: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun showTokenInputDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入微信读书 API Key"
            setPadding(48, 24, 48, 24)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("微信读书授权")
            .setMessage("请在微信读书 App 设置中获取 API Key（格式：wrk-xxx）")
            .setView(editText)
            .setPositiveButton("授权") { _, _ ->
                val token = editText.text.toString().trim()
                if (token.isNotBlank()) {
                    viewModel.authorize(token)
                    Toast.makeText(context, "授权成功，点击立即同步拉取数据", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "API Key 不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeauthorizeConfirmDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("取消授权")
            .setMessage("确定要取消微信读书授权吗？取消后将无法自动同步阅读数据。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.deauthorize()
                Toast.makeText(context, "已取消授权", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSyncTimePicker() {
        val state = viewModel.uiState.value
        val picker = android.app.TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                viewModel.updateSyncTime(hour, minute)
            },
            state.syncHour,
            state.syncMinute,
            true
        )
        picker.show()
    }

    private fun showRefreshModeDialog() {
        val options = arrayOf("局部刷新", "全刷模式")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("刷新模式")
            .setItems(options) { _, which ->
                viewModel.updateRefreshMode(options[which])
            }
            .show()
    }

    private fun showAboutDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.label_about_moyuan))
            .setMessage(getString(R.string.about_content))
            .setPositiveButton("确定", null)
            .show()
    }
}
