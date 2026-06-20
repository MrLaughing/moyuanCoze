package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 微信读书阅读数据响应
 */
@JsonClass(generateAdapter = true)
data class ReadDataDto(
    @Json(name = "readTime") val readTime: Long,           // 累计阅读时长(秒)
    @Json(name = "bookCount") val bookCount: Int,           // 已读书目数
    @Json(name = "nightReadDays") val nightReadDays: Int,   // 夜读天数
    @Json(name = "maxStreakDays") val maxStreakDays: Int,   // 最高连续天数
    @Json(name = "currentStreakDays") val currentStreakDays: Int  // 当前连续天数
)

/**
 * 阅读数据响应包装
 */
@JsonClass(generateAdapter = true)
data class ReadDataResponse(
    @Json(name = "data") val data: ReadDataDto?,
    @Json(name = "errCode") val errCode: Int = 0,
    @Json(name = "errMsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0 && data != null
}
