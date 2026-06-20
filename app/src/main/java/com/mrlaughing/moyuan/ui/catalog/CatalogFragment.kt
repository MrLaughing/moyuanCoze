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
import com.google.android.material.chip.ChipGroup
import com.mrlaughing.moyuan.R
import kotlinx.coroutines.launch

/**
 * 图鉴 Fragment：路径筛选标签 + 植物卡片网格
 */
@AndroidEntryPoint
class CatalogFragment : Fragment() {

    private val viewModel: CatalogViewModel by viewModels()

    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlantCardAdapter
    private lateinit var unlockedCountText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroup = view.findViewById(R.id.chip_group_path)
        recyclerView = view.findViewById(R.id.recycler_catalog)
        unlockedCountText = view.findViewById(R.id.text_unlocked_count)

        // 设置 RecyclerView 3列网格
        adapter = PlantCardAdapter { plant ->
            if (plant.isUnlocked) {
                navigateToPlantDetail(plant.plantId)
            }
        }
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        // 设置 Chip 筛选
        setupChips()

        // 观察数据
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
        // 获取所有 Chip
        val chips = listOf(
            view.findViewById<Chip>(R.id.chip_all),
            view.findViewById<Chip>(R.id.chip_herb),
            view.findViewById<Chip>(R.id.chip_woody),
            view.findViewById<Chip>(R.id.chip_vine),
            view.findViewById<Chip>(R.id.chip_aquatic),
            view.findViewById<Chip>(R.id.chip_accumulate)
        )

        chips.forEachIndexed { index, chip ->
            chip?.setOnClickListener {
                viewModel.setPathFilter(index)
            }
        }

        // 默认选中第一个
        chips[0]?.isChecked = true
    }

    private fun navigateToPlantDetail(plantId: Long) {
        val direction = CatalogFragmentDirections
            .actionCatalogFragmentToPlantDetailFragment(plantId)
        findNavController().navigate(direction)
    }
}
