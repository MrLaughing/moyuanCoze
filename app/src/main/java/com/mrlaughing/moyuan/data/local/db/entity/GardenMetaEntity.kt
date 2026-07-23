package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 花园元数据表
 * 存储花园全局状态与用户配置
 */
@Entity(tableName = "garden_meta")
data class GardenMetaEntity(
    @PrimaryKey val id: Int = 1,  // 单例
    val installDate: String,       // 安装日期
    val accumulatedMinutes: Int,   // 总累计阅读分钟
    val streakDays: Int,           // 当前连续天数
    val maxStreakDays: Int,        // 历史最高连续天数
    val nightReadDays: Int,        // 累计夜读天数
    val booksRead: Int,            // 已读书目数
    val currentWeather: String,    // 当前天气枚举名
    val weatherDate: String?,      // 天气roll日期
    val lastSyncDate: String?,     // 上次同步日期
    val lastReadDate: String? = null, // 最近一次实际发生阅读的日期，用于连续天数计算
    val lastNightReadDate: String? = null, // 上次计入夜读天数的日期（去重用，避免同一天多次同步重复计数）
    val syncHour: Int = 8,         // 每日同步时间(时)
    val syncMinute: Int = 0,       // 每日同步时间(分)
    val todayReadMinutes: Int = 0, // 今日阅读分钟数
    val totalReadDays: Int = 0     // 累计总阅读天数（权威值来自微信读书 API overallData.readDays，含全部历史）
)
