package com.mrlaughing.moyuan.data.model

/**
 * 墨园天气类型
 *
 * 直接从 WMO 天气码映射，不再包含 multiplier/probability 等游戏数值。
 * isAvailableIn 始终返回 true（使用实时 API，无需随机限制）。
 */
enum class Weather(val label: String, val icon: String) {
    CLEAR("晴", "☀️"),
    CLOUDY("多云", "⛅"),
    OVERCAST("阴", "☁️"),
    DRIZZLE("毛毛雨", "🌦️"),
    RAIN("雨", "🌧️"),
    THUNDERSTORM("雷暴", "⛈️"),
    SNOW("雪", "❄️"),
    FOGGY("雾", "🌫️"),
    WINDY("大风", "🌬️");

    fun isAvailableIn(season: Season, isNight: Boolean): Boolean = true
}
