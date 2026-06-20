package com.mrlaughing.moyuan.ui.garden

import android.graphics.Bitmap
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather

/**
 * 花园 ViewModel UI 状态
 */
data class GardenUiState(
    val plants: List<PlantUiItem> = emptyList(),
    val season: Season = Season.SPRING,
    val weather: Weather = Weather.CLEAR,
    val todayReadMinutes: Int = 0,
    val streakDays: Int = 0,
    val bonusMultiplier: Float = 1.0f,
    val dateText: String = "",
    val irrigationHours: Int = 0,
    val irrigationGoal: Int = 40
)
