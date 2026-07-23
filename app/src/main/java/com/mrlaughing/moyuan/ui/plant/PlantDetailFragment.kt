package com.mrlaughing.moyuan.ui.plant

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.render.PlantImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 植物详情 Fragment
 * v2.0：展示大图、文化小传（描述 + 诗文引用）、解锁条件
 */
@AndroidEntryPoint
class PlantDetailFragment : Fragment() {

    private val viewModel: PlantDetailViewModel by viewModels()
    private val args: PlantDetailFragmentArgs by navArgs()

    private lateinit var plantImage: ImageView
    private lateinit var textTitle: TextView
    private lateinit var plantDescription: TextView
    private lateinit var plantLore: TextView
    private lateinit var unlockCondition: TextView
    private lateinit var gardenToggleButton: TextView
    private lateinit var backButton: View

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

        try {
            plantImage = view.findViewById(R.id.image_plant_detail)
            textTitle = view.findViewById(R.id.text_title)
            plantDescription = view.findViewById(R.id.text_plant_description)
            plantLore = view.findViewById(R.id.text_plant_lore)
            unlockCondition = view.findViewById(R.id.text_unlock_condition)
            gardenToggleButton = view.findViewById(R.id.button_garden_toggle)
            backButton = view.findViewById(R.id.button_back)
            Log.d("PlantDetail", "视图初始化完成")
        } catch (e: Exception) {
            Log.e("PlantDetail", "视图初始化失败!!!", e)
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

        // 解析植物 String ID
        val plantIndex = (args.plantId - 1L).toInt().coerceIn(0, PlantDefinitions.all.lastIndex)
        val plantStringId = PlantDefinitions.all.getOrNull(plantIndex)?.id

        if (plantStringId.isNullOrBlank()) {
            Log.e("PlantDetail", "无法解析植物ID: plantId=${args.plantId}, index=$plantIndex")
            textTitle.text = "植物不存在"
            return
        }

        try {
            viewModel.loadPlant(plantStringId)
            Log.d("PlantDetail", "viewModel.loadPlant($plantStringId) 调用成功")
        } catch (e: Exception) {
            Log.e("PlantDetail", "加载植物详情失败", e)
            textTitle.text = "加载失败"
            return
        }

        // 观察 UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        try {
                            renderState(state)
                        } catch (e: Exception) {
                            Log.e("PlantDetail", "渲染植物详情失败", e)
                        }
                    }
                }
                launch {
                    viewModel.messages.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderState(state: PlantDetailUiState) {
        try {
            Log.d("PlantDetail", "renderState: name=${state.name}, plantIdStr=${state.plantIdStr}")

            // 顶栏标题：植物名
            textTitle?.text = state.name

            if (state.isUnlocked) {
                plantDescription.text = state.description
                plantLore.text = state.lore
                unlockCondition.text = state.unlockDate?.let {
                    getString(R.string.plant_discovered_on, it)
                } ?: getString(R.string.plant_discovered)
                gardenToggleButton?.visibility = View.VISIBLE
                gardenToggleButton?.text = if (state.isInGarden) {
                    getString(R.string.label_remove_from_garden)
                } else {
                    getString(R.string.label_put_in_garden)
                }
                gardenToggleButton?.setOnClickListener {
                    try { viewModel.toggleGardenStatus() } catch (e: Exception) {
                        Log.e("PlantDetail", "切换花园状态失败", e)
                    }
                }
            } else {
                plantDescription.text = getString(R.string.plant_undiscovered_description)
                plantLore.text = getString(R.string.plant_undiscovered_lore)
                unlockCondition.text = getString(R.string.msg_random_discovery)
                gardenToggleButton?.visibility = View.GONE
            }

            // 加载植物大图
            loadPlantImage(state)
        } catch (e: Exception) {
            Log.e("PlantDetail", "renderState异常", e)
        }
    }

    private fun loadPlantImage(state: PlantDetailUiState) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    if (state.isUnlocked) {
                        PlantImageLoader.loadByStringId(ctx, state.plantIdStr)
                    } else {
                        PlantImageLoader.loadSilhouetteByStringId(ctx, state.plantIdStr)
                    }
                } catch (e: Exception) {
                    Log.e("PlantDetail", "加载植物图片失败: ${state.plantIdStr}", e)
                    null
                }
            }
            if (view != null && isAdded) {
                bitmap?.let {
                    plantImage.setImageBitmap(it)
                }
            }
        }
    }
}
