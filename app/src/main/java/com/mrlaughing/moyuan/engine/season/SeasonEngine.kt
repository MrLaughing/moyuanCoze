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
}
