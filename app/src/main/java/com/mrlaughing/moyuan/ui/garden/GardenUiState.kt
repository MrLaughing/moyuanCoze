package com.mrlaughing.moyuan.ui.garden

import android.graphics.Bitmap
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather

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

data class PlantUiItem(
    val plantId: Long,
    val name: String,
    val level: Int,
    val witherStage: Int,
    val pathType: Int,
    val bitmap: Bitmap? = null
)
