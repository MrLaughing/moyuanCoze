package com.mrlaughing.moyuan.data.model

enum class Season(val label: String, val multiplier: Float) {
    SPRING("春·萌芽", 1.2f),
    SUMMER("夏·盛放", 1.2f),
    AUTUMN("秋·收获", 1.3f),
    WINTER("冬·沉淀", 1.2f);

    companion object {
        fun fromMonth(month: Int): Season = when (month) {
            3, 4 -> SPRING
            5, 6, 7 -> SUMMER
            8, 9 -> AUTUMN
            else -> WINTER
        }
    }
}
