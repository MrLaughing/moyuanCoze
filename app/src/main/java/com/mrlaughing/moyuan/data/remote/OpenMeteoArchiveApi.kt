package com.mrlaughing.moyuan.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Archive API（历史天气数据）
 * https://open-meteo.com/en/docs/historical-weather
 * 
 * 免费、无需 API Key
 * 可查询过去任意日期的天气数据
 */
interface OpenMeteoArchiveApi {

    /**
     * 获取历史天气数据
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param startDate 起始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @param daily 返回的日数据字段
     * @param timezone 时区
     * @return OpenMeteoArchiveResponse
     */
    @GET("v1/archive")
    suspend fun getHistoricalWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "Asia/Shanghai"
    ): OpenMeteoArchiveResponse

    companion object {
        const val BASE_URL = "https://archive-api.open-meteo.com/"
    }
}

/**
 * Open-Meteo Archive 响应
 */
@JsonClass(generateAdapter = true)
data class OpenMeteoArchiveResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "daily") val daily: DailyWeatherData?
)

@JsonClass(generateAdapter = true)
data class DailyWeatherData(
    @Json(name = "time") val time: List<String>?,           // 日期列表
    @Json(name = "weather_code") val weatherCode: List<Int>? // WMO 天气代码
)
