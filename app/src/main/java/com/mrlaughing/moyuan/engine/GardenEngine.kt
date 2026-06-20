package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.PlantPath
import com.mrlaughing.moyuan.data.model.ReadStats
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.WitherStage
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.engine.growth.GrowthEngine
import com.mrlaughing.moyuan.engine.irrigation.IrrigationEngine
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import com.mrlaughing.moyuan.engine.unlock.UnlockEngine
import com.mrlaughing.moyuan.engine.wither.WitherEngine
import java.time.LocalDate

/**
 * 墨园总调度引擎
 *
 * 每日重算流程（7个阶段）：
 * Phase 1 - 环境：确定季节与天气
 * Phase 2 - 路径：判定用户激活的路径
 * Phase 3 - 元数据：更新花园全局元数据（累计分钟、连续天数等）
 * Phase 4 - 植物：逐株更新——灌溉、枯萎/恢复、等级判定
 * Phase 5 - 解锁：检查新植物解锁条件
 * Phase 6 - 清理：重置每日标记（justRevived等）
 * Phase 7 - 返回：组装 GardenUpdateResult
 */
object GardenEngine {

    /**
     * 每日重算入口
     *
     * @param meta 当前花园元数据（引擎层）
     * @param plantStates 当前所有植物状态（引擎层）
     * @param dailyInput 当日阅读输入
     * @param today 当前日期
     * @return GardenUpdateResult
     */
    fun recalculate(
        meta: EngineMeta,
        plantStates: List<EnginePlantState>,
        dailyInput: DailyReadInput,
        today: LocalDate
    ): GardenUpdateResult {

        // ── Phase 1: 环境 ──────────────────────────────────────
        val season = SeasonEngine.getSeason(today)
        val isNight = SeasonEngine.isNightHour(java.time.LocalTime.now().hour)
        val weather = SeasonEngine.rollWeather(season, isNight)
        val multiplier = SeasonEngine.calculateMultiplier(season, weather)

        // ── Phase 2: 路径 ──────────────────────────────────────
        val readStats = ReadStats(
            todayMinutes = dailyInput.minutesRead,
            streakDays = meta.streakDays,
            nightReadDays = meta.nightReadDays,
            accumulatedMinutes = meta.accumulatedMinutes,
            booksRead = meta.booksRead,
            hasNightReadToday = dailyInput.isNightRead
        )
        val userPaths = IrrigationEngine.determineUserPaths(readStats)

        // ── Phase 3: 元数据更新 ─────────────────────────────────
        val updatedMeta = updateMeta(meta, dailyInput, today, userPaths)

        // ── Phase 4: 植物更新 ───────────────────────────────────
        val updatedPlants = updatePlants(
            plantStates = plantStates,
            dailyInput = dailyInput,
            season = season,
            weather = weather,
            userPaths = userPaths,
            today = today
        )

        // ── Phase 5: 解锁 ──────────────────────────────────────
        val newlyUnlocked = UnlockEngine.checkAndUnlock(updatedMeta, updatedPlants)
        val plantsWithNewlyUnlocked = addNewlyUnlockedPlants(
            currentPlants = updatedPlants,
            newlyUnlocked = newlyUnlocked,
            today = today
        )

        // ── Phase 6: 清理 ──────────────────────────────────────
        // justRevived 在 Phase 4 中被设置，在 Phase 5 中被使用
        // 解锁判定之后再清理 justRevived
        val finalPlants = plantsWithNewlyUnlocked.map { plant ->
            plant.copy(justRevived = false)
        }

        // 但彼岸花解锁需要检测 justRevived，所以在解锁判定之后再清理
        // 重新检查彼岸花（如果之前没解锁但在本次 justRevived 中满足条件）
        val finalNewlyUnlocked = if (newlyUnlocked.any { it.id == "bianhua" }) {
            newlyUnlocked
        } else {
            // 用未清理 justRevived 的状态再检查一次
            val extraUnlocks = UnlockEngine.checkAndUnlock(updatedMeta, updatedPlants)
            (newlyUnlocked + extraUnlocks).distinctBy { it.id }
        }

        // 重新添加可能遗漏的解锁植物
        val trulyFinalPlants = addNewlyUnlockedPlants(
            currentPlants = finalPlants,
            newlyUnlocked = finalNewlyUnlocked.filter { newlyUnlockedPlant ->
                finalPlants.none { it.plantId == newlyUnlockedPlant.id }
            },
            today = today
        )

        // ── Phase 7: 返回 ──────────────────────────────────────
        return GardenUpdateResult(
            meta = updatedMeta,
            plants = trulyFinalPlants,
            season = season,
            weather = weather,
            multiplier = multiplier,
            newlyUnlocked = finalNewlyUnlocked
        )
    }

