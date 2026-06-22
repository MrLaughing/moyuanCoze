package com.mrlaughing.moyuan.ui.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import android.util.Log

/**
 * 植物详情 ViewModel
 *
 * 从 Repository 加载真实数据：
 * - PlantRepository.observePlant(plantId) 获取植物详情
 * - PlantDefinitions.getById() 获取植物定义信息
 *
 * unlockDate != null 表示已解锁
 */
@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载植物详情 - 使用 String plantId 直接查找，不再依赖 Long 索引
     */
    fun loadPlant(plantStringId: String) {
        viewModelScope.launch {
            try {
                // 使用 PlantDefinitions.getById() 直接通过字符串ID查找
                val plantDef = PlantDefinitions.getById(plantStringId)
                if (plantDef == null) {
                    Log.e("PlantDetailVM", "未找到植物定义: $plantStringId")
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = "未知植物",
                        level = 1
                    )
                    return@launch
                }

                Log.d("PlantDetailVM", "加载植物: $plantStringId (${plantDef.name})")

                // 观察该植物的状态 — 内层也加 try-catch 防御
                plantRepository.observePlant(plantStringId).collect { entity ->
                    try {
                        if (entity != null && !entity.unlockDate.isNullOrEmpty()) {
                            // 已解锁，显示真实数据
                            val safeLevel = entity.level.coerceIn(1, Constants.MAX_LEVEL)
                            val levelProgress = calculateLevelProgress(
                                entity.accumulatedMinutes,
                                safeLevel
                            )
                            _uiState.value = PlantDetailUiState(
                                plantIdStr = plantStringId,
                                name = plantDef.name,
                                level = safeLevel,
                                maxLevel = Constants.MAX_LEVEL,
                                totalReadMinutes = entity.accumulatedMinutes,
                                levelProgress = levelProgress,
                                description = plantDef.description,
                                witherStage = entity.witherStage.coerceIn(0, 4),
                                witherCountdownDays = -1,
                                pathName = pathToName(plantDef.path),
                                rarity = rarityToInt(plantDef.rarity)
                            )
                        } else {
                            // 未解锁，显示植物定义信息
                            _uiState.value = PlantDetailUiState(
                                plantIdStr = plantStringId,
                                name = plantDef.name,
                                level = 1,
                                maxLevel = Constants.MAX_LEVEL,
                                totalReadMinutes = 0,
                                levelProgress = 0f,
                                description = plantDef.description,
                                witherStage = 0,
                                witherCountdownDays = -1,
                                pathName = pathToName(plantDef.path),
                                rarity = rarityToInt(plantDef.rarity)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("PlantDetailVM", "处理植物状态失败", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantDetailVM", "加载植物详情失败 plantStringId=$plantStringId", e)
                // 异常时降级：显示植物基本信息，避免崩溃
                val plantDef = PlantDefinitions.getById(plantStringId)
                if (plantDef != null) {
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = plantDef.name,
                        level = 1,
                        maxLevel = Constants.MAX_LEVEL,
                        totalReadMinutes = 0,
                        levelProgress = 0f,
                        description = plantDef.description,
                        witherStage = 0,
                        witherCountdownDays = -1,
                        pathName = pathToName(plantDef.path),
                        rarity = rarityToInt(plantDef.rarity)
                    )
                } else {
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = "未知植物",
                        level = 1
                    )
                }
            }
        }
    }

    /**
     * 计算等级进度
     */
    private fun calculateLevelProgress(currentMinutes: Int, currentLevel: Int): Float {
        if (currentLevel >= Constants.MAX_LEVEL) return 1.0f
        if (currentLevel <= 0) return 0f  // 防御：level=0时返回0进度
        val currentThreshold = Constants.LEVEL_THRESHOLDS[currentLevel - 1]
        val nextThreshold = Constants.LEVEL_THRESHOLDS.getOrElse(currentLevel) { Constants.LEVEL_THRESHOLDS.last() }
        if (nextThreshold <= currentThreshold) return 1.0f
        return ((currentMinutes - currentThreshold).toFloat() /
                (nextThreshold - currentThreshold)).coerceIn(0f, 1f)
    }

    /**
     * 将 PlantPath 转换为中文名称
     */
    private fun pathToName(path: PlantPath): String {
        return when (path) {
            PlantPath.JIMO -> "积墨"
            PlantPath.BINGZHU -> "秉烛"
            PlantPath.SUIHAN -> "岁寒"
            PlantPath.XUNFANG -> "寻芳"
            PlantPath.HIDDEN -> "隐藏"
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
}

/**
 * 植物详情 UI 状态
 * plantIdStr: 字符串ID，用于稳定地加载图片
 */
data class PlantDetailUiState(
    val plantIdStr: String = "",   // 字符串ID（主键）
    val name: String = "",
    val level: Int = 1,
    val maxLevel: Int = Constants.MAX_LEVEL,
    val totalReadMinutes: Int = 0,
    val levelProgress: Float = 0f,
    val description: String = "",
    val witherStage: Int = 0,
    val witherCountdownDays: Int = -1,  // -1 表示无枯萎风险
    val pathName: String = "",
    val rarity: Int = 1
)
