package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import com.mrlaughing.moyuan.engine.unlock.UnlockEngine
import java.time.LocalDate

/**
 * 墨园总调度引擎（2.0 收敛版）
 *
 * 每日重算流程：
 * Phase 1 - 环境：确定季节
 * Phase 2 - 元数据：更新花园全局元数据（累计分钟、连续天数等）
 * Phase 3 - 解锁：随机解锁新植物（每阈值一株 + 初始3株）
 * Phase 4 - 返回：组装 GardenUpdateResult
 */
object GardenEngine {

    /**
     * 每日重算入口
     */
    fun recalculate(
        meta: EngineMeta,
        plantStates: List<EnginePlantState>,
        dailyInput: DailyReadInput,
        today: LocalDate
    ): GardenUpdateResult {

        // ── Phase 1: 环境 ──────────────────────────────────────
        val season = SeasonEngine.getSeason(today)

        // ── Phase 2: 元数据更新 ─────────────────────────────────
        val updatedMeta = updateMeta(meta, dailyInput, today)

        // ── Phase 3: 解锁（随机/每阈值一株）─────────────────────
        val newlyUnlocked = UnlockEngine.checkAndUnlock(updatedMeta, plantStates)
        val finalPlants = addNewlyUnlockedPlants(
            currentPlants = plantStates,
            newlyUnlocked = newlyUnlocked,
            today = today
        )

        // ── Phase 4: 返回 ──────────────────────────────────────
        return GardenUpdateResult(
            meta = updatedMeta,
            plants = finalPlants,
            season = season,
            newlyUnlocked = newlyUnlocked
        )
    }

    // ─── 内部方法 ──────────────────────────────────────────────

    /**
     * Phase 2: 更新花园元数据（累计分钟、连续天数、总阅读天数）
     */
    private fun updateMeta(
        meta: EngineMeta,
        dailyInput: DailyReadInput,
        today: LocalDate
    ): EngineMeta {
        val hasReadToday = dailyInput.minutesRead > 0
        // 去重：今天已经计入过连续天数（同一天多次同步/手动+定时），不再重复累加
        val alreadyCountedToday = meta.lastReadDate == today
        // 连续判定：今天读了 + 今天尚未计入 + 昨天读过（lastReadDate==昨天）
        val isConsecutive = hasReadToday && !alreadyCountedToday && meta.lastReadDate == today.minusDays(1)

        val newStreakDays = when {
            isConsecutive -> meta.streakDays + 1
            hasReadToday && !alreadyCountedToday -> 1
            !hasReadToday && meta.lastReadDate.isBefore(today.minusDays(1)) -> 0
            else -> meta.streakDays
        }

        val newMaxStreakDays = maxOf(meta.maxStreakDays, newStreakDays)
        val newAccumulatedMinutes = meta.accumulatedMinutes + dailyInput.minutesRead
        val newBooksRead = meta.booksRead + dailyInput.booksReadToday
        val newLastReadDate = if (hasReadToday) today else meta.lastReadDate

        // 总阅读天数不再在引擎内增量计算：权威值来自微信读书 API（overallData.readDays），
        // 由 SyncWorker step6 设置并经 toDbMeta 持久化；引擎透传，避免与 API 值偏离/重复累加
        val newTotalReadDays = meta.totalReadDays

        return meta.copy(
            accumulatedMinutes = newAccumulatedMinutes,
            streakDays = newStreakDays,
            maxStreakDays = newMaxStreakDays,
            booksRead = newBooksRead,
            lastReadDate = newLastReadDate,
            totalReadDays = newTotalReadDays
        )
    }

    /**
     * Phase 3 辅助：将新解锁的植物加入植物状态列表
     */
    private fun addNewlyUnlockedPlants(
        currentPlants: List<EnginePlantState>,
        newlyUnlocked: List<Plant>,
        today: LocalDate
    ): List<EnginePlantState> {
        val updatedExisting = currentPlants.map { plant ->
            if (plant.plantId in newlyUnlocked.map { it.id } && !plant.isUnlocked) {
                plant.copy(isUnlocked = true, unlockDate = today)
            } else {
                plant
            }
        }

        val existingIds = currentPlants.map { it.plantId }.toSet()
        val newPlantStates = newlyUnlocked
            .filter { it.id !in existingIds }
            .map { plant ->
                EnginePlantState(
                    plantId = plant.id,
                    isUnlocked = true,
                    unlockDate = today
                )
            }

        return updatedExisting + newPlantStates
    }

    // ─── 便捷方法 ──────────────────────────────────────────────

    /**
     * 初始化花园：创建所有植物的初始状态（全部锁定）
     */
    fun initializeGarden(userId: String, today: LocalDate): Pair<EngineMeta, List<EnginePlantState>> {
        val meta = EngineMeta(
            userId = userId,
            accumulatedMinutes = 0,
            streakDays = 0,
            maxStreakDays = 0,
            nightReadDays = 0,
            booksRead = 0,
            lastReadDate = today,
            totalReadDays = 0
        )

        val plants = PlantDefinitions.all.map { def ->
            EnginePlantState(
                plantId = def.id,
                isUnlocked = false,
                unlockDate = null
            )
        }

        return Pair(meta, plants)
    }

    /**
     * 获取花圃概览统计（简化）
     */
    fun getGardenStats(plants: List<EnginePlantState>): GardenStats {
        val unlocked = plants.filter { it.isUnlocked }
        return GardenStats(
            totalPlants = PlantDefinitions.all.size,
            unlockedCount = unlocked.size
        )
    }
}

/**
 * 花圃统计概览
 */
data class GardenStats(
    val totalPlants: Int,
    val unlockedCount: Int,
    val aliveCount: Int = unlockedCount,
    val witheringCount: Int = 0,
    val deadCount: Int = 0
)
