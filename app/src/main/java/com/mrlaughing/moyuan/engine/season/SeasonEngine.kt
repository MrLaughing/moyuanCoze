package com.mrlaughing.moyuan.engine.season

import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import java.time.LocalDate

/**
 * 季节天气引擎（简化版）
 *
 * 季节：春(3,4月) 夏(5,6,7月) 秋(8,9月) 冬(10,11,12,1,2月)
 * 天气：直接通过 WMO code 从实时 API 映射
 */
object SeasonEngine {

    /**
     * 根据日期获取当前季节
     */
    fun getSeason(date: LocalDate): Season {
        return Season.fromMonth(date.monthValue)
    }

    /**
     * 判断是否为夜间（22:00 ~ 06:00）
     */
    fun isNightHour(hour: Int): Boolean {
        return hour >= 22 || hour < 6
    }

    /**
     * 将 WMO weather_code 映射为墨园天气（新枚举）
     *
     * WMO 代码参考：https://open-meteo.com/en/docs#weathervariables
     */
    fun mapWmoCodeToWeather(wmoCode: Int): Weather {
        return when (wmoCode) {
            0 -> Weather.CLEAR
            1, 2 -> Weather.CLOUDY
            3 -> Weather.OVERCAST
            45, 48 -> Weather.FOGGY
            51, 53, 55 -> Weather.DRIZZLE
            56, 57 -> Weather.FOGGY
            61, 63, 65 -> Weather.RAIN
            66, 67 -> Weather.FOGGY
            71, 73, 75 -> Weather.SNOW
            77 -> Weather.SNOW
            80, 81, 82 -> Weather.RAIN
            85, 86 -> Weather.SNOW
            95, 96, 99 -> Weather.THUNDERSTORM
            else -> Weather.CLEAR
        }
    }
}