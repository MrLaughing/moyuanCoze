package com.mrlaughing.moyuan.data.model

enum class Season(val label: String, val icon: String) {
    SPRING("春", "🌸"),
    SUMMER("夏", "☀️"),
    AUTUMN("秋", "🍂"),
    WINTER("冬", "❄️");

    companion object {
        fun fromMonth(month: Int): Season = when (month) {
            3, 4 -> SPRING
            5, 6, 7 -> SUMMER
            8, 9 -> AUTUMN
            else -> WINTER
        }
    }
}
