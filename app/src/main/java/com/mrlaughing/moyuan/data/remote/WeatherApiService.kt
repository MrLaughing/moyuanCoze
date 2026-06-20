package com.mrlaughing.moyuan.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 天气 API（免费、无需 API Key）
 * https://open-meteo.com/
 */
interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "Asia/Shanghai"
    ): OpenMeteoResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "current") val current: CurrentWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature2m: Double?,
    @Json(name = "weather_code") val weatherCode: Int?
)

/**
 * IP 地理定位 API（ip-api.com，免费、无需 API Key）
 * http://ip-api.com/
 */
interface IpGeoApi {

    @GET("json")
    suspend fun getLocation(
        @Query("fields") fields: String = "status,lat,lon"
    ): IpGeoResponse

    companion object {
        const val BASE_URL = "http://ip-api.com/"
    }
}

@JsonClass(generateAdapter = true)
data class IpGeoResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?
)
