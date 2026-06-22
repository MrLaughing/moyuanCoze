package com.mrlaughing.moyuan.ui.garden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.render.GardenRenderer
import com.mrlaughing.moyuan.render.PlantRenderInfo
import com.mrlaughing.moyuan.util.formatMinutes
import kotlinx.coroutines.launch

/**
 * 花园主画面 Fragment
 */
@AndroidEntryPoint
class GardenFragment : Fragment() {

    private val viewModel: GardenViewModel by viewModels()

    private lateinit var rendererView: GardenRendererView
    private lateinit var tagSeason: TextView
    private lateinit var tagWeather: TextView
    private lateinit var tagDate: TextView
    private lateinit var textTodayRead: TextView
    private lateinit var textStreak: TextView
    private lateinit var textBonus: TextView
    private lateinit var textBonusDetail: TextView
    private lateinit var textPlantStatus: TextView
    private lateinit var textPathInfo: TextView
    private lateinit var waterButton: MaterialButton
    private lateinit var witherAlertCard: LinearLayout
    private lateinit var textWitherAlert: TextView

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
        textBonus = view.findViewById(R.id.text_bonus)
        textBonusDetail = view.findViewById(R.id.text_bonus_detail)
        textPlantStatus = view.findViewById(R.id.text_plant_status)
        textPathInfo = view.findViewById(R.id.text_path_info)
        waterButton = view.findViewById(R.id.water_button)
        witherAlertCard = view.findViewById(R.id.wither_alert_card)
        textWitherAlert = view.findViewById(R.id.text_wither_alert)

        // 设置植物点击监听
        rendererView.setOnPlantClickListener { plantId ->
            navigateToPlantDetail(plantId)
        }

        // 设置季节/天气切换点击
        tagSeason.setOnClickListener {
            viewModel.cycleSeason()
        }

        tagWeather.setOnClickListener {
            viewModel.cycleWeather()
        }

        // 设置浇灌按钮点击
        waterButton.setOnClickListener {
            viewModel.waterPlants()
        }

        // 观察 UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    /**
     * 根据状态渲染花园界面
     */
    private fun renderState(state: GardenUiState) {
        // 顶部标签
        tagSeason.text = state.season.label
        tagWeather.text = state.weather.label
        tagDate.text = state.dateText

        // 更新Canvas的季节和天气
        rendererView.setSeasonAndWeather(state.season, state.weather)

        // 三列数据
        textTodayRead.text = state.todayReadMinutes.toString()
        textStreak.text = state.streakDays.toString()
        textBonus.text = "×${String.format("%.1f", state.bonusMultiplier)}"
        textBonusDetail.text = "${state.season.label}${state.weather.label}"

        // 植物状态汇总
        if (state.plants.isNotEmpty()) {
            val statusText = state.plants.take(6).joinToString(" · ") { plant ->
                val witherLabel = when (plant.witherStage) {
                    0 -> "鲜活"
                    1 -> "初淡"
                    2 -> "渐枯"
                    3 -> "将枯"
                    else -> "枯寂"
                }
                "${plant.name}${witherLabel}"
            }
            textPlantStatus.text = statusText
            textPlantStatus.visibility = View.VISIBLE
        } else {
            textPlantStatus.visibility = View.GONE
        }

        // 枯萎预警
        val witheringCount = state.plants.count { it.witherStage > 0 }
        if (witheringCount > 0) {
            witherAlertCard.visibility = View.VISIBLE
            textWitherAlert.text = "⚠ $witheringCount 株植物开始枯萎，请浇灌"
        } else {
            witherAlertCard.visibility = View.GONE
        }

        // 路径加成信息
        textPathInfo.text = getPathBonusText(state.season)

        // 浇灌按钮文字
        val waterButtonText = if (state.todayReadMinutes > 0) {
            "浇灌 · 今日阅读 ${state.todayReadMinutes.formatMinutes()}"
        } else {
            "今日暂无阅读"
        }
        waterButton.text = waterButtonText

        // 更新按钮状态
        waterButton.isEnabled = state.todayReadMinutes > 0

        // 渲染植物
        renderPlants(state.plants)
    }

    /**
     * 获取路径加成文本
     */
    private fun getPathBonusText(season: Season): String {
        // 简化实现，根据季节返回对应的加成路径
        val pathNames = when (season) {
            Season.SPRING -> "积墨 · 秉烛"
            Season.SUMMER -> "积墨 · 岁寒"
            Season.AUTUMN -> "秉烛 · 寻芳"
            Season.WINTER -> "岁寒 · 寻芳"
        }
        return "$pathNames ×1.2"
    }

    /**
     * 将植物列表渲染到 GardenRendererView
     * 新方案：直接构造 PlantRenderInfo，Canvas绘制水墨风植物，无需加载bitmap
     */
    private fun renderPlants(plants: List<PlantUiItem>) {
        if (plants.isEmpty()) {
            rendererView.updatePlants(emptyList())
            return
        }

        // 等待视图布局完成再计算位置
        rendererView.post {
            val w = rendererView.width
            val h = rendererView.height
            if (w <= 0 || h <= 0) return@post

            // 计算渲染位置
            val positions = GardenRenderer.calculatePositions(plants.size, w, h)

            // 直接构造 PlantRenderInfo（Canvas水墨风绘制，无需bitmap）
            val renderInfo = plants.mapIndexed { index, plant ->
                if (index < positions.size) {
                    positions[index].copy(
                        plantId = plant.plantId,
                        plantName = plant.name,
                        level = plant.level,
                        pathType = plant.pathType,
                        witherStage = plant.witherStage
                    )
                } else {
                    PlantRenderInfo(
                        bitmap = null,
                        x = w * 0.5f,
                        y = h * 0.7f,
                        scale = 0.8f,
                        plantId = plant.plantId,
                        plantName = plant.name,
                        level = plant.level,
                        pathType = plant.pathType,
                        witherStage = plant.witherStage
                    )
                }
            }

            rendererView.updatePlants(renderInfo)
        }
    }

    /**
     * 导航到植物详情
     */
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


