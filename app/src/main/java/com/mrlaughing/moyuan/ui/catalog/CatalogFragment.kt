package com.mrlaughing.moyuan.ui.catalog

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.ui.common.GridSpacingItemDecoration
import com.mrlaughing.moyuan.util.ScreenUtils
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CatalogFragment : Fragment() {

    private val viewModel: CatalogViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlantCardAdapter
    private lateinit var unlockedCountText: TextView
    
    private lateinit var chipAll: Chip
    private lateinit var chipJimo: Chip
    private lateinit var chipBingzhu: Chip
    private lateinit var chipSuihan: Chip
    private lateinit var chipXunfang: Chip
    private lateinit var chipHidden: Chip
    private lateinit var selectedIndicator: View
    
    private val chipList = mutableListOf<Chip>()
    private var currentSelectedIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_catalog)
        unlockedCountText = view.findViewById(R.id.text_unlocked_count)
        selectedIndicator = view.findViewById(R.id.view_selected_indicator)
        
        // 获取 Chip 引用
        chipAll = view.findViewById(R.id.chip_all)
        chipJimo = view.findViewById(R.id.chip_jimo)
        chipBingzhu = view.findViewById(R.id.chip_bingzhu)
        chipSuihan = view.findViewById(R.id.chip_suihan)
        chipXunfang = view.findViewById(R.id.chip_xunfang)
        chipHidden = view.findViewById(R.id.chip_hidden)
        
        chipList.addAll(listOf(chipAll, chipJimo, chipBingzhu, chipSuihan, chipXunfang, chipHidden))

        adapter = PlantCardAdapter { plant ->
            // 无论已解锁还是未解锁，都可以进入详情页查看
            navigateToPlantDetail(plant.plantId)
        }

        val columns = ScreenUtils.getRecommendedGridColumns(requireContext())
        recyclerView.layoutManager = GridLayoutManager(context, columns)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        // 卡片间距：8dp（dp转px）
        val spacingPx = (8 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(columns, spacingPx, includeEdge = false))

        setupChips()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filtered.collect { plants ->
                        adapter.submitList(plants)
                    }
                }
                launch {
                    viewModel.allPlants.collect { plants ->
                        val unlockedCount = plants.count { it.isUnlocked }
                        val totalCount = plants.size
                        unlockedCountText.text = "${getString(R.string.label_unlocked)} $unlockedCount/$totalCount"
                    }
                }
            }
        }
    }

    private fun setupChips() {
        chipList.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                selectChip(index)
                viewModel.setPathFilter(index)
            }
        }
        
        // 默认选中第一个
        selectChip(0)
    }
    
    private fun selectChip(index: Int) {
        currentSelectedIndex = index
        
        // 更新 Chip 样式
        chipList.forEachIndexed { i, chip ->
            if (i == index) {
                // 选中态：深色背景、白色文字
                chip.setChipBackgroundColorResource(R.color.ink_dark)
                chip.setTextColor(resources.getColor(R.color.white, null))
            } else {
                // 未选中态：浅色背景、深色文字
                chip.setChipBackgroundColorResource(R.color.surface)
                chip.setTextColor(resources.getColor(R.color.text_primary, null))
            }
        }
    }

    /**
     * 导航到植物详情页 - 使用 plantStringId（String）传递参数
     */
    private fun navigateToPlantDetail(plantId: Long) {
        val direction = CatalogFragmentDirections
            .actionCatalogFragmentToPlantDetailFragment(plantId)
        findNavController().navigate(direction)
    }
}

