package com.mrlaughing.moyuan.data.mapper

import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.WitherStage
import com.mrlaughing.moyuan.engine.EngineMeta
import com.mrlaughing.moyuan.engine.EnginePlantState
import java.time.LocalDate

/**
 * DB Entity ↔ Engine Entity 映射器
 *
 * 职责：在 Room 持久化层和引擎计算层之间转换数据类型
 * - DB Entity 使用基本类型（String/Int/Null），适合持久化
 * - Engine Entity 使用枚举和 LocalDate，适合业务计算
 * 
 * 注意：PlantStateEntity.unlockDate 允许为 null
 * - null 表示植物未解锁
 * - 非 null 表示已解锁，解锁日期为该值
 */
object EntityMapper {

    // ─── GardenMeta: DB → Engine ────────────────────────────

    fun toEngineMeta(db: GardenMetaEntity): EngineMeta {
        return EngineMeta(
            userId = "user_${db.id}",
            accumulatedMinutes = db.accumulatedMinutes,
            streakDays = db.streakDays,
            maxStreakDays = db.maxStreakDays,
            nightReadDays = db.nightReadDays,
            booksRead = db.booksRead,
            lastReadDate = parseDate(db.installDate),
            totalReadDays = 0,
            hasRevivedFromDead = db.hasRevivedFromDead,
            activePaths = emptySet()
        )
    }

    // ─── GardenMeta: Engine → DB ────────────────────────────

    fun toDbMeta(engine: EngineMeta, existing: GardenMetaEntity): GardenMetaEntity {
        return existing.copy(
            accumulatedMinutes = engine.accumulatedMinutes,
            streakDays = engine.streakDays,
            maxStreakDays = engine.maxStreakDays,
            nightReadDays = engine.nightReadDays,
            booksRead = engine.booksRead,
            hasRevivedFromDead = engine.hasRevivedFromDead || existing.hasRevivedFromDead
        )
    }

    // ─── PlantState: DB → Engine ────────────────────────────

    /**
     * 将 DB Entity 转换为 Engine Entity
     * 
     * 解锁判定：
     * - unlockDate != null → isUnlocked = true
     * - unlockDate == null → isUnlocked = false
     */
    fun toEnginePlant(db: PlantStateEntity): EnginePlantState {
        val isUnlocked = !db.unlockDate.isNullOrEmpty()
        return EnginePlantState(
            plantId = db.plantId,
            path = db.path,
            isUnlocked = isUnlocked,
            accumulatedMinutes = db.accumulatedMinutes,
            growthLevel = growthLevelFromInt(db.level),
            witherStage = witherStageFromInt(db.witherStage),
            witherStartDate = db.witherStartDate?.let { parseDate(it) },
            lastWateredDate = parseDate(db.lastReadDate),
            justRevived = db.justRevived,
            unlockDate = parseDateOrNull(db.unlockDate),
            reviveDate = parseDateOrNull(db.reviveDate)
        )
    }

    // ─── PlantState: Engine → DB ────────────────────────────

    /**
     * 将 Engine Entity 转换为 DB Entity
     * 
     * 解锁状态转换：
     * - isUnlocked = true → unlockDate = unlockDate?.toString() ?: lastWateredDate.toString()
     * - isUnlocked = false → unlockDate = null
     */
    fun toDbPlant(engine: EnginePlantState, existingId: Int = 0): PlantStateEntity {
        return PlantStateEntity(
            id = existingId,
            plantId = engine.plantId,
            path = engine.path,
            level = engine.growthLevel.level,
            accumulatedMinutes = engine.accumulatedMinutes,
            witherStage = engine.witherStage.stage,
            witherStartDate = engine.witherStartDate?.toString(),
            lastReadDate = engine.lastWateredDate.toString(),
            // 解锁状态：如果是解锁的，保存解锁日期；否则为 null
            unlockDate = if (engine.isUnlocked) {
                engine.unlockDate?.toString() ?: engine.lastWateredDate.toString()
            } else {
                null
            },
            justRevived = engine.justRevived,
            reviveDate = engine.reviveDate?.toString()
        )
    }

    // ─── 辅助方法 ──────────────────────────────────────────

    private fun growthLevelFromInt(level: Int): GrowthLevel {
        return GrowthLevel.entries.find { it.level == level } ?: GrowthLevel.LV1
    }

    private fun witherStageFromInt(stage: Int): WitherStage {
        return WitherStage.entries.find { it.stage == stage } ?: WitherStage.NONE
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try { LocalDate.parse(dateStr) } catch (_: Exception) { LocalDate.now() }
    }

    private fun parseDateOrNull(dateStr: String?): LocalDate? {
        return dateStr?.let { str ->
            try { LocalDate.parse(str) } catch (_: Exception) { null }
        }
    }
}
