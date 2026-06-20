package com.mrlaughing.moyuan.ui.plant

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.util.Constants
import javax.inject.Inject

/**
 * 植物详情 ViewModel
 */
@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    // 注入 PlantRepository（待实现）
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载植物详情
     */
    fun loadPlant(plantId: Long) {
        viewModelScope.launch {
            // TODO: 从 Repository 加载真实数据
            _uiState.value = PlantDetailUiState(
                plantId = plantId,
                name = "墨兰",
                level = 3,
                maxLevel = Constants.MAX_LEVEL,
                totalReadMinutes = 280,
                levelProgress = calculateLevelProgress(280, 3),
                description = "墨兰，生于幽谷，不争不抢。需要细心的浇灌才能绽放。",
                witherStage = 0,
                witherCountdownDays = -1,
                pathName = "草本",
                rarity = 1
            )
        }
    }

    private fun calculateLevelProgress(currentMinutes: Int, currentLevel: Int): Float {
        if (currentLevel >= Constants.MAX_LEVEL) return 1.0f
        val currentThreshold = Constants.LEVEL_THRESHOLDS[currentLevel - 1]
        val nextThreshold = Constants.LEVEL_THRESHOLDS[currentLevel]
        if (nextThreshold <= currentThreshold) return 1.0f
        return ((currentMinutes - currentThreshold).toFloat() /
                (nextThreshold - currentThreshold)).coerceIn(0f, 1f)
    }
}

data class PlantDetailUiState(
    val plantId: Long = 0,
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
