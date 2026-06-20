package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.data.remote.IpGeoApi
import com.mrlaughing.moyuan.data.remote.OpenMeteoApi
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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

    // 懒初始化 Retrofit 实例（避免应用启动时创建不必要的网络客户端）
    private val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(OpenMeteoApi::class.java)
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
     * @param season 当前季节（用于 WMO 代码映射时的季节判断）
     * @param isNight 是否为夜间（用于月夜判断）
     * @return Weather 枚举值
     */
    suspend fun fetchWeather(season: Season, isNight: Boolean): Weather {
        return try {
            val (lat, lon) = getLocation()
            val weather = fetchOpenMeteoWeather(lat, lon, season, isNight)
            weather
        } catch (e: Exception) {
            // 全部失败，回退到概率抽取
            SeasonEngine.rollWeather(season, isNight)
        }
    }

    /**
     * 通过 IP 定位获取经纬度
     * 失败时使用北京默认坐标
     */
    private suspend fun getLocation(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        try {
            val response = ipGeoApi.getLocation()
            if (response.status == "success" && response.lat != null && response.lon != null) {
                Pair(response.lat, response.lon)
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
        lon: Double,
        season: Season,
        isNight: Boolean
    ): Weather = withContext(Dispatchers.IO) {
        val response = openMeteoApi.getCurrentWeather(lat, lon)
        val wmoCode = response.current?.weatherCode ?: return@withContext SeasonEngine.rollWeather(season, isNight)

        SeasonEngine.mapWmoCodeToWeather(wmoCode, season, isNight)
    }

    companion object {
        /** 默认坐标：北京 */
        private val DEFAULT_LOCATION = Pair(39.9, 116.4)
    }
}
