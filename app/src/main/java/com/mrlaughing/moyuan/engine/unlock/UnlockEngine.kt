package com.mrlaughing.moyuan.engine.unlock

import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.engine.EngineMeta
import com.mrlaughing.moyuan.engine.EnginePlantState
import java.time.LocalDate

/**
 * 解锁引擎（2.0 随机收敛版）
 *
 * 规则：
 * - 首次进入花园（无已解锁植物）随机解锁 3 株作为初始植物
 * - 之后每跨过一个阅读分钟阈值（解锁里程碑），随机解锁 1 株未解锁植物
 * - 解锁的具体植物随机选取，不再是"达到阈值即解锁对应植物"
 */
object UnlockEngine {

    /** 初始随机解锁数量 */
    private const val INITIAL_UNLOCK_COUNT = 3

    /**
     * 检查并解锁新植物
     *
     * @param meta 花园元数据（含累计阅读分钟数）
     * @param plants 当前所有植物状态
     * @return 新解锁的植物定义列表（随机选取）
     */
    fun checkAndUnlock(
        meta: EngineMeta,
        plants: List<EnginePlantState>
    ): List<Plant> {
        val locked = plants.filter { !it.isUnlocked }
        if (locked.isEmpty()) return emptyList()

        val unlockedCount = plants.size - locked.size

        // 目标解锁数 = 初始3株 + 已跨过的阈值里程碑数
        val targetUnlocked = INITIAL_UNLOCK_COUNT + crossedThresholdCount(meta.accumulatedMinutes)
        val need = (targetUnlocked - unlockedCount).coerceAtLeast(0)
        if (need <= 0) return emptyList()

        // 从已锁定植物中随机选取 need 株
        val lockedDefs = locked.mapNotNull { PlantDefinitions.getById(it.plantId) }
        if (lockedDefs.isEmpty()) return emptyList()

        return lockedDefs.shuffled().take(need.coerceAtMost(lockedDefs.size))
    }

    /**
     * 计算已跨过的阈值里程碑数量（排除初始阈值 0）
     */
    private fun crossedThresholdCount(accumulatedMinutes: Int): Int {
        return distinctThresholds().count { accumulatedMinutes >= it }
    }

    /**
     * 去重并升序的阈值列表（排除初始阈值 0）
     */
    private fun distinctThresholds(): List<Int> {
        return PlantDefinitions.all
            .map { it.unlockThreshold }
            .distinct()
            .filter { it > 0 }
            .sorted()
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
     * 获取下一解锁里程碑阈值（下一个尚未跨过的阈值）
     */
    fun getNextUnlockThreshold(meta: EngineMeta): Int? {
        return distinctThresholds().firstOrNull { meta.accumulatedMinutes < it }
    }
}