    // ─── 内部方法 ──────────────────────────────────────────────

    /**
     * Phase 3: 更新花园元数据
     */
    private fun updateMeta(
        meta: EngineMeta,
        dailyInput: DailyReadInput,
        today: LocalDate,
        userPaths: Set<PlantPath>
    ): EngineMeta {
        val hasReadToday = dailyInput.minutesRead > 0
        val isConsecutive = hasReadToday && (
            meta.lastReadDate == today || meta.lastReadDate == today.minusDays(1)
        )

        val newStreakDays = if (isConsecutive) {
            meta.streakDays + 1
        } else if (hasReadToday) {
            1 // 断连后重新开始
        } else {
            0 // 今天没读，连续天数归零
        }

        val newMaxStreakDays = maxOf(meta.maxStreakDays, newStreakDays)

        val newNightReadDays = if (dailyInput.isNightRead) {
            meta.nightReadDays + 1
        } else {
            meta.nightReadDays
        }

        val newAccumulatedMinutes = meta.accumulatedMinutes + dailyInput.minutesRead

        val newBooksRead = meta.booksRead + dailyInput.booksReadToday

        val newLastReadDate = if (hasReadToday) today else meta.lastReadDate

        val newTotalReadDays = if (hasReadToday && meta.lastReadDate != today) {
            meta.totalReadDays + 1
        } else {
            meta.totalReadDays
        }

        return meta.copy(
            accumulatedMinutes = newAccumulatedMinutes,
            streakDays = newStreakDays,
            maxStreakDays = newMaxStreakDays,
            nightReadDays = newNightReadDays,
            booksRead = newBooksRead,
            lastReadDate = newLastReadDate,
            totalReadDays = newTotalReadDays,
            activePaths = userPaths
        )
    }

    /**
     * Phase 4: 逐株更新植物状态
     */
    private fun updatePlants(
        plantStates: List<EnginePlantState>,
        dailyInput: DailyReadInput,
        season: Season,
        weather: Weather,
        userPaths: Set<PlantPath>,
        today: LocalDate
    ): List<EnginePlantState> {
        return plantStates.map { plant ->
            if (!plant.isUnlocked) return@map plant

            val plantDef = PlantDefinitions.getById(plant.plantId) ?: return@map plant

            // Step 1: 枯萎判定
            val witherResult = WitherEngine.calculateWither(
                lastReadDate = plant.lastWateredDate,
                currentDate = today,
                currentWitherStage = plant.witherStage
            )

            // Step 2: 判断是否今日有阅读（恢复条件）
            val hasReadToday = dailyInput.minutesRead > 0
            val wasWithering = plant.witherStage != WitherStage.NONE
            val nowRecovering = hasReadToday && wasWithering

            val (finalWitherStage, finalMinutes, justRevived) = if (nowRecovering) {
                // 恢复：计算保留分钟数，枯萎阶段归零
                val recovery = WitherEngine.calculateRecovery(
                    witherStage = plant.witherStage,
                    accumulatedMinutes = plant.accumulatedMinutes,
                    level = plant.growthLevel
                )
                Triple(WitherStage.NONE, recovery.recoveredMinutes, recovery.justRevived)
            } else if (witherResult.stage != plant.witherStage && !hasReadToday) {
                // 枯萎加深
                if (witherResult.stage == WitherStage.DEAD) {
                    // 进入枯寂，保留30%
                    val deadMinutes = (plant.accumulatedMinutes * 0.3f).toInt().coerceAtLeast(0)
                    Triple(WitherStage.DEAD, deadMinutes, false)
                } else {
                    // 枯萎阶段变化但未到枯寂，分钟数不变（只改变视觉表现）
                    Triple(witherResult.stage, plant.accumulatedMinutes, false)
                }
            } else {
                // 无变化
                Triple(plant.witherStage, plant.accumulatedMinutes, false)
            }

            // Step 4: 灌溉计算（枯寂植物 effectiveMinutes=0）
            val effectiveMinutes = IrrigationEngine.calculateEffectiveMinutes(
                dailyReadMinutes = dailyInput.minutesRead,
                season = season,
                weather = weather,
                plantPath = plantDef.path,
                userPaths = userPaths,
                witherStage = finalWitherStage
            )

            // Step 5: 累加有效分钟数
            val newAccumulatedMinutes = finalMinutes + effectiveMinutes

            // Step 6: 等级判定
            val newLevel = GrowthLevel.fromMinutes(newAccumulatedMinutes)

            // Step 7: 更新最后浇水日期
            val newLastWateredDate = if (hasReadToday) today else plant.lastWateredDate

            plant.copy(
                accumulatedMinutes = newAccumulatedMinutes,
                growthLevel = newLevel,
                witherStage = finalWitherStage,
                lastWateredDate = newLastWateredDate,
                justRevived = justRevived
            )
        }
    }

