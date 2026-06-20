package com.mrlaughing.moyuan.ui.catalog

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.mrlaughing.moyuan.util.Constants

/**
 * 图鉴 ViewModel
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    // 注入 PlantRepository, UnlockEngine（待实现）
    // private val plantRepository: PlantRepository,
    // private val unlockEngine: UnlockEngine
) : ViewModel() {

    private val _pathFilter = MutableStateFlow(Constants.PATH_ALL)
    val pathFilter: StateFlow<Int> = _pathFilter.asStateFlow()

    private val _allPlants = MutableStateFlow<List<CatalogPlantItem>>(emptyList())
    val allPlants: StateFlow<List<CatalogPlantItem>> = _allPlants.asStateFlow()

    /** 根据路径筛选后的植物列表 */
    val filteredPlants: StateFlow<List<CatalogPlantItem>> = combine(
        _allPlants,
        _pathFilter
    ) { plants, filter ->
        if (filter == Constants.PATH_ALL) plants
        else plants.filter { it.pathType == filter }
    }.let { flow ->
        val stateFlow = MutableStateFlow<List<CatalogPlantItem>>(emptyList())
        // 简化：直接收集
        stateFlow
    }

    private val _filteredPlants = MutableStateFlow<List<CatalogPlantItem>>(emptyList())
    val filtered: StateFlow<List<CatalogPlantItem>> = _filteredPlants.asStateFlow()

    init {
        loadPlants()
    }

    private fun loadPlants() {
        // TODO: 从 Repository 加载
        // 模拟数据
        val mockPlants = listOf(
            CatalogPlantItem(1L, "墨兰", 3, 1, Constants.PATH_JIMO, true, "累计阅读200分钟"),
            CatalogPlantItem(2L, "青竹", 5, 2, Constants.PATH_SUIHAN, true, "累计阅读500分钟"),
            CatalogPlantItem(3L, "紫藤", 2, 1, Constants.PATH_XUNFANG, true, "累计阅读60分钟"),
            CatalogPlantItem(4L, "白莲", 1, 3, Constants.PATH_BINGZHU, true, "累计阅读0分钟"),
            CatalogPlantItem(5L, "苍松", 4, 2, Constants.PATH_SUIHAN, true, "累计阅读300分钟"),
            CatalogPlantItem(6L, "幽兰", 1, 4, Constants.PATH_JIMO, false, "累计阅读1000分钟"),
            CatalogPlantItem(7L, "寒梅", 1, 5, Constants.PATH_SUIHAN, false, "连续阅读30天"),
            CatalogPlantItem(8L, "荷韵", 1, 3, Constants.PATH_BINGZHU, false, "累计阅读800分钟"),
            CatalogPlantItem(9L, "藤萝", 1, 2, Constants.PATH_XUNFANG, false, "累计阅读400分钟"),
            CatalogPlantItem(10L, "蒲草", 1, 1, Constants.PATH_JIMO, false, "累计阅读100分钟")
        )
        _allPlants.value = mockPlants
        _filteredPlants.value = mockPlants
    }

    /**
     * 设置路径筛选
     */
    fun setPathFilter(filter: Int) {
        _pathFilter.value = filter
        _filteredPlants.value = if (filter == Constants.PATH_ALL) {
            _allPlants.value
        } else {
            _allPlants.value.filter { it.pathType == filter }
        }
    }
}

/**
 * 图鉴植物卡片数据
 */
data class CatalogPlantItem(
    val plantId: Long,
    val name: String,
    val level: Int,
    val rarity: Int,         // 1-5 星
    val pathType: Int,
    val isUnlocked: Boolean,
    val unlockCondition: String  // 解锁条件描述
)
