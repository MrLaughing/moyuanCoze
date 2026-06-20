package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.GrowthLevel

data class LevelResult(
    val level: GrowthLevel,
    val progressInLevel: Int,
    val minutesToNext: Int,
    val progressPercent: Float
)
