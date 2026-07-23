package com.mrlaughing.moyuan.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书目追踪表
 * 追踪微信读书中的每本书的阅读进度
 */
@Entity(tableName = "book_tracking", indices = [Index(value = ["bookId"], unique = true)])
data class BookTrackingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: String,            // 微信读书书籍ID
    val title: String,             // 书名
    val author: String?,           // 作者
    val progressPercent: Int,      // 阅读进度百分比
    val readMinutes: Int,          // 累计阅读分钟
    val startDate: String,         // 开始阅读日期
    val lastReadDate: String?      // 最后阅读日期
)
