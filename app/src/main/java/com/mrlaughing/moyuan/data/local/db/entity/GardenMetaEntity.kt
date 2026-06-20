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
    val hasRevivedFromDead: Boolean, // 是否有枯寂复活
    val wereadToken: String?,      // 微信读书Token
    val syncHour: Int = 8,         // 每日同步时间(时)
    val syncMinute: Int = 0        // 每日同步时间(分)
)
