package com.mrlaughing.moyuan.ui.garden

import android.graphics.Bitmap
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather

/** 园圃布局配置 */
data class GridLayoutConfig(
    val cols: Int,
    val rows: Int,
    val label: String,
    val minUnlockedPlants: Int
) {
    val totalSlots: Int get() = cols * rows
}

/** 完整方阵经等距投影后组成一座连续菱形花圃。 */
val GRID_LAYOUTS = listOf(
    GridLayoutConfig(3, 3, "初庭", 0),
    GridLayoutConfig(4, 4, "雅庭", 9),
    GridLayoutConfig(5, 5, "盛庭", 16),
    GridLayoutConfig(6, 6, "繁庭", 25),
    GridLayoutConfig(7, 7, "满园", 36)
)

const val DEFAULT_GRID_INDEX = 0

/** 花园展示模式 */
enum class GardenMode {
    AUTO,   // 自动：展示全部已解锁植物
    CUSTOM  // 自定义：仅展示用户"放入花园"的植物
}

data class GardenUiState(
    val plants: List<PlantUiItem> = emptyList(),
    val season: Season = Season.SPRING,
    val weather: Weather = Weather.CLEAR,
    val todayReadMinutes: Int = 0,
    val streakDays: Int = 0,
    val dateText: String = "",
    val totalUnlocked: Int = 0,
    val totalPlants: Int = 20,
    val accumulatedMinutes: Int = 0,
    val nextUnlockThreshold: Int? = null,
    val isAuthorized: Boolean = false,
    val requiredSlots: Int = 0,
    /** 当前选中的园圃布局索引（在 GRID_LAYOUTS 中的位置） */
    val gridLayoutIndex: Int = DEFAULT_GRID_INDEX,
    /** 花园展示模式 */
    val gardenMode: GardenMode = GardenMode.AUTO
) {
    /** 快捷获取当前布局的列数和行数 */
    val gridCols: Int get() = GRID_LAYOUTS[gridLayoutIndex].cols
    val gridRows: Int get() = GRID_LAYOUTS[gridLayoutIndex].rows
}

data class PlantUiItem(
    val plantId: Long,
    val name: String,
    val level: Int,
    val bitmap: Bitmap? = null,
    val gardenSlot: Int? = null
)
