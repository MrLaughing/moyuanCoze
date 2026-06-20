package com.mrlaughing.moyuan.engine.season

import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.engine.WitherCountdown
import com.mrlaughing.moyuan.engine.wither.WitherEngine
import java.time.LocalDate
import kotlin.random.Random

/**
 * 季节天气引擎
 *
 * 季节：春(3,4月) 夏(5,6,7月) 秋(8,9月) 冬(10,11,12,1,2月)
 * 天气：按概率抽取，部分天气有季节/昼夜限制
 */
object SeasonEngine {

    /**
     * 根据日期获取当前季节
     *
     * @param date 日期
     * @return Season
     */
    fun getSeason(date: LocalDate): Season {
        return Season.fromMonth(date.monthValue)
    }

    /**
     * 判断是否为夜间（22:00 ~ 06:00）
     *
     * @param hour 当前小时（0-23）
     * @return 是否为夜间
     */
    fun isNightHour(hour: Int): Boolean {
        return hour >= 22 || hour < 6
    }

    /**
     * 概率抽取天气
     *
     * 仅从当前季节和昼夜条件下可用的天气中抽取。
     * 先计算可用天气的概率总和，再按比例归一化后抽取。
     *
     * @param season 当前季节
     * @param isNight 是否为夜间
     * @param random 随机数生成器（便于测试注入）
     * @return Weather 抽取结果
     */
    fun rollWeather(
        season: Season,
        isNight: Boolean,
        random: Random = Random.Default
    ): Weather {
        val availableWeathers = Weather.entries.filter { it.isAvailableIn(season, isNight) }

        if (availableWeathers.isEmpty()) {
            return Weather.CLEAR
        }

        // 计算可用天气的概率总和
        val totalProbability = availableWeathers.sumOf { it.probability.toDouble() }

        // 归一化后按概率抽取
        var roll = random.nextDouble() * totalProbability
        for (weather in availableWeathers) {
            roll -= weather.probability.toDouble()
            if (roll <= 0) {
                return weather
            }
        }

        // 浮点精度兜底
        return availableWeathers.last()
    }

    /**
     * 计算环境总倍率
     *
     * @param season 季节
     * @param weather 天气
     * @return 总倍率
     */
    fun calculateMultiplier(season: Season, weather: Weather): Float {
        return season.multiplier * weather.multiplier
    }

    /**
     * 获取植物的枯萎倒计时
     *
     * @param plant 植物定义
     * @param lastReadDate 最后阅读日期
     * @param today 当前日期
     * @return WitherCountdown
     */
    fun getWitherCountdown(
        plant: Plant,
        lastReadDate: LocalDate,
        today: LocalDate
    ): WitherCountdown {
        return WitherEngine.calculateWitherCountdown(plant, lastReadDate, today)
    }

    /**
     * 将 WMO weather_code 映射为墨园天气
     *
     * WMO 代码参考：https://open-meteo.com/en/docs#weathervariables
     * 0:  晴空 → CLEAR
     * 1:  主晴 → CLEAR
     * 2:  多云 → OVERCAST
     * 3:  阴天 → OVERCAST
     * 45,48: 雾 → FOGGY
     * 51,53,55: 毛毛雨 → SPRING_RAIN
     * 56,57: 冻毛毛雨 → FOGGY（少见，归为雾）
     * 61,63,65: 雨 → SPRING_RAIN
     * 66,67: 冻雨 → FOGGY
     * 71,73,75: 雪 → FIRST_SNOW
     * 77:  雪粒 → FIRST_SNOW
     * 80,81,82: 阵雨 → SPRING_RAIN
     * 85,86: 阵雪 → FIRST_SNOW
     * 95:  雷暴 → SPRING_RAIN
     * 96,99: 雷暴+冰雹 → SPRING_RAIN
     *
     * 特殊规则：
     * - 夜间 + 晴空 → MOONLIT（仅夏季）
     * - 春雨和初雪受季节限制
     * - 非对应季节的雨/雪降级为 OVERCAST
     */
    fun mapWmoCodeToWeather(wmoCode: Int, season: Season, isNight: Boolean): Weather {
        return when (wmoCode) {
            0 -> {
                // 晴空：夏季夜间 → 月夜，否则 → 晴
                if (isNight && season == Season.SUMMER) Weather.MOONLIT
                else Weather.CLEAR
            }
            1 -> {
                // 主晴：夏季夜间 → 月夜
                if (isNight && season == Season.SUMMER) Weather.MOONLIT
                else Weather.CLEAR
            }
            2 -> Weather.OVERCAST  // 多云
            3 -> Weather.OVERCAST  // 阴天
            45, 48 -> Weather.FOGGY  // 雾
            56, 57 -> Weather.FOGGY  // 冻毛毛雨 → 归为雾
            66, 67 -> Weather.FOGGY  // 冻雨 → 归为雾
            51, 53, 55,  // 毛毛雨
            61, 63, 65,  // 雨
            80, 81, 82,  // 阵雨
            95, 96, 99   // 雷暴
            -> {
                // 雨类：春季 → 春雨，其他季节 → 阴（墨园春雨仅限春季）
                if (season == Season.SPRING) Weather.SPRING_RAIN
                else Weather.OVERCAST
            }
            71, 73, 75,  // 雪
            77,          // 雪粒
            85, 86       // 阵雪
            -> {
                // 雪类：冬季 → 初雪，其他季节 → 阴（墨园初雪仅限冬季）
                if (season == Season.WINTER) Weather.FIRST_SNOW
                else Weather.OVERCAST
            }
            else -> Weather.CLEAR  // 未知代码默认晴
        }
    }
}