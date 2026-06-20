package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.WitherStage

data class WitherResult(
    val stage: WitherStage,
    val daysSinceLastRead: Int,
    val isWithering: Boolean,
    val justDied: Boolean
)

data class RecoveryResult(
    val recoveredMinutes: Int,
    val recoveredLevel: GrowthLevelResult,
    val justRevived: Boolean,
    val retentionRatio: Float
)

data class GrowthLevelResult(
    val level: Int,
    val label: String
)

data class WitherCountdown(
    val currentStage: WitherStage,
    val nextStage: WitherStage?,
    val daysToNext: Int,
    val daysToDeath: Int
)
