package com.mrlaughing.moyuan.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 和风天气实时天气 API（/v7/weather/now），JWT 鉴权。
 * 文档：https://dev.qweather.com/docs/api/weather/weather-now/
 *
 * 注意：location 参数需要「和风 LocationID」（如 101010100），
 * 不能直接用经纬度，经纬度需先经 [QWeatherGeoApi.cityLookup] 转换。
 */
interface QWeatherApi {

    @GET("v7/weather/now")
    suspend fun getWeatherNow(
        @Query("location") locationId: String
    ): QWeatherNowResponse

    companion object {
        const val BASE_URL = "https://devapi.qweather.com/"
    }
}

/**
 * 和风 GeoAPI v2：经纬度 / 城市名 -> LocationID。
 * 文档：https://dev.qweather.com/docs/api/geo/city-lookup/
 *
 * cityLookup 的 location 参数支持「经度,纬度」（如 "116.41,39.92"）或城市名。
 */
interface QWeatherGeoApi {

    @GET("geo/v2/city-lookup")
    suspend fun cityLookup(
        @Query("location") location: String
    ): QWeatherGeoResponse

    companion object {
        const val BASE_URL = "https://geoapi.qweather.com/"
    }
}

@JsonClass(generateAdapter = true)
data class QWeatherNowResponse(
    @Json(name = "code") val code: String?,
    @Json(name = "now") val now: QWeatherNow?
)

@JsonClass(generateAdapter = true)
data class QWeatherNow(
    @Json(name = "text") val text: String?,   // 天气现象文字，如 "晴"、"雷阵雨"
    @Json(name = "icon") val icon: String?,   // 天气现象图标代码
    @Json(name = "code") val code: String?,   // 天气现象代码，如 "100"、"302"
    @Json(name = "temp") val temp: String?,   // 实况温度
    @Json(name = "obsTime") val obsTime: String?
)

@JsonClass(generateAdapter = true)
data class QWeatherGeoResponse(
    @Json(name = "code") val code: String?,
    @Json(name = "location") val location: List<QWeatherGeoLocation>?
)

@JsonClass(generateAdapter = true)
data class QWeatherGeoLocation(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "adm1") val adm1: String?,
    @Json(name = "adm2") val adm2: String?
)
