package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.engine.unlock.UnlockEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GardenEngineTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 19)

    @Test
    fun sameDaySyncDoesNotIncrementStreakTwice() {
        val result = recalculate(
            meta = meta(streakDays = 4, lastReadDate = today),
            date = today,
            minutes = 20
        )

        assertEquals(4, result.meta.streakDays)
        assertEquals(today, result.meta.lastReadDate)
    }

    @Test
    fun readingOnFollowingDayExtendsStreak() {
        val result = recalculate(
            meta = meta(streakDays = 4, lastReadDate = today.minusDays(1)),
            date = today,
            minutes = 20
        )

        assertEquals(5, result.meta.streakDays)
        assertEquals(5, result.meta.maxStreakDays)
    }

    @Test
    fun readingAfterMissedDayStartsNewStreak() {
        val result = recalculate(
            meta = meta(streakDays = 8, lastReadDate = today.minusDays(2)),
            date = today,
            minutes = 20
        )

        assertEquals(1, result.meta.streakDays)
        assertEquals(8, result.meta.maxStreakDays)
    }

    @Test
    fun noReadingAfterMissedDayClearsCurrentStreak() {
        val result = recalculate(
            meta = meta(streakDays = 8, lastReadDate = today.minusDays(2)),
            date = today,
            minutes = 0
        )

        assertEquals(0, result.meta.streakDays)
        assertEquals(8, result.meta.maxStreakDays)
    }

    @Test
    fun newGardenStartsWithThreeRandomPlants() {
        val unlocked = UnlockEngine.checkAndUnlock(
            meta = meta(accumulatedMinutes = 0),
            plants = allLockedPlants()
        )

        assertEquals(3, unlocked.size)
        assertEquals(3, unlocked.map { it.id }.distinct().size)
    }

    @Test
    fun crossedMilestonesIncreaseTargetUnlockCount() {
        val atFirstMilestone = UnlockEngine.checkAndUnlock(
            meta = meta(accumulatedMinutes = 200),
            plants = allLockedPlants()
        )
        val atSecondMilestone = UnlockEngine.checkAndUnlock(
            meta = meta(accumulatedMinutes = 500),
            plants = allLockedPlants()
        )

        assertEquals(4, atFirstMilestone.size)
        assertEquals(5, atSecondMilestone.size)
    }

    @Test
    fun finalMilestoneCanUnlockWholeCatalog() {
        val unlocked = UnlockEngine.checkAndUnlock(
            meta = meta(accumulatedMinutes = 30_000),
            plants = allLockedPlants()
        )

        assertEquals(PlantDefinitions.all.size, unlocked.size)
        assertNull(
            UnlockEngine.getNextUnlockThreshold(meta(accumulatedMinutes = 30_000))
        )
    }

    // ─── 全收集后「培育新苗」奖励（设计文稿 2.0 §2.4）───────────

    @Test
    fun bonusSeedlingIsZeroBeforeFullCollection() {
        assertEquals(0, UnlockEngine.bonusSeedlingCount(50_000, allUnlocked = false))
        assertEquals(0, UnlockEngine.bonusSeedlingCount(30_000, allUnlocked = true))
    }

    @Test
    fun bonusSeedlingCountsEveryIntervalAfterFullCollection() {
        // 满园阈值 = 30000，间隔 700
        assertEquals(1, UnlockEngine.bonusSeedlingCount(30_700, allUnlocked = true))
        assertEquals(1, UnlockEngine.bonusSeedlingCount(31_399, allUnlocked = true))
        assertEquals(2, UnlockEngine.bonusSeedlingCount(31_400, allUnlocked = true))
        assertEquals(14, UnlockEngine.bonusSeedlingCount(39_800, allUnlocked = true))
    }

    @Test
    fun nextSeedlingThresholdAdvancesByInterval() {
        assertEquals(30_700, UnlockEngine.getNextSeedlingThreshold(30_000))
        assertEquals(31_400, UnlockEngine.getNextSeedlingThreshold(30_701))
        // 全收集前也应给出首个延伸阈值
        assertEquals(30_700, UnlockEngine.getNextSeedlingThreshold(10_000))
    }

    @Test
    fun fullCollectionThresholdEqualsHighestPlantThreshold() {
        val expected = PlantDefinitions.all.maxOf { it.unlockThreshold }
        assertEquals(expected, UnlockEngine.fullCollectionThreshold())
    }

    @Test
    fun dormantStageStaysZeroForRecentReading() {
        assertEquals(0, GardenEngine.dormantStage(today, today))
        assertEquals(0, GardenEngine.dormantStage(today.minusDays(2), today))
        assertEquals(0, GardenEngine.dormantStage(today.minusDays(3), today))
    }

    @Test
    fun dormantStageEscalatesWithReadingGap() {
        assertEquals(1, GardenEngine.dormantStage(today.minusDays(5), today))
        assertEquals(2, GardenEngine.dormantStage(today.minusDays(10), today))
        assertEquals(3, GardenEngine.dormantStage(today.minusDays(20), today))
        assertEquals(3, GardenEngine.dormantStage(today.minusDays(60), today))
    }

    @Test
    fun dormantStageNeverNegativeForFutureDate() {
        assertEquals(0, GardenEngine.dormantStage(today.plusDays(3), today))
    }

    private fun recalculate(
        meta: EngineMeta,
        date: LocalDate,
        minutes: Int
    ): GardenUpdateResult {
        return GardenEngine.recalculate(
            meta = meta,
            plantStates = emptyList(),
            dailyInput = DailyReadInput(
                date = date,
                minutesRead = minutes,
                booksReadToday = 0,
                isNightRead = false
            ),
            today = date
        )
    }

    private fun allLockedPlants(): List<EnginePlantState> {
        return PlantDefinitions.all.map {
            EnginePlantState(
                plantId = it.id,
                isUnlocked = false,
                unlockDate = null
            )
        }
    }

    private fun meta(
        accumulatedMinutes: Int = 0,
        streakDays: Int = 0,
        lastReadDate: LocalDate = today.minusDays(1)
    ): EngineMeta {
        return EngineMeta(
            userId = "test",
            accumulatedMinutes = accumulatedMinutes,
            streakDays = streakDays,
            maxStreakDays = streakDays,
            nightReadDays = 0,
            booksRead = 0,
            lastReadDate = lastReadDate,
            totalReadDays = 0
        )
    }
}