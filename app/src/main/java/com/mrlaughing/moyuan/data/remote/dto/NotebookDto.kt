package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 笔记本列表响应（/user/notebooks）
 */
@JsonClass(generateAdapter = true)
data class NotebookResponse(
    @Json(name = "totalBookCount") val totalBookCount: Int = 0,
    @Json(name = "noteBookCount") val noteBookCount: Int = 0,
    @Json(name = "books") val books: List<NotebookItem> = emptyList(),
    @Json(name = "errcode") val errCode: Int = 0,
    @Json(name = "errmsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0
}

@JsonClass(generateAdapter = true)
data class NotebookItem(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "book") val book: BookInfo? = null,
    @Json(name = "reviewCount") val reviewCount: Int = 0,
    @Json(name = "noteCount") val noteCount: Int = 0,
    @Json(name = "bookmarkCount") val bookmarkCount: Int = 0,
    @Json(name = "sort") val sort: Long = 0
) {
    val totalCount: Int get() = reviewCount + noteCount + bookmarkCount
}

/**
 * 划线列表响应（/book/bookmarklist）
 */
@JsonClass(generateAdapter = true)
data class BookmarkListResponse(
    @Json(name = "updated") val updated: List<BookmarkItem> = emptyList(),
    @Json(name = "book") val book: BookInfo? = null,
    @Json(name = "errcode") val errCode: Int = 0,
    @Json(name = "errmsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0
}

@JsonClass(generateAdapter = true)
data class BookmarkItem(
    @Json(name = "bookmarkId") val bookmarkId: String = "",
    @Json(name = "markText") val markText: String = "",
    @Json(name = "chapterUid") val chapterUid: Int = 0,
    @Json(name = "chapterName") val chapterName: String? = null,
    @Json(name = "createTime") val createTime: Long = 0,
    @Json(name = "range") val range: String? = null
)
