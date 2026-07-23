package com.mrlaughing.moyuan.data.mapper

import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
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
            // lastReadDate 与 lastSyncDate 分离：同步成功不代表当天实际发生阅读。
            // 未有阅读记录时默认"昨天"，目的：
            //   1) 首次同步时 lastReadDate==昨天，updateMeta 会把它当作连续第1天（streakDays=1）
            //   2) 不会把单纯的同步行为误判成阅读行为
            lastReadDate = parseDateOrNull(db.lastReadDate) ?: LocalDate.now().minusDays(1),
            // 读取持久化的真实总阅读天数（权威值来自微信读书 API，见 SyncWorker step6），不再硬编码 0
            totalReadDays = db.totalReadDays
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
            lastReadDate = engine.lastReadDate.toString(),
            // 持久化总阅读天数（引擎透传 SyncWorker 设置的 API 权威值）
            totalReadDays = engine.totalReadDays
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
            isUnlocked = isUnlocked,
            unlockDate = parseDateOrNull(db.unlockDate)
        )
    }

    // ─── PlantState: Engine → DB ────────────────────────────

    /**
     * 将 Engine Entity 转换为 DB Entity
     *
     * 解锁状态转换：
     * - isUnlocked = true → unlockDate = unlockDate?.toString()
     * - isUnlocked = false → unlockDate = null
     *
     * 注意：isInGarden / gardenOrder 由用户手动控制（"放入花园"），
     * 引擎不感知，因此必须从 existing 中保留，避免被覆盖为默认值。
     */
    fun toDbPlant(engine: EnginePlantState, existing: PlantStateEntity): PlantStateEntity {
        return existing.copy(
            plantId = engine.plantId,
            // 解锁状态：如果是解锁的，保存解锁日期；否则为 null
            unlockDate = if (engine.isUnlocked) {
                engine.unlockDate?.toString() ?: LocalDate.now().toString()
            } else {
                null
            }
        )
    }

    // ─── 辅助方法 ──────────────────────────────────────────

    private fun parseDate(dateStr: String): LocalDate {
        return try { LocalDate.parse(dateStr) } catch (_: Exception) { LocalDate.now() }
    }

    private fun parseDateOrNull(dateStr: String?): LocalDate? {
        return dateStr?.let { str ->
            try { LocalDate.parse(str) } catch (_: Exception) { null }
        }
    }
}
