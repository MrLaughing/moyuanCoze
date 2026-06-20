package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 书籍详情响应（/book/info）- 扁平结构
 */
@JsonClass(generateAdapter = true)
data class BookResponse(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "translator") val translator: String? = null,
    @Json(name = "cover") val cover: String = "",
    @Json(name = "intro") val intro: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "publisher") val publisher: String = "",
    @Json(name = "publishTime") val publishTime: String = "",
    @Json(name = "isbn") val isbn: String = "",
    @Json(name = "wordCount") val wordCount: Long = 0,
    @Json(name = "newRating") val newRating: Double = 0.0,
    @Json(name = "newRatingCount") val newRatingCount: Int = 0,
    @Json(name = "errcode") val errCode: Int = 0,
    @Json(name = "errmsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0
}

/**
 * 阅读进度响应（/book/getprogress）
 */
@JsonClass(generateAdapter = true)
data class BookProgressResponse(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "timestamp") val timestamp: Long = 0,
    @Json(name = "book") val progress: BookProgress? = null,
    @Json(name = "errcode") val errCode: Int = 0,
    @Json(name = "errmsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0
}

@JsonClass(generateAdapter = true)
data class BookProgress(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "progress") val progress: Int = 0,
    @Json(name = "readTime") val readTime: Long = 0
)
