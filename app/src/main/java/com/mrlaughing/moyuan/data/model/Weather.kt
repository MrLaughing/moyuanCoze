package com.mrlaughing.moyuan.data.model

enum class Weather(val label: String, val multiplier: Float, val probability: Float) {
    CLEAR("晴", 1.0f, 0.60f),
    OVERCAST("阴", 1.0f, 0.15f),
    FOGGY("雾", 1.0f, 0.10f),
    SPRING_RAIN("春雨", 1.3f, 0.08f),
    MOONLIT("月夜", 1.2f, 0.05f),
    FIRST_SNOW("初雪", 1.8f, 0.02f);

    fun isAvailableIn(season: Season, isNight: Boolean): Boolean = when (this) {
        SPRING_RAIN -> season == Season.SPRING
        MOONLIT -> season == Season.SUMMER && isNight
        FIRST_SNOW -> season == Season.WINTER
        else -> true
    }
}