    /**
     * Phase 5 辅助：将新解锁的植物加入植物状态列表
     */
    private fun addNewlyUnlockedPlants(
        currentPlants: List<EnginePlantState>,
        newlyUnlocked: List<Plant>,
        today: LocalDate
    ): List<EnginePlantState> {
        val newPlantStates = newlyUnlocked.map { plant ->
            EnginePlantState(
                plantId = plant.id,
                path = plant.path.name,
                isUnlocked = true,
                accumulatedMinutes = 0,
                growthLevel = GrowthLevel.LV1,
                witherStage = WitherStage.NONE,
                lastWateredDate = today,
                justRevived = false,
                unlockDate = today
            )
        }

        // 如果当前列表中已有该植物的锁定状态，则替换为解锁状态
        val existingIds = currentPlants.map { it.plantId }.toSet()
        val updatedExisting = currentPlants.map { plant ->
            if (plant.plantId in newlyUnlocked.map { it.id } && !plant.isUnlocked) {
                plant.copy(
                    isUnlocked = true,
                    unlockDate = today
                )
            } else {
                plant
            }
        }

        // 添加全新的植物（之前不在列表中的）
        val brandNewPlants = newPlantStates.filter { it.plantId !in existingIds }

        return updatedExisting + brandNewPlants
    }

    // ─── 便捷方法 ──────────────────────────────────────────────

    /**
     * 初始化花园：创建所有27种植物的初始状态（全部锁定）
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
            totalReadDays = 0,
            activePaths = emptySet()
        )

        val plants = PlantDefinitions.all.map { def ->
            EnginePlantState(
                plantId = def.id,
                path = def.path.name,
                isUnlocked = false,
                accumulatedMinutes = 0,
                growthLevel = GrowthLevel.LV1,
                witherStage = WitherStage.NONE,
                lastWateredDate = today,
                justRevived = false,
                unlockDate = null
            )
        }

        return Pair(meta, plants)
    }

    /**
     * 获取花圃概览统计
     */
    fun getGardenStats(plants: List<EnginePlantState>): GardenStats {
        val unlocked = plants.filter { it.isUnlocked }
        val alive = unlocked.filter { it.witherStage != WitherStage.DEAD }
        val withering = unlocked.filter { it.witherStage != WitherStage.NONE && it.witherStage != WitherStage.DEAD }
        val maxLevel = unlocked.filter { it.growthLevel == GrowthLevel.LV5 && it.witherStage == WitherStage.NONE }

        return GardenStats(
            totalPlants = PlantDefinitions.all.size,
            unlockedCount = unlocked.size,
            aliveCount = alive.size,
            witheringCount = withering.size,
            deadCount = unlocked.count { it.witherStage == WitherStage.DEAD },
            maxLevelCount = maxLevel.size
        )
    }
}

/**
 * 花圃统计概览
 */
data class GardenStats(
    val totalPlants: Int,
    val unlockedCount: Int,
    val aliveCount: Int,
    val witheringCount: Int,
    val deadCount: Int,
    val maxLevelCount: Int
)
