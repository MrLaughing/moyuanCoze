package com.mrlaughing.moyuan.ui.garden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
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
class GardenFragment : Fragment() {

    private val viewModel: GardenViewModel by viewModels()

    private lateinit var rendererView: GardenRendererView
    private lateinit var seasonText: TextView
    private lateinit var weatherText: TextView
    private lateinit var dateText: TextView
    private lateinit var todayReadText: TextView
    private lateinit var streakText: TextView
    private lateinit var bonusText: TextView

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
        seasonText = view.findViewById(R.id.text_season)
        weatherText = view.findViewById(R.id.text_weather)
        dateText = view.findViewById(R.id.text_date)
        todayReadText = view.findViewById(R.id.text_today_read)
        streakText = view.findViewById(R.id.text_streak)
        bonusText = view.findViewById(R.id.text_bonus)
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
        // 顶栏
        seasonText.text = state.season.label
        weatherText.text = state.weather.label
        dateText.text = state.dateText

        // 信息条
        todayReadText.text = "今日阅读 ${state.todayReadMinutes.formatMinutes()}"
        streakText.text = "连续 ${state.streakDays} 天"
        bonusText.text = if (state.bonusMultiplier > 1.0f) {
            "加成 ×${state.bonusMultiplier}"
        } else {
            ""
        }

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
