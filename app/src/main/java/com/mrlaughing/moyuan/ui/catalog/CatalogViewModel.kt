package com.mrlaughing.moyuan.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.PlantPath
import com.mrlaughing.moyuan.data.model.PlantRarity
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 图鉴 ViewModel
 *
 * 从 Repository 加载真实数据：
 * - PlantRepository.observePlants() 获取已解锁植物
 * - PlantDefinitions.all 获取全部 27 种植物定义
 * - 未解锁植物显示锁定状态
 *
 * 注意：unlockDate 为 null 表示未解锁，非 null 表示已解锁
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _pathFilter = MutableStateFlow(Constants.PATH_ALL)
    val pathFilter: StateFlow<Int> = _pathFilter.asStateFlow()

    private val _allPlants = MutableStateFlow<List<CatalogPlantItem>>(emptyList())
    val allPlants: StateFlow<List<CatalogPlantItem>> = _allPlants.asStateFlow()

    private val _filteredPlants = MutableStateFlow<List<CatalogPlantItem>>(emptyList())
    val filtered: StateFlow<List<CatalogPlantItem>> = _filteredPlants.asStateFlow()

    init {
        loadPlants()
    }

    /**
     * 从 Repository 加载植物数据
     *
     * DB 里只有已解锁的植物（unlockDate != null），需要结合 PlantDefinitions 的全部 27 种
     * 已解锁的显示真实等级，未解锁的显示锁定状态
     */
    private fun loadPlants() {
        viewModelScope.launch {
            // 监听已解锁的植物
            plantRepository.observePlants().collect { unlockedPlants ->
                // 构建解锁植物的 Map（unlockDate 非 null 的才是已解锁）
                val unlockedMap = unlockedPlants.associateBy { it.plantId }

                // 合并 PlantDefinitions 的全部 27 种植物
                val allCatalogItems = PlantDefinitions.all.map { plantDef ->
                    val dbPlant = unlockedMap[plantDef.id]

                    // unlockDate != null 表示已解锁
                    val isUnlocked = dbPlant?.unlockDate != null

                    // 获取解锁条件描述
                    val unlockCondition = getUnlockCondition(plantDef)

                    // 获取路径类型
                    val pathType = pathToConstant(plantDef.path)

                    CatalogPlantItem(
                        plantId = PlantDefinitions.all.indexOf(plantDef) + 1L,
                        plantStringId = plantDef.id,  // 直接使用字符串ID，不再依赖Long索引映射
                        name = plantDef.name,
                        level = if (isUnlocked) dbPlant?.level ?: 1 else 0,
                        rarity = rarityToInt(plantDef.rarity),
                        pathType = pathType,
                        isUnlocked = isUnlocked,
                        unlockCondition = unlockCondition
                    )
                }

                _allPlants.value = allCatalogItems
                applyFilter(allCatalogItems, _pathFilter.value)
            }
        }
    }

    /**
     * 应用路径筛选
     */
    private fun applyFilter(plants: List<CatalogPlantItem>, filter: Int) {
        _filteredPlants.value = if (filter == Constants.PATH_ALL) {
            plants
        } else {
            plants.filter { it.pathType == filter }
        }
    }

    /**
     * 设置路径筛选
     */
    fun setPathFilter(filter: Int) {
        _pathFilter.value = filter
        applyFilter(_allPlants.value, filter)
    }

    /**
     * 将 PlantPath 转换为 Constants 中的路径常量
     */
    private fun pathToConstant(path: PlantPath): Int {
        return when (path) {
            PlantPath.JIMO -> Constants.PATH_JIMO
            PlantPath.BINGZHU -> Constants.PATH_BINGZHU
            PlantPath.SUIHAN -> Constants.PATH_SUIHAN
            PlantPath.XUNFANG -> Constants.PATH_XUNFANG
            PlantPath.HIDDEN -> Constants.PATH_HIDDEN
        }
    }

    /**
     * 将 PlantRarity 转换为数字
     */
    private fun rarityToInt(rarity: PlantRarity): Int {
        return when (rarity) {
            PlantRarity.COMMON -> 1
            PlantRarity.RARE -> 2
            PlantRarity.LEGENDARY -> 3
            PlantRarity.HIDDEN -> 4
        }
    }

    /**
     * 获取解锁条件描述
     */
    private fun getUnlockCondition(plant: Plant): String {
        return when (plant.path) {
            PlantPath.JIMO -> "累计阅读 ${plant.unlockThreshold} 分钟"
            PlantPath.BINGZHU -> "累计夜读 ${plant.unlockThreshold} 天"
            PlantPath.SUIHAN -> "连续阅读 ${plant.unlockThreshold} 天"
            PlantPath.XUNFANG -> "阅读 ${plant.unlockThreshold} 本书"
            PlantPath.HIDDEN -> "特殊条件解锁"
        }
    }
}

/**
 * 图鉴植物卡片数据
 * 
 * plantStringId 是植物的字符串标识符（如 "changpu", "orchid"），用于稳定地加载图片
 * plantId 是 Long 类型的索引（从1开始），仅用于兼容旧代码
 */
data class CatalogPlantItem(
    val plantId: Long,          // Long索引（兼容用）
    val plantStringId: String,  // 字符串ID（用于稳定加载）
    val name: String,
    val level: Int,
    val rarity: Int,         // 1-4 星
    val pathType: Int,
    val isUnlocked: Boolean,
    val unlockCondition: String  // 解锁条件描述
)
