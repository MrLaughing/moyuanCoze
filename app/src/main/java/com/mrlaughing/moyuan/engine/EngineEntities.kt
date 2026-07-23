package com.mrlaughing.moyuan.engine

import java.time.LocalDate

/**
 * 花园全局元数据（引擎计算用，非Room持久化对象）
 *
 * 与 db.entity.GardenMetaEntity 的区别：
 * - 本类使用枚举类型而非字符串/整型
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
    val totalReadDays: Int
)

/**
 * 单株植物状态（引擎计算用，非Room持久化对象）
 *
 * 2.0 收敛模型：仅保留解锁状态。移除 1.0 的等级/路径/枯萎/累计分钟等概念。
 */
data class EnginePlantState(
    val plantId: String,
    val isUnlocked: Boolean,
    val unlockDate: LocalDate?
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
