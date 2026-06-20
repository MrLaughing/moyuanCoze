package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 植物状态表
 * 存储每棵植物的生长、枯萎、复活状态
 * 
 * 注意：unlockDate 允许为 null，表示植物未解锁
 * 未解锁的植物 unlockDate = null，解锁后设置为解锁日期
 */
@Entity(tableName = "plant_state", indices = [Index(value = ["plantId"], unique = true)])
data class PlantStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: String,           // 植物ID: changpu, wenzhu 等
    val path: String,              // 所属路径: JIMO, BINGZHU 等
    val level: Int,                // 当前等级 1-5
    val accumulatedMinutes: Int,   // 累计有效灌溉分钟
    val witherStage: Int,          // 枯萎阶段 0-4
    val witherStartDate: String?,  // 枯萎开始日期
    val lastReadDate: String,      // 最后阅读日期
    val unlockDate: String?,       // 解锁日期，null 表示未解锁
    val justRevived: Boolean,      // 是否刚复活
    val reviveDate: String?        // 复活日期
)
