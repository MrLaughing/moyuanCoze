package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 微信读书书籍信息
 */
@JsonClass(generateAdapter = true)
data class BookDto(
    @Json(name = "bookId") val bookId: String,               // 书籍ID
    @Json(name = "title") val title: String,                 // 书名
    @Json(name = "author") val author: String? = null,       // 作者
    @Json(name = "cover") val cover: String? = null,         // 封面URL
    @Json(name = "progress") val progress: Int = 0,          // 阅读进度百分比 0-100
    @Json(name = "readTime") val readTime: Long = 0,         // 累计阅读时长(秒)
    @Json(name = "startDate") val startDate: String? = null,  // 开始阅读日期
    @Json(name = "lastReadDate") val lastReadDate: String? = null  // 最后阅读日期
)

/**
 * 单本书籍信息响应包装
 */
@JsonClass(generateAdapter = true)
data class BookResponse(
    @Json(name = "data") val data: BookDto?,
    @Json(name = "errCode") val errCode: Int = 0,
    @Json(name = "errMsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0 && data != null
}
