package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather

data class GardenUpdateResult(
    val meta: EngineMeta,
    val plants: List<EnginePlantState>,
    val season: Season,
    val weather: Weather,
    val multiplier: Float,
    val newlyUnlocked: List<Plant>
)
