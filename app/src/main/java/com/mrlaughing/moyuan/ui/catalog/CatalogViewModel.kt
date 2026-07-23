package com.mrlaughing.moyuan.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 图鉴 ViewModel
 *
 * 从 Repository 加载真实数据：
 * - PlantRepository.observePlants() 获取已解锁植物
 * - PlantDefinitions.all 获取全部 50 种植物定义
 * - 未解锁植物显示锁定状态
 *
 * 注意：unlockDate 为 null 表示未解锁，非 null 表示已解锁
 *
 * v2.0：移除路径筛选、等级、稀有度
 */
@HiltViewModel
@OptIn(kotlinx.coroutines.FlowPreview::class)
class CatalogViewModel @Inject constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _allPlants = MutableStateFlow<List<CatalogPlantItem>>(emptyList())
    private val _filter = MutableStateFlow(CatalogFilter.ALL)
    val filter: StateFlow<CatalogFilter> = _filter.asStateFlow()
    val plants: StateFlow<List<CatalogPlantItem>> = combine(_allPlants, _filter) { plants, filter ->
        when (filter) {
            CatalogFilter.ALL -> plants
            CatalogFilter.DISCOVERED -> plants.filter { it.isUnlocked }
            CatalogFilter.UNDISCOVERED -> plants.filterNot { it.isUnlocked }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 保留 allPlants 别名兼容 CatalogFragment（内部已传入 plants） */
    val filtered: StateFlow<List<CatalogPlantItem>> = plants
    val allPlants: StateFlow<List<CatalogPlantItem>> = _allPlants.asStateFlow()

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch {
            plantRepository.observePlants()
                .distinctUntilChanged()
                .debounce(150)
                .collect { unlockedPlants ->
                val unlockedMap = unlockedPlants.associateBy { it.plantId }

                val items = PlantDefinitions.all.map { plantDef ->
                    val dbPlant = unlockedMap[plantDef.id]
                    val isUnlocked = dbPlant?.unlockDate != null

                    CatalogPlantItem(
                        plantId = PlantDefinitions.all.indexOf(plantDef) + 1L,
                        plantStringId = plantDef.id,
                        name = plantDef.name,
                        isUnlocked = isUnlocked,
                        unlockThreshold = plantDef.unlockThreshold,
                        unlockCondition = "随阅读进度随机发现"
                    )
                }

                _allPlants.value = items
            }
        }
    }

    fun setFilter(filter: CatalogFilter) {
        _filter.value = filter
    }
}

enum class CatalogFilter { ALL, DISCOVERED, UNDISCOVERED }

/**
 * 图鉴植物卡片数据
 * v2.0：精简为仅基础字段，移除等级/稀有度/路径
 */
data class CatalogPlantItem(
    val plantId: Long,          // Long索引（兼容导航用）
    val plantStringId: String,  // 字符串ID（用于稳定加载图片）
    val name: String,
    val isUnlocked: Boolean,
    val unlockThreshold: Int = 0,       // 解锁所需分钟数
    val unlockCondition: String = ""    // 解锁条件描述（备用）
)
