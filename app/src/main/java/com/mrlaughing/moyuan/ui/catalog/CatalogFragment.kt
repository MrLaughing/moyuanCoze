package com.mrlaughing.moyuan.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.util.Constants
import kotlinx.coroutines.launch

/**
 * 图鉴 Fragment：路径筛选 Tab + 植物卡片网格
 */
@AndroidEntryPoint
class CatalogFragment : Fragment() {

    private val viewModel: CatalogViewModel by viewModels()

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlantCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tab_layout)
        recyclerView = view.findViewById(R.id.recycler_catalog)

        // 设置 RecyclerView 3列网格
        adapter = PlantCardAdapter { plant ->
            if (plant.isUnlocked) {
                navigateToPlantDetail(plant.plantId)
            }
            // 未解锁的点击由 Adapter 内部 Toast 处理
        }
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null  // 禁用动画

        // 设置 Tab
        setupTabs()

        // 观察数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filtered.collect { plants ->
                    adapter.submitList(plants)
                }
            }
        }
    }

    private fun setupTabs() {
        Constants.PATH_NAMES.forEachIndexed { index, name ->
            val tab = tabLayout.newTab().setText(name)
            tabLayout.addTab(tab)

            if (index == 0) tab.select()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    viewModel.setPathFilter(position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun navigateToPlantDetail(plantId: Long) {
        val direction = CatalogFragmentDirections
            .actionCatalogFragmentToPlantDetailFragment(plantId)
        findNavController().navigate(direction)
    }
}
