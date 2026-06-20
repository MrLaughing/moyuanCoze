package com.mrlaughing.moyuan.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 阅读统计响应（/readdata/detail）
 * Gateway 返回扁平结构，错误时返回 { errcode, errmsg }
 */
@JsonClass(generateAdapter = true)
data class ReadDataResponse(
    @Json(name = "readTimes") val readTimes: Map<String, Long> = emptyMap(),
    @Json(name = "readDays") val readDays: Int = 0,
    @Json(name = "totalReadTime") val totalReadTime: Long = 0,
    @Json(name = "dayAverageReadTime") val dayAverageReadTime: Int = 0,
    @Json(name = "compare") val compare: Double = 0.0,
    @Json(name = "baseTime") val baseTime: Long = 0,
    @Json(name = "readLongest") val readLongest: List<ReadLongestBook> = emptyList(),
    @Json(name = "readStat") val readStat: List<ReadStatItem> = emptyList(),
    @Json(name = "preferCategory") val preferCategory: List<ApiPreferCategory> = emptyList(),
    @Json(name = "preferCategoryWord") val preferCategoryWord: String? = null,
    @Json(name = "preferTimeWord") val preferTimeWord: String? = null,
    @Json(name = "preferAuthor") val preferAuthor: List<ApiPreferAuthor> = emptyList(),
    @Json(name = "preferBooks") val preferBooks: List<ApiPreferBook> = emptyList(),
    @Json(name = "medals") val medals: List<ApiMedalItem> = emptyList(),
    @Json(name = "registTime") val registTime: Long = 0,
    @Json(name = "wrReadTime") val wrReadTime: Long = 0,
    @Json(name = "wrListenTime") val wrListenTime: Long = 0,
    @Json(name = "rank") val rank: ApiRankInfo? = null,
    @Json(name = "errcode") val errCode: Int = 0,
    @Json(name = "errmsg") val errMsg: String? = null
) {
    val isSuccess: Boolean get() = errCode == 0

    fun getTodayReadSeconds(): Long {
        val todayStart = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toEpochSecond()
        return readTimes.entries
            .filter { it.key.toLongOrNull()?.let { k -> k >= todayStart } ?: false }
            .sumOf { it.value }
            .takeIf { it > 0 }
            ?: 0L
    }
}

/** API 实际返回的分类偏好结构 */
@JsonClass(generateAdapter = true)
data class ApiPreferCategory(
    @Json(name = "categoryId") val categoryId: Long = 0,
    @Json(name = "categoryTitle") val categoryTitle: String = "",
    @Json(name = "readingCount") val readingCount: Int = 0,
    @Json(name = "readingTime") val readingTime: Long = 0
)

/** API 实际返回的作者偏好结构 - readTime 是字符串如"8小时33分钟" */
@JsonClass(generateAdapter = true)
data class ApiPreferAuthor(
    @Json(name = "authorId") val authorId: Long = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "count") val count: Int = 0,
    @Json(name = "readTime") val readTime: String = ""
)

/** API 实际返回的偏好书籍结构 */
@JsonClass(generateAdapter = true)
data class ApiPreferBook(
    @Json(name = "type") val type: Int = 0,
    @Json(name = "title") val title: String = ""
)

/** API 实际返回的勋章结构 */
@JsonClass(generateAdapter = true)
data class ApiMedalItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "hint") val hint: String = "",
    @Json(name = "displayText") val displayText: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "type") val type: Int = 0,
    @Json(name = "subtype") val subtype: Int = 0,
    @Json(name = "level") val level: Int = 0
)

/** API 实际返回的排名结构 */
@JsonClass(generateAdapter = true)
data class ApiRankInfo(
    @Json(name = "text") val text: String = "",
    @Json(name = "scheme") val scheme: String? = null
)

@JsonClass(generateAdapter = true)
data class ReadLongestBook(
    @Json(name = "book") val book: BookInfo? = null,
    @Json(name = "readTime") val readTime: Long = 0,
    @Json(name = "tags") val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BookInfo(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "cover") val cover: String = "",
    @Json(name = "intro") val intro: String = "",
    @Json(name = "format") val format: String = "",
    @Json(name = "finished") val finished: Int = 0,
    @Json(name = "lastChapterIdx") val lastChapterIdx: Int = 0
)

@JsonClass(generateAdapter = true)
data class ReadStatItem(
    @Json(name = "stat") val stat: String = "",
    @Json(name = "counts") val counts: String = ""
)
