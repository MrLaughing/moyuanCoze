package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日阅读记录表
 * 存储每日增量阅读数据
 * 
 * 新增字段：
 * - source: 数据来源 (sync=同步, backfill=补算, manual=手动)
 * - weather: 当日天气枚举名
 */
@Entity(tableName = "daily_record", indices = [Index(value = ["date"], unique = true)])
data class DailyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,              // yyyy-MM-dd
    val readMinutes: Int,          // 当日增量阅读分钟
    val hasNightRead: Boolean,     // 是否有夜间阅读
    val newBookCount: Int,         // 当日新读书目数
    val syncedAt: Long = System.currentTimeMillis(),
    val source: String = "sync",   // 数据来源: sync=同步, backfill=补算, manual=手动
    val weather: String? = null    // 当日天气枚举名
)
