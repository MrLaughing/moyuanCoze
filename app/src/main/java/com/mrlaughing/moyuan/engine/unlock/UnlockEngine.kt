package com.mrlaughing.moyuan.engine.unlock

import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.PlantPath
import com.mrlaughing.moyuan.data.model.WitherStage
import com.mrlaughing.moyuan.engine.EngineMeta
import com.mrlaughing.moyuan.engine.EnginePlantState
import java.time.LocalDate
import java.time.LocalTime

/**
 * 解锁引擎
 *
 * 解锁条件：
 * - 积墨：累计阅读分钟数达到阈值
 * - 秉烛：夜间阅读天数达到阈值
 * - 岁寒：历史最高连续阅读天数达到阈值
 * - 寻芳：已读不同书目数达到阈值
 * - 隐藏：
 *   - 忘忧草：四径各解锁3种植物
 *   - 彼岸花：枯寂植物复活
 *   - 连理枝：5株鲜活Lv.5植物
 */
object UnlockEngine {

    /**
     * 检查并解锁新植物
     *
     * @param meta 花园元数据（引擎层）
     * @param plants 当前所有植物状态（引擎层）
     * @return 新解锁的植物定义列表
     */
    fun checkAndUnlock(
        meta: EngineMeta,
        plants: List<EnginePlantState>
    ): List<Plant> {
        val unlockedIds = plants.filter { it.isUnlocked }.map { it.plantId }.toSet()
        val newlyUnlocked = mutableListOf<Plant>()

        for (def in PlantDefinitions.all) {
            if (def.id in unlockedIds) continue

            val shouldUnlock = when (def.path) {
                PlantPath.JIMO -> meta.accumulatedMinutes >= def.unlockThreshold
                PlantPath.BINGZHU -> meta.nightReadDays >= def.unlockThreshold
                PlantPath.SUIHAN -> meta.maxStreakDays >= def.unlockThreshold
                PlantPath.XUNFANG -> meta.booksRead >= def.unlockThreshold
                PlantPath.HIDDEN -> checkHiddenCondition(def, meta, plants)
            }

            if (shouldUnlock) {
                newlyUnlocked.add(def)
            }
        }

        return newlyUnlocked
    }

    /**
     * 检查隐藏植物解锁条件
     */
    fun checkHiddenCondition(
        def: Plant,
        meta: EngineMeta,
        plants: List<EnginePlantState>
    ): Boolean {
        return when (def.id) {
            "wangyoucao" -> checkWangyoucaoCondition(plants)
            "bianhua" -> checkBianhuaCondition(plants)
            "lianlizhi" -> checkLianlizhiCondition(plants)
            else -> false
        }
    }

    /**
     * 忘忧草条件：四径各解锁3种植物
     */
    private fun checkWangyoucaoCondition(plants: List<EnginePlantState>): Boolean {
        val unlockedIds = plants.filter { it.isUnlocked }.map { it.plantId }.toSet()

        val paths = listOf(PlantPath.JIMO, PlantPath.BINGZHU, PlantPath.SUIHAN, PlantPath.XUNFANG)
        return paths.all { path ->
            val pathPlantIds = PlantDefinitions.getByPath(path).map { it.id }
            pathPlantIds.count { it in unlockedIds } >= 3
        }
    }

    /**
     * 彼岸花条件：有枯寂植物复活（justRevived=true）
     */
    private fun checkBianhuaCondition(plants: List<EnginePlantState>): Boolean {
        return plants.any { it.justRevived }
    }

    /**
     * 连理枝条件：5株鲜活（非枯萎）Lv.5植物
     */
    private fun checkLianlizhiCondition(plants: List<EnginePlantState>): Boolean {
        return plants.count {
            it.isUnlocked &&
            it.growthLevel == GrowthLevel.LV5 &&
            it.witherStage == WitherStage.NONE
        } >= 5
    }

    /**
     * 计算连续阅读天数
     */
    fun calculateStreakDays(today: LocalDate, readDates: Set<LocalDate>): Int {
        var streak = 0
        var checkDate = today

        while (checkDate in readDates) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    /**
     * 判断某次阅读是否为夜间阅读（22:00 ~ 06:00）
     */
    fun isNightRead(time: LocalTime): Boolean {
        val hour = time.hour
        return hour >= 22 || hour < 6
    }

    /**
     * 获取某条路径的解锁进度
     */
    fun getPathProgress(path: PlantPath, meta: EngineMeta): Pair<Int, Int?> {
        val currentValue = when (path) {
            PlantPath.JIMO -> meta.accumulatedMinutes
            PlantPath.BINGZHU -> meta.nightReadDays
            PlantPath.SUIHAN -> meta.maxStreakDays
            PlantPath.XUNFANG -> meta.booksRead
            PlantPath.HIDDEN -> return Pair(0, null)
        }

        val pathPlants = PlantDefinitions.getByPath(path)
            .sortedBy { it.unlockThreshold }

        val nextThreshold = pathPlants
            .firstOrNull { it.unlockThreshold > currentValue }
            ?.unlockThreshold

        return Pair(currentValue, nextThreshold)
    }

    /**
     * 获取某条路径已解锁的植物数量
     */
    fun getUnlockedCountForPath(path: PlantPath, plants: List<EnginePlantState>): Int {
        val pathPlantIds = PlantDefinitions.getByPath(path).map { it.id }.toSet()
        return plants.count { it.plantId in pathPlantIds && it.isUnlocked }
    }
}
