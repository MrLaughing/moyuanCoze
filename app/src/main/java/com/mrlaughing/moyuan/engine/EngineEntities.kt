package com.mrlaughing.moyuan.engine

import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.PlantPath
import com.mrlaughing.moyuan.data.model.WitherStage
import java.time.LocalDate

/**
 * 花园全局元数据（引擎计算用，非Room持久化对象）
 *
 * 与 db.entity.GardenMetaEntity 的区别：
 * - 本类使用枚举类型（Season/Weather/Set<PlantPath>）而非字符串/整型
 * - 本类不含 Room 注解和同步配置字段
 * - Repository 层负责两者之间的映射
 */
data class EngineMeta(
    val userId: String,
    val accumulatedMinutes: Int,
    val streakDays: Int,
    val maxStreakDays: Int,
    val nightReadDays: Int,
    val booksRead: Int,
    val lastReadDate: LocalDate,
    val totalReadDays: Int,
    val hasRevivedFromDead: Boolean = false,
    val activePaths: Set<PlantPath> = emptySet()
)

/**
 * 单株植物状态（引擎计算用，非Room持久化对象）
 *
 * 与 db.entity.PlantStateEntity 的区别：
 * - 本类使用枚举类型（GrowthLevel/WitherStage）而非整型
 * - 本类不含 Room 注解和自增主键
 * - Repository 层负责两者之间的映射
 */
data class EnginePlantState(
    val plantId: String,
    val path: String,
    val isUnlocked: Boolean,
    val accumulatedMinutes: Int,
    val growthLevel: GrowthLevel,
    val witherStage: WitherStage,
    val witherStartDate: LocalDate? = null,
    val lastWateredDate: LocalDate,
    val justRevived: Boolean = false,
    val unlockDate: LocalDate? = null,
    val reviveDate: LocalDate? = null
)

/**
 * 每日阅读记录输入
 */
data class DailyReadInput(
    val date: LocalDate,
    val minutesRead: Int,
    val booksReadToday: Int,
    val isNightRead: Boolean
)
