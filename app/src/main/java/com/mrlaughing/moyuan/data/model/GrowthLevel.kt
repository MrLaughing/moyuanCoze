package com.mrlaughing.moyuan.data.model

enum class GrowthLevel(val level: Int, val label: String, val thresholdMinutes: Int) {
    LV1(1, "墨芽", 0),
    LV2(2, "墨枝", 120),
    LV3(3, "墨苞", 480),
    LV4(4, "墨花", 1200),
    LV5(5, "墨韵", 2400);

    companion object {
        fun fromMinutes(accumulatedMinutes: Int): GrowthLevel {
            return entries.sortedByDescending { it.thresholdMinutes }
                .first { accumulatedMinutes >= it.thresholdMinutes }
        }
    }
}
