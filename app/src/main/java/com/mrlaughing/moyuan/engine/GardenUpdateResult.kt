package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.Season

data class GardenUpdateResult(
    val meta: EngineMeta,
    val plants: List<EnginePlantState>,
    val season: Season,
    val newlyUnlocked: List<Plant>
)
