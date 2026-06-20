package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 微信读书书架响应
 */
@JsonClass(generateAdapter = true)
data class ShelfDto(
    @Json(name = "books") val books: List<BookDto> = emptyList()
)

/**
 * 书架响应包装
 */
@JsonClass(generateAdapter = true)
data class ShelfResponse(
    @Json(name = "data") val data: ShelfDto?,
    @Json(name = "errCode") val errCode: Int = 0,
    @Json(name = "errMsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0 && data != null
}
