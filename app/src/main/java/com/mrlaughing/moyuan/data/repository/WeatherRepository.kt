package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.data.remote.IpGeoApi
import com.mrlaughing.moyuan.data.remote.OpenMeteoApi
import com.mrlaughing.moyuan.data.remote.OpenMeteoArchiveApi
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 天气仓库：接入真实天气 API
 *
 * 优先级：
 * 1. Open-Meteo API + IP 定位 → 真实天气
 * 2. 定位失败 → 使用北京默认坐标（39.9, 116.4）
 * 3. API 全部失败 → 回退到概率抽取
 */
@Singleton
class WeatherRepository @Inject constructor() {

    // 懒初始化 Retrofit 实例
    private val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(OpenMeteoApi::class.java)
    }

    private val openMeteoArchiveApi: OpenMeteoArchiveApi by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoArchiveApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(OpenMeteoArchiveApi::class.java)
    }

    private val ipGeoApi: IpGeoApi by lazy {
        Retrofit.Builder()
            .baseUrl(IpGeoApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(IpGeoApi::class.java)
    }

    /**
     * 获取当前天气
     *
     * @return Weather 枚举值
     */
    suspend fun fetchWeather(): Weather {
        return try {
            val (lat, lon) = getLocation()
            fetchOpenMeteoWeather(lat, lon)
        } catch (e: Exception) {
            Weather.CLEAR
        }
    }

    /**
     * 获取历史天气数据（用于补算）
     * 
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return Map<LocalDate, Weather> 日期到天气的映射
     */
    suspend fun fetchHistoricalWeather(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Weather> {
        return try {
            val (lat, lon) = getLocation()
            fetchHistoricalWeatherFromArchive(lat, lon, startDate, endDate)
        } catch (e: Exception) {
            // API 失败时返回空映射
            emptyMap()
        }
    }

    /**
     * 通过 IP 定位获取经纬度
     * 失败时使用北京默认坐标
     */
    private suspend fun getLocation(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        try {
            val response = ipGeoApi.getLocation()
            if (response.success != false && response.latitude != null && response.longitude != null) {
                Pair(response.latitude, response.longitude)
            } else {
                DEFAULT_LOCATION
            }
        } catch (e: Exception) {
            DEFAULT_LOCATION
        }
    }

    /**
     * 调用 Open-Meteo API 获取天气并映射到 Weather 枚举
     */
    private suspend fun fetchOpenMeteoWeather(
        lat: Double,
        lon: Double
    ): Weather = withContext(Dispatchers.IO) {
        val response = openMeteoApi.getCurrentWeather(lat, lon)
        val wmoCode = response.current?.weatherCode ?: return@withContext Weather.CLEAR

        SeasonEngine.mapWmoCodeToWeather(wmoCode)
    }

    /**
     * 从 Archive API 获取历史天气
     */
    private suspend fun fetchHistoricalWeatherFromArchive(
        lat: Double,
        lon: Double,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Weather> = withContext(Dispatchers.IO) {
        try {
            val response = openMeteoArchiveApi.getHistoricalWeather(
                latitude = lat,
                longitude = lon,
                startDate = startDate.toString(),
                endDate = endDate.toString()
            )

            val dates = response.daily?.time ?: return@withContext emptyMap()
            val codes = response.daily?.weatherCode ?: return@withContext emptyMap()

            val result = mutableMapOf<LocalDate, Weather>()
            dates.forEachIndexed { index, dateStr ->
                if (index < codes.size) {
                    try {
                        val date = LocalDate.parse(dateStr)
                        val wmoCode = codes[index]
                        val weather = SeasonEngine.mapWmoCodeToWeather(wmoCode)
                        result[date] = weather
                    } catch (_: Exception) {
                        // 忽略解析失败的日期
                    }
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        /** 默认坐标：北京 */
        private val DEFAULT_LOCATION = Pair(39.9, 116.4)
    }
}
