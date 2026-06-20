package com.mrlaughing.moyuan.data.model

enum class WitherStage(val stage: Int, val label: String, val thresholdDays: Int) {
    NONE(0, "正常", 0),
    FADE(1, "初淡", 2),
    WITHER(2, "渐枯", 4),
    SEVERE(3, "将枯", 7),
    DEAD(4, "枯寂", 14);

    companion object {
        fun fromDays(daysSinceRead: Int): WitherStage {
            return entries.sortedByDescending { it.thresholdDays }
                .first { daysSinceRead >= it.thresholdDays }
        }
    }
}
