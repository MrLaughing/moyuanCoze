package com.mrlaughing.moyuan.ui.plant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.PlantImageLoader
import com.mrlaughing.moyuan.ui.common.EinkProgressBar
import com.mrlaughing.moyuan.util.formatMinutes
import kotlinx.coroutines.launch

/**
 * 植物详情 Fragment
 */
class PlantDetailFragment : Fragment() {

    private val viewModel: PlantDetailViewModel by viewModels()
    private val args: PlantDetailFragmentArgs by navArgs()

    private lateinit var plantImage: ImageView
    private lateinit var plantName: TextView
    private lateinit var plantLevel: TextView
    private lateinit var levelProgressBar: EinkProgressBar
    private lateinit var levelProgressText: TextView
    private lateinit var plantDescription: TextView
    private lateinit var witherWarning: TextView
    private lateinit var backButton: View
    private lateinit var pathName: TextView
    private lateinit var rarityText: TextView

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
        plantLevel = view.findViewById(R.id.text_plant_level)
        levelProgressBar = view.findViewById(R.id.progress_level)
        levelProgressText = view.findViewById(R.id.text_level_progress)
        plantDescription = view.findViewById(R.id.text_plant_description)
        witherWarning = view.findViewById(R.id.text_wither_warning)
        backButton = view.findViewById(R.id.button_back)
        pathName = view.findViewById(R.id.text_path_name)
        rarityText = view.findViewById(R.id.text_rarity)

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
        plantName.text = state.name
        plantLevel.text = "Lv.${state.level}"
        levelProgressBar.progress = state.levelProgress
        levelProgressText.text = "${(state.levelProgress * 100).toInt()}% → Lv.${state.level + 1}"
        plantDescription.text = state.description
        pathName.text = state.pathName
        rarityText.text = "★".repeat(state.rarity) + "☆".repeat(5 - state.rarity)

        // 枯萎预警
        if (state.witherCountdownDays >= 0) {
            witherWarning.visibility = View.VISIBLE
            witherWarning.text = when {
                state.witherStage >= 2 -> "⚠ 植物已严重枯萎，请尽快阅读！"
                state.witherStage == 1 -> "⚠ 植物轻度枯萎，还有${state.witherCountdownDays}天"
                else -> "还有${state.witherCountdownDays}天未阅读将枯萎"
            }
        } else {
            witherWarning.visibility = View.GONE
        }

        // 加载植物大图
        loadPlantImage(state)
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
