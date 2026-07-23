package com.mrlaughing.moyuan.ui.garden

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButton
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.render.GardenRenderer
import com.mrlaughing.moyuan.render.PlantRenderInfo
import com.mrlaughing.moyuan.sync.SyncScheduler
import com.mrlaughing.moyuan.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 花园主画面 Fragment
 */
@AndroidEntryPoint
class GardenFragment : Fragment() {

    companion object {
        private const val KEY_PANEL_EXPANDED = "panel_expanded"
    }

    private val viewModel: GardenViewModel by viewModels()

    private lateinit var rendererView: GardenRendererView
    private lateinit var tagSeason: TextView
    private lateinit var tagWeather: TextView
    private lateinit var tagDate: TextView
    private lateinit var textTodayRead: TextView
    private lateinit var textStreak: TextView
    private lateinit var textCollectionStat: TextView
    private lateinit var textReadingProgress: TextView
    private lateinit var waterButton: MaterialButton
    private lateinit var panelHeader: LinearLayout
    private lateinit var panelArrow: TextView
    private lateinit var expandableContent: LinearLayout
    private lateinit var layoutOptions: LinearLayout
    private lateinit var tabModeAuto: TextView
    private lateinit var tabModeCustom: TextView
    private var isPanelExpanded = false
    private var isWatering = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_garden, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 绑定视图
        rendererView = view.findViewById(R.id.garden_renderer)
        tagSeason = view.findViewById(R.id.tag_season)
        tagWeather = view.findViewById(R.id.tag_weather)
        tagDate = view.findViewById(R.id.tag_date)
        textTodayRead = view.findViewById(R.id.text_today_read)
        textStreak = view.findViewById(R.id.text_streak)
        textCollectionStat = view.findViewById(R.id.text_collection_stat)
        textReadingProgress = view.findViewById(R.id.text_reading_progress)
        waterButton = view.findViewById(R.id.water_button)
        waterButton.setOnClickListener {
            triggerWatering()
        }

        panelHeader = view.findViewById(R.id.panel_header)
        panelArrow = view.findViewById(R.id.panel_arrow)
        expandableContent = view.findViewById(R.id.expandable_content)

        // 初始化网格布局选择器
        layoutOptions = view.findViewById(R.id.layout_options)
        initGridLayoutOptions()

        // 花园模式切换（自动 / 自定义）
        tabModeAuto = view.findViewById(R.id.tab_mode_auto)
        tabModeCustom = view.findViewById(R.id.tab_mode_custom)
        tabModeAuto.setOnClickListener { viewModel.setGardenMode(GardenMode.AUTO) }
        tabModeCustom.setOnClickListener { viewModel.setGardenMode(GardenMode.CUSTOM) }

        // 设置植物点击监听
        rendererView.setOnPlantClickListener { plantId ->
            navigateToPlantDetail(plantId)
        }
        rendererView.setOnEmptyPlotClickListener {
            Toast.makeText(requireContext(), "等待下一株植物", Toast.LENGTH_SHORT).show()
        }
        rendererView.setOnPlantMoveListener { plantId, targetSlot ->
            viewModel.movePlantToSlot(plantId, targetSlot)
        }

        // 点击天气刷新（重新获取天气并更新花园）
        tagWeather.setOnClickListener {
            viewModel.refreshWeather()
            android.widget.Toast.makeText(requireContext(), "正在刷新天气...", android.widget.Toast.LENGTH_SHORT).show()
        }

        // 设置底部面板展开/收起
        panelHeader.setOnClickListener {
            isPanelExpanded = !isPanelExpanded
            updatePanelExpansion()
        }

        isPanelExpanded = savedInstanceState?.getBoolean(KEY_PANEL_EXPANDED) ?: false
        updatePanelExpansion()

        // 加载所有 50 棵植物的 2.5D PNG 图片到 GardenRenderer 缓存
        loadPlantImages()

