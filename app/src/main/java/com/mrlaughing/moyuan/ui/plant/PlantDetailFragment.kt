package com.mrlaughing.moyuan.ui.plant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.PlantImageLoader
import com.mrlaughing.moyuan.ui.common.EinkProgressBar
import kotlinx.coroutines.launch

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

    /** 等级名映射 */
    private val levelNames = mapOf(
        1 to "萌芽",
        2 to "墨枝",
        3 to "墨叶",
        4 to "墨韵",
        5 to "墨华"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plant_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // 加载植物数据
        viewModel.loadPlant(args.plantId)

        // 观察 UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: PlantDetailUiState) {
        // 顶栏标题：植物名 · 路径名
        textTitle.text = "${state.name} · ${state.pathName}"
        
        // 隐藏的植物名（保留数据绑定）
        plantName.text = state.name
        pathName.text = state.pathName
        
        // 等级显示格式：Lv.5 墨韵
        val levelName = levelNames[state.level] ?: "萌芽"
        plantLevel.text = "Lv.${state.level} $levelName"
        
        // 稀有度星标 - 使用淡墨色
        rarityText.text = "★".repeat(state.rarity) + "☆".repeat(5 - state.rarity)
        
        // 进度条
        levelProgressBar.progress = state.levelProgress
        
        // 灌溉进度文字格式：XXh XXmin / 下一级
        val totalMinutes = state.totalReadMinutes
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val progressText = if (hours > 0) {
            "${hours}h ${minutes}min / 下一级"
        } else {
            "${minutes}min / 下一级"
        }
        levelProgressText.text = progressText
        
        // 植物描述
        plantDescription.text = state.description
        
        // 解锁条件
        val unlockMinutes = getUnlockMinutes(state.level)
        unlockCondition.text = "解锁条件：累计阅读 ≥ ${unlockMinutes}min"

        // 枯萎预警
        if (state.witherStage > 0) {
            layoutWitherWarning.visibility = View.VISIBLE
            witherWarning.text = when {
                state.witherStage >= 2 -> "未阅读 ${state.witherCountdownDays}天 · 严重枯萎，请尽快阅读！"
                state.witherStage == 1 -> "未阅读 ${state.witherCountdownDays}天 · 轻度枯萎"
                else -> "未阅读 ${state.witherCountdownDays}天"
            }
        } else if (state.witherCountdownDays >= 0) {
            layoutWitherWarning.visibility = View.VISIBLE
            witherWarning.text = "未阅读 ${state.witherCountdownDays}天 · 渐枯倒计时 ${state.witherCountdownDays}天"
        } else {
            layoutWitherWarning.visibility = View.GONE
        }

        // 加载植物大图
        loadPlantImage(state)
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

    private fun loadPlantImage(state: PlantDetailUiState) {
        val context = context ?: return
        Thread {
            val bitmap = PlantImageLoader.load(
                context,
                state.plantId,
                state.level,
                state.witherStage
            )
            bitmap?.let {
                activity?.runOnUiThread {
                    plantImage.setImageBitmap(it)
                }
            }
        }.start()
    }
}
