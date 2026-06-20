package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 书架响应（/shelf/sync）- 扁平结构
 */
@JsonClass(generateAdapter = true)
data class ShelfResponse(
    @Json(name = "books") val books: List<ShelfBook> = emptyList(),
    @Json(name = "albums") val albums: List<ShelfAlbum> = emptyList(),
    @Json(name = "mp") val mp: MpInfo? = null,
    @Json(name = "archive") val archive: List<ShelfArchive> = emptyList(),
    @Json(name = "errcode") val errCode: Int = 0,
    @Json(name = "errmsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0
}

@JsonClass(generateAdapter = true)
data class ShelfBook(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "cover") val cover: String = "",
    @Json(name = "updateTime") val updateTime: Long = 0,
    @Json(name = "finishReading") val finishReading: Int = 0,
    @Json(name = "readUpdateTime") val readUpdateTime: Long = 0,
    @Json(name = "secret") val secret: Int = 0
)

@JsonClass(generateAdapter = true)
data class ShelfAlbum(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "cover") val cover: String = ""
)

@JsonClass(generateAdapter = true)
data class MpInfo(
    @Json(name = "mp") val mp: List<ShelfMpItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShelfMpItem(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = ""
)

@JsonClass(generateAdapter = true)
data class ShelfArchive(
    @Json(name = "archiveId") val archiveId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "bookIds") val bookIds: List<String> = emptyList()
)
