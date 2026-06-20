package com.mrlaughing.moyuan.engine.irrigation

import com.mrlaughing.moyuan.data.model.PlantPath
import com.mrlaughing.moyuan.data.model.ReadStats
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.WitherStage
import com.mrlaughing.moyuan.data.model.Weather
import kotlin.math.roundToInt

/**
 * 灌溉计算引擎
 *
 * 核心公式：effectiveMinutes = dailyReadMinutes × seasonMultiplier × weatherMultiplier × pathMatchMultiplier
 * 枯萎中植物 effectiveMinutes = 0
 */
object IrrigationEngine {

    private const val PATH_MATCH_MULTIPLIER = 1.2f

    /**
     * 计算有效灌溉分钟数
     *
     * @param dailyReadMinutes 当日阅读分钟数
     * @param season 当前季节
     * @param weather 当日天气
     * @param plantPath 植物所属路径
     * @param userPaths 用户已激活的路径集合
     * @param witherStage 植物当前枯萎阶段（枯寂则不灌溉）
     * @return 有效灌溉分钟数
     */
    fun calculateEffectiveMinutes(
        dailyReadMinutes: Int,
        season: Season,
        weather: Weather,
        plantPath: PlantPath,
        userPaths: Set<PlantPath>,
        witherStage: WitherStage
    ): Int {
        // 枯寂植物不再获得灌溉
        if (witherStage == WitherStage.DEAD) {
            return 0
        }

        // 当日无阅读则无灌溉
        if (dailyReadMinutes <= 0) {
            return 0
        }

        val seasonMul = season.multiplier
        val weatherMul = weather.multiplier
        val pathMul = if (plantPath in userPaths) PATH_MATCH_MULTIPLIER else 1.0f

        val effective = dailyReadMinutes * seasonMul * weatherMul * pathMul
        return effective.roundToInt().coerceAtLeast(0)
    }

    /**
     * 判定用户已激活的路径
     *
     * 激活条件：
     * - 积墨：累计分钟 > 0
     * - 秉烛：夜读天数 >= 1
     * - 岁寒：连续天数 >= 1
     * - 寻芳：书目数 >= 1
     * - 隐藏：不可通过此方式激活
     */
    fun determineUserPaths(readStats: ReadStats): Set<PlantPath> {
        val paths = mutableSetOf<PlantPath>()

        if (readStats.accumulatedMinutes > 0) {
            paths.add(PlantPath.JIMO)
        }
        if (readStats.nightReadDays >= 1) {
            paths.add(PlantPath.BINGZHU)
        }
        if (readStats.streakDays >= 1) {
            paths.add(PlantPath.SUIHAN)
        }
        if (readStats.booksRead >= 1) {
            paths.add(PlantPath.XUNFANG)
        }
        // HIDDEN 路径不通过此方式激活

        return paths
    }

    /**
     * 仅计算环境倍率（不含路径匹配），用于总览显示
     */
    fun calculateEnvironmentMultiplier(season: Season, weather: Weather): Float {
        return season.multiplier * weather.multiplier
    }

    /**
     * 计算完整倍率（含路径匹配），用于单株植物展示
     */
    fun calculateFullMultiplier(
        season: Season,
        weather: Weather,
        plantPath: PlantPath,
        userPaths: Set<PlantPath>
    ): Float {
        val pathMul = if (plantPath in userPaths) PATH_MATCH_MULTIPLIER else 1.0f
        return season.multiplier * weather.multiplier * pathMul
    }
}
