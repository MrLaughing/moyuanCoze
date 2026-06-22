package com.mrlaughing.moyuan.ui.plant

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import androidx.navigation.fragment.navArgs
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.render.PlantImageLoader
import com.mrlaughing.moyuan.ui.common.EinkProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 植物详情 Fragment
 */
@AndroidEntryPoint
class PlantDetailFragment : Fragment() {

    private val viewModel: PlantDetailViewModel by viewModels()
    private val args: PlantDetailFragmentArgs by navArgs()

    private lateinit var plantImage: ImageView
    private lateinit var plantName: TextView
    private lateinit var textTitle: TextView
    private lateinit var plantLevel: TextView
    private lateinit var levelProgressBar: EinkProgressBar
    private lateinit var levelProgressText: TextView
    private lateinit var plantDescription: TextView
    private lateinit var witherWarning: TextView
    private lateinit var layoutWitherWarning: LinearLayout
    private lateinit var backButton: View
    private lateinit var pathName: TextView
    private lateinit var rarityText: TextView
    private lateinit var unlockCondition: TextView

    /** 等级名从 GrowthLevel 枚举获取，与引擎定义保持同步 */
    private fun getLevelLabel(level: Int): String {
        return GrowthLevel.entries.firstOrNull { it.level == level }?.label ?: "墨芽"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("PlantDetail", "onCreateView: plantId=${args.plantId}")
        return inflater.inflate(R.layout.fragment_plant_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PlantDetail", "onViewCreated: plantId=${args.plantId}")

        // 初始化视图 - 分步try-catch，定位具体失败点
        try {
            plantImage = view.findViewById(R.id.image_plant_detail)
            plantName = view.findViewById(R.id.text_plant_name)
            textTitle = view.findViewById(R.id.text_title)
            plantLevel = view.findViewById(R.id.text_plant_level)
            levelProgressBar = view.findViewById(R.id.progress_level)
            levelProgressText = view.findViewById(R.id.text_level_progress)
            plantDescription = view.findViewById(R.id.text_plant_description)
            witherWarning = view.findViewById(R.id.text_wither_warning)
            layoutWitherWarning = view.findViewById(R.id.layout_wither_warning)
            backButton = view.findViewById(R.id.button_back)
            pathName = view.findViewById(R.id.text_path_name)
            rarityText = view.findViewById(R.id.text_rarity)
            unlockCondition = view.findViewById(R.id.text_unlock_condition)
            Log.d("PlantDetail", "视图初始化完成")
        } catch (e: Exception) {
            Log.e("PlantDetail", "视图初始化失败!!!", e)
            // 不再navigateUp闪回，改为显示Toast提示
            Toast.makeText(requireContext(), "视图初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        backButton.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e("PlantDetail", "返回导航失败", e)
            }
        }

        // 加载植物数据 - args.plantId 为 Long 索引，转为 String ID
        val plantIndex = (args.plantId - 1L).toInt().coerceIn(0, PlantDefinitions.all.lastIndex)
        val plantStringId = PlantDefinitions.all.getOrNull(plantIndex)?.id
        Log.d("PlantDetail", "plantId=${args.plantId}, index=$plantIndex, stringId=$plantStringId, totalPlants=${PlantDefinitions.all.size}")

        if (plantStringId.isNullOrBlank()) {
            Log.e("PlantDetail", "无法解析植物ID: plantId=${args.plantId}, index=$plantIndex")
            // 不再navigateUp闪回，改为显示错误信息
            textTitle.text = "植物不存在"
            plantName.text = "未知"
            return
        }

        try {
            viewModel.loadPlant(plantStringId)
            Log.d("PlantDetail", "viewModel.loadPlant($plantStringId) 调用成功")
        } catch (e: Exception) {
            Log.e("PlantDetail", "加载植物详情失败", e)
            // 不再navigateUp闪回，降级显示
            textTitle.text = "加载失败"
            return
        }

        // 观察 UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    try {
                        renderState(state)
                    } catch (e: Exception) {
                        Log.e("PlantDetail", "渲染植物详情失败", e)
                    }
                }
            }
        }
    }

    private fun renderState(state: PlantDetailUiState) {
        try {
            Log.d("PlantDetail", "renderState: name=${state.name}, level=${state.level}, plantIdStr=${state.plantIdStr}")

            // 顶栏标题：植物名 · 路径名
            textTitle?.text = "${state.name} · ${state.pathName}"

            // 隐藏的植物名（保留数据绑定）— 全部安全调用
            plantName?.text = state.name
            pathName?.text = state.pathName

            // 等级显示格式：Lv.5 墨韵（使用 GrowthLevel 枚举定义，与引擎保持一致）
            val levelName = getLevelLabel(state.level)
            plantLevel?.text = "Lv.${state.level} $levelName"

            // 稀有度星标 - 使用淡墨色
            rarityText?.text = "★".repeat(state.rarity.coerceIn(1, 5)) + "☆".repeat((5 - state.rarity).coerceIn(0, 4))

            // 进度条
            levelProgressBar?.progress = state.levelProgress

            // 灌溉进度文字格式：XXh XXmin / 下一级
            val totalMinutes = state.totalReadMinutes
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val progressText = if (hours > 0) {
                "${hours}h ${minutes}min / 下一级"
            } else {
                "${minutes}min / 下一级"
            }
            levelProgressText?.text = progressText

            // 植物描述
            plantDescription?.text = state.description

            // 解锁条件
            val unlockMinutes = getUnlockMinutes(state.level)
            unlockCondition?.text = "解锁条件：累计阅读 ≥ ${unlockMinutes}min"

            // 枯萎预警
            if (state.witherStage > 0) {
                layoutWitherWarning?.visibility = View.VISIBLE
                witherWarning?.text = when {
                    state.witherStage >= 2 -> "未阅读 ${state.witherCountdownDays}天 · 严重枯萎，请尽快阅读！"
                    state.witherStage == 1 -> "未阅读 ${state.witherCountdownDays}天 · 轻度枯萎"
                    else -> "未阅读 ${state.witherCountdownDays}天"
                }
            } else if (state.witherCountdownDays >= 0) {
                layoutWitherWarning?.visibility = View.VISIBLE
                witherWarning?.text = "未阅读 ${state.witherCountdownDays}天 · 渐枯倒计时 ${state.witherCountdownDays}天"
            } else {
                layoutWitherWarning?.visibility = View.GONE
            }

            // 加载植物大图
            loadPlantImage(state)
        } catch (e: Exception) {
            Log.e("PlantDetail", "renderState异常", e)
        }
    }

    /**
     * 获取当前等级对应的解锁分钟数
     */
    private fun getUnlockMinutes(level: Int): Int {
        return when (level) {
            1 -> 30
            2 -> 120
            3 -> 480
            4 -> 1920
            5 -> 7680
            else -> 30
        }
    }

    /**
     * 加载植物大图 - 使用 plantIdStr (String) 直接加载
     */
    private fun loadPlantImage(state: PlantDetailUiState) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    // 使用 loadByStringId 直接传入字符串ID
                    PlantImageLoader.loadByStringId(ctx, state.plantIdStr, state.level, state.witherStage)
                } catch (e: Exception) {
                    Log.e("PlantDetail", "加载植物图片失败: ${state.plantIdStr}", e)
                    null
                }
            }
            // 确保Fragment仍然存活
            if (view != null && isAdded) {
                bitmap?.let {
                    plantImage.setImageBitmap(it)
                }
            }
        }
    }
}
