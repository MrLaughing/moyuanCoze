package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 安装时刻基准快照表
 * 记录用户安装App时的微信读书累计数据，用于计算增量
 */
@Entity(tableName = "base_snapshot")
data class BaseSnapshotEntity(
    @PrimaryKey val id: Int = 1,  // 单例
    val snapshotDate: String,      // 快照日期 yyyy-MM-dd
    val totalReadMinutes: Int,     // 安装时累计阅读分钟
    val totalBooksRead: Int,       // 安装时已读书目数
    val nightReadDays: Int,        // 安装时夜读天数
    val maxStreakDays: Int,        // 安装时最高连续天数
    val createdAt: Long = System.currentTimeMillis()
)
