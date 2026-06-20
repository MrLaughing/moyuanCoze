package com.mrlaughing.moyuan.ui.garden

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
import androidx.navigation.fragment.findNavController
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.GardenRenderer
import com.mrlaughing.moyuan.render.PlantImageLoader
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
    private lateinit var weatherDateText: TextView
    private lateinit var irrigationText: TextView
    private lateinit var infoSummaryText: TextView

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
        weatherDateText = view.findViewById(R.id.text_weather_date)
        irrigationText = view.findViewById(R.id.text_irrigation)
        infoSummaryText = view.findViewById(R.id.text_info_summary)
        rendererView = view.findViewById(R.id.garden_renderer)

        // 设置植物点击监听
        rendererView.setOnPlantClickListener { plantId ->
            navigateToPlantDetail(plantId)
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
        // 右上角：天气·季节·日期（如"晴·夏·初七"）
        val weatherDate = "${state.weather.label}·${state.season.label}·${state.dateText}"
        weatherDateText.text = weatherDate

        // 信息条：今日 XhXmin · 连续X天 · ×X.X
        val todayReadStr = "今日 ${state.todayReadMinutes.formatMinutes()}"
        val streakStr = "连续${state.streakDays}天"
        val bonusStr = if (state.bonusMultiplier > 1.0f) "×${state.bonusMultiplier}" else ""
        
        val infoParts = listOf(todayReadStr, streakStr, bonusStr).filter { it.isNotEmpty() }
        infoSummaryText.text = infoParts.joinToString(" · ")

        // 灌溉进度
        irrigationText.text = "${getString(R.string.label_irrigation_progress)} ${state.irrigationHours}/${state.irrigationGoal}h"

        // 渲染植物
        renderPlants(state.plants)
    }

    /**
     * 将植物列表渲染到 GardenRendererView
     */
    private fun renderPlants(plants: List<PlantUiItem>) {
        val context = context ?: return

        // 异步加载植物图片并计算位置
        Thread {
            // 加载 Bitmap
            val plantData = plants.map { plant ->
                val bitmap = PlantImageLoader.load(
                    context,
                    plant.plantId,
                    plant.level,
                    plant.witherStage
                )
                Triple(plant.plantId, bitmap, plant.level)
            }

            // 计算渲染位置
            val positions = GardenRenderer.calculatePositions(
                plants.size,
                rendererView.width,
                rendererView.height
            )

            // 绑定植物到位置
            val renderInfo = GardenRenderer.bindPlantsToPositions(plantData, positions)

            // 切回主线程更新
            activity?.runOnUiThread {
                rendererView.updatePlants(renderInfo)
            }
        }.start()
    }

    /**
     * 导航到植物详情
     */
    private fun navigateToPlantDetail(plantId: Long) {
        val direction = GardenFragmentDirections
            .actionGardenFragmentToPlantDetailFragment(plantId)
        findNavController().navigate(direction)
    }
}
