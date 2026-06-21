package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 成就实体
 * 存储用户已解锁或进行中的成就信息
 */
@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,           // 成就ID: "first_sync", "bookworm" 等
    val category: String,                  // 分类: "READING", "GROWTH", "MILESTONE"
    val name: String,                      // 成就名: "初芽"
    val description: String,               // 描述文案: "墨香初识，书卷初开"
    val condition: String,                // 解锁条件描述: "首次同步"
    val targetValue: Int,                 // 目标值: 1, 10, 50 等
    val currentValue: Int = 0,            // 当前进度
    val isUnlocked: Boolean = false,     // 是否已解锁
    val unlockedDate: String? = null       // 解锁日期
)
