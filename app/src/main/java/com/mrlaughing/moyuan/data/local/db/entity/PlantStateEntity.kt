package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 植物状态表（2.0 收敛模型）
 *
 * 仅保留解锁与花园摆放状态，移除 1.0 的等级(level)/路径(path)/枯萎(wither) 等废弃字段。
 *
 * 注意：unlockDate 允许为 null，表示植物未解锁
 * 未解锁的植物 unlockDate = null，解锁后设置为解锁日期
 */
@Entity(tableName = "plant_state", indices = [Index(value = ["plantId"], unique = true)])
data class PlantStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: String,           // 植物ID: changpu, wenzhu 等
    val unlockDate: String? = null,// 解锁日期，null 表示未解锁
    val isInGarden: Boolean = false, // 是否已放入花园（自定义模式）
    val gardenOrder: Int = 0       // 花园内排序（自定义模式）
)