        // 观察 UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::renderState) }
                launch {
                    viewModel.messages.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        isPanelExpanded = savedInstanceState?.getBoolean(KEY_PANEL_EXPANDED) ?: false
        updatePanelExpansion()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PANEL_EXPANDED, isPanelExpanded)
        super.onSaveInstanceState(outState)
    }

    private fun updatePanelExpansion() {
        expandableContent.visibility = if (isPanelExpanded) View.VISIBLE else View.GONE
        panelArrow.text = if (isPanelExpanded) "▲" else "▼"
    }

    /**
     * 初始化网格布局选择按钮
     */
    private fun initGridLayoutOptions() {
        layoutOptions.removeAllViews()
        GRID_LAYOUTS.forEachIndexed { index, config ->
            val btn = LayoutInflater.from(context).inflate(
                R.layout.item_grid_layout_option, layoutOptions, false
            ) as TextView
            btn.text = "${config.label} · ${config.cols}×${config.rows}"
            btn.tag = index
            btn.setOnClickListener { v ->
                val idx = v.tag as Int
                viewModel.setGridLayout(idx)
                // 高亮当前选中
                updateGridOptionStyles(idx)
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val margin = (4 * resources.displayMetrics.density).toInt()
            lp.setMargins(0, 0, margin, 0)
            layoutOptions.addView(btn, lp)
        }
    }

    /**
     * 更新布局选项的高亮样式
     */
    private fun updateGridOptionStyles(selectedIndex: Int, unlockedCount: Int = 0, visibleCount: Int = 0) {
        for (i in 0 until layoutOptions.childCount) {
            val btn = layoutOptions.getChildAt(i) as? TextView ?: continue
            val config = GRID_LAYOUTS[i]
            val enabled = unlockedCount >= config.minUnlockedPlants && config.totalSlots >= visibleCount
            btn.isEnabled = enabled
            btn.alpha = if (enabled) 1f else 0.42f
            if (i == selectedIndex) {
                btn.setBackgroundResource(R.drawable.bg_pill_dark)
                btn.setTextColor(requireContext().getColor(R.color.text_on_dark))
            } else {
                btn.setBackgroundResource(R.drawable.bg_pill_light)
                btn.setTextColor(requireContext().getColor(R.color.text_primary))
            }
        }
    }

    /**
     * 根据状态渲染花园界面
     */
    private fun renderState(state: GardenUiState) {
        tagDate.text = state.dateText
        tagSeason.text = state.season.label
        tagWeather.text = state.weather.label

        textTodayRead.text = state.todayReadMinutes.toString()
        textStreak.text = state.streakDays.toString()

        // 图鉴进度（面板头部第三列）
        val gardenCapacity = state.gridCols * state.gridRows
        textCollectionStat.text = getString(
            R.string.garden_plant_count,
            state.plants.size.coerceAtMost(gardenCapacity),
            gardenCapacity
        )

        if (isWatering) {
            waterButton.isEnabled = false
            waterButton.alpha = 0.72f
            waterButton.text = getString(R.string.watering_in_progress)
        } else {
            waterButton.isEnabled = true
            waterButton.alpha = if (state.isAuthorized) 1f else 0.78f
            waterButton.text = if (state.isAuthorized) {
                getString(R.string.watering_ready, state.todayReadMinutes)
            } else {
                getString(R.string.watering_connect)
            }
        }
        // 阅读进度（展开面板中）：随机解锁模型下展示"距下一株还差多少分钟"
        val nextThreshold = state.nextUnlockThreshold
        if (nextThreshold != null && nextThreshold > state.accumulatedMinutes) {
            val remain = nextThreshold - state.accumulatedMinutes
            textReadingProgress.text = "再阅读 ${remain} 分钟，会随机遇见一株新植物"

        } else {
            textReadingProgress.text = "五十株植物已经全部来到你的墨园"
        }

        // 设置 GardenRenderer 网格行列数（用于位置计算）
        GardenRenderer.gridCols = state.gridCols
        GardenRenderer.gridRows = state.gridRows

        // 更新渲染视图的季节/天气和网格布局
        rendererView.setWeatherSeason(state.season, state.weather)
        rendererView.setGridLayout(state.gridCols, state.gridRows)
        rendererView.setEditingEnabled(state.gardenMode == GardenMode.CUSTOM)

        // 更新布局选择器高亮
        updateGridOptionStyles(state.gridLayoutIndex, state.totalUnlocked, state.requiredSlots)

        // 更新花园模式切换高亮
        updateModeStyles(state.gardenMode)

        // 渲染植物（使用网格布局）
        renderPlants(state.plants)
    }

    /**
     * 更新花园模式切换的高亮样式
     */
    private fun updateModeStyles(mode: GardenMode) {
        if (mode == GardenMode.AUTO) {
            tabModeAuto.setBackgroundResource(R.drawable.bg_pill_dark)
            tabModeAuto.setTextColor(requireContext().getColor(R.color.text_on_dark))
            tabModeCustom.setBackgroundResource(R.drawable.bg_pill_light)
            tabModeCustom.setTextColor(requireContext().getColor(R.color.text_secondary))
        } else {
            tabModeCustom.setBackgroundResource(R.drawable.bg_pill_dark)
            tabModeCustom.setTextColor(requireContext().getColor(R.color.text_on_dark))
            tabModeAuto.setBackgroundResource(R.drawable.bg_pill_light)
            tabModeAuto.setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }

    /**
     * 将植物列表渲染到 GardenRendererView（网格布局）
     */
    private fun renderPlants(plants: List<PlantUiItem>) {
        if (plants.isEmpty()) {
            rendererView.updatePlants(emptyList())
            return
        }

        rendererView.post {
            val w = rendererView.width
            val h = rendererView.height
            if (w <= 0 || h <= 0) return@post

            // 计算网格布局位置（使用当前 gridCols/gridRows）
            val positions = GardenRenderer.calculateGridPositions(
                GardenRenderer.gridCols * GardenRenderer.gridRows,
                w,
                h
            )

            val renderInfo = plants.mapIndexed { index, plant ->
                val positionIndex = plant.gardenSlot ?: index
                positions.getOrNull(positionIndex)?.let { position ->
                    position.copy(
                        bitmap = plant.bitmap,
                        plantId = plant.plantId,
                        plantName = plant.name,
                        level = plant.level
                    )
                } ?: run {
                    PlantRenderInfo(
                        bitmap = plant.bitmap,
                        x = w * 0.5f,
                        y = h * 0.5f,
                        scale = 0.8f,
                        plantId = plant.plantId,
                        plantName = plant.name,
                        level = plant.level
                    )
                }
            }

            rendererView.updatePlants(renderInfo)
        }
    }

    /**
     * 从 assets/plants/ 批量加载所有 2.5D 植物 PNG + 四季背景 PNG
     */
    private fun loadPlantImages() {
        if (GardenRenderer.isAssetCacheReady()) {
            rendererView.notifySceneAssetsChanged()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val assets = requireContext().assets
            val plantFiles = try { assets.list("plants") ?: emptyArray() } catch (e: Exception) { emptyArray() }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 4
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val seasonNames = setOf("春", "夏", "秋", "冬")
            for (fileName in plantFiles) {
                if (!fileName.endsWith(".png")) continue
                val plantName = fileName.removeSuffix(".png")
                // 跳过四季图片，单独处理
                if (plantName in seasonNames) continue
                try {
                    val bmp = BitmapFactory.decodeStream(assets.open("plants/$fileName"), null, opts)
                    if (bmp != null) {
                        GardenRenderer.setPlantPng(plantName, bmp)
                    }
                } catch (_: Exception) { }
            }
            val seasonOptions = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val seasonMap = mapOf(
                "春" to Season.SPRING,
                "夏" to Season.SUMMER,
                "秋" to Season.AUTUMN,
                "冬" to Season.WINTER
            )
            for ((name, season) in seasonMap) {
                try {
                    val bmp = BitmapFactory.decodeStream(assets.open("plants/$name.png"), null, seasonOptions)
                    if (bmp != null) {
                        GardenRenderer.setSeasonPng(season, bmp)
                    }
                } catch (_: Exception) { }
            }
            // 加载完成后刷新渲染视图
            withContext(Dispatchers.Main) {
                if (isAdded) rendererView.notifySceneAssetsChanged()
            }
        }
    }

    /**
     * 浇灌 = 触发一次同步，用今日阅读数据浇灌花园（解锁/生长植物）
     */
    private fun triggerWatering() {
        if (!viewModel.uiState.value.isAuthorized) {
            val bottomNav = requireActivity()
                .findViewById<com.mrlaughing.moyuan.ui.common.MoyuanBottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.profileFragment
            Toast.makeText(context, "请先连接微信读书", Toast.LENGTH_SHORT).show()
            return
        }

        isWatering = true
        renderState(viewModel.uiState.value)
        rendererView.playWateringAnimation()
        val syncId = SyncScheduler.enqueueImmediateSync(requireContext(), replaceRunning = true)
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(syncId)
            .observe(viewLifecycleOwner) { workInfo ->
                workInfo ?: return@observe
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING -> Unit
                    WorkInfo.State.SUCCEEDED -> {
                        finishWatering()
                        val newMinutes = workInfo.outputData.getInt(SyncWorker.KEY_NEW_READ_MINUTES, 0)
                        val newPlants = workInfo.outputData.getInt(SyncWorker.KEY_NEW_PLANT_COUNT, 0)
                        val message = if (newMinutes == 0 && newPlants == 0) {
                            getString(R.string.watering_already_done)
                        } else {
                            getString(R.string.watering_complete)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    WorkInfo.State.FAILED -> {
                        finishWatering()
                        val error = workInfo.outputData.getString(SyncWorker.KEY_ERROR_MSG)
                            ?: getString(R.string.msg_sync_failed)
                        Toast.makeText(context, "浇灌失败：$error", Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.CANCELLED -> {
                        finishWatering()
                        Toast.makeText(context, R.string.watering_cancelled, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun finishWatering() {
        isWatering = false
        renderState(viewModel.uiState.value)
    }
    private fun navigateToPlantDetail(plantId: Long) {
        try {
            val direction = GardenFragmentDirections
                .actionGardenFragmentToPlantDetailFragment(plantId)
            findNavController().navigate(direction)
        } catch (e: Exception) {
            android.util.Log.e("GardenFragment", "导航到植物详情失败: plantId=$plantId", e)
        }
    }
}


