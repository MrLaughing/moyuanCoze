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
    private lateinit var filterAll: TextView
    private lateinit var filterDiscovered: TextView
    private lateinit var filterUndiscovered: TextView

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
        filterAll = view.findViewById(R.id.filter_all)
        filterDiscovered = view.findViewById(R.id.filter_discovered)
        filterUndiscovered = view.findViewById(R.id.filter_undiscovered)

        filterAll.setOnClickListener { viewModel.setFilter(CatalogFilter.ALL) }
        filterDiscovered.setOnClickListener { viewModel.setFilter(CatalogFilter.DISCOVERED) }
        filterUndiscovered.setOnClickListener { viewModel.setFilter(CatalogFilter.UNDISCOVERED) }

        adapter = PlantCardAdapter { plant ->
            navigateToPlantDetail(plant.plantId)
        }
        adapter.setFragment(this)

        val columns = ScreenUtils.getRecommendedGridColumns(requireContext())
        recyclerView.layoutManager = GridLayoutManager(context, columns)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        val spacingPx = (8 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(columns, spacingPx, includeEdge = false))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.plants.collect { plants ->
                        adapter.submitList(plants)
                    }
                }
                launch {
                    viewModel.allPlants.collect { plants ->
                        unlockedCountText.text = getString(
                            R.string.catalog_discovered_count,
                            plants.count { it.isUnlocked },
                            plants.size
                        )
                    }
                }
                launch { viewModel.filter.collect(::renderFilter) }
            }
        }
    }

    private fun renderFilter(filter: CatalogFilter) {
        listOf(
            filterAll to CatalogFilter.ALL,
            filterDiscovered to CatalogFilter.DISCOVERED,
            filterUndiscovered to CatalogFilter.UNDISCOVERED
        ).forEach { (view, value) ->
            val selected = filter == value
            view.setBackgroundResource(if (selected) R.drawable.bg_pill_dark else R.drawable.bg_pill_light)
            view.setTextColor(requireContext().getColor(if (selected) R.color.text_on_dark else R.color.text_secondary))
        }
    }

    private fun navigateToPlantDetail(plantId: Long) {
        try {
            val direction = CatalogFragmentDirections
                .actionCatalogFragmentToPlantDetailFragment(plantId)
            findNavController().navigate(direction)
        } catch (e: Exception) {
            android.util.Log.e("CatalogFragment", "导航到植物详情失败: plantId=$plantId", e)
            android.widget.Toast.makeText(
                requireContext(),
                "暂时无法查看植物详情",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
