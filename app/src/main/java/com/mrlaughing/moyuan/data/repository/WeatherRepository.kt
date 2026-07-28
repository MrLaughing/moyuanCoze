package com.mrlaughing.moyuan.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.mrlaughing.moyuan.BuildConfig
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.data.remote.IpGeoApi
import com.mrlaughing.moyuan.data.remote.OpenMeteoArchiveApi
import com.mrlaughing.moyuan.data.remote.QWeatherApi
import com.mrlaughing.moyuan.data.remote.QWeatherAuthInterceptor
import com.mrlaughing.moyuan.data.remote.QWeatherGeoApi
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 天气仓库：接入和风天气（QWeather）实时天气 API，JWT 鉴权。
 *
 * 数据流：
 * 1. getLocation()：真机定位（GPS/网络，需权限）优先，无权限/失败回退 IP 定位
 * 2. 经纬度 -> 和风 GeoAPI 拿到 LocationID
 * 3. LocationID -> 和风 /v7/weather/now 实时天气 -> 映射到 Weather 枚举
 * 4. 任何一步失败 -> 回退 Weather.CLEAR
 *
 * 历史天气仍走 Open-Meteo Archive（不受本次接入影响）。
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val qWeatherClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(QWeatherAuthInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        redactHeader("Authorization")
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .connectTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .build()
    }

    private val qWeatherApi: QWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(QWeatherApi.BASE_URL)
            .client(qWeatherClient)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(QWeatherApi::class.java)
    }

    private val qWeatherGeoApi: QWeatherGeoApi by lazy {
        Retrofit.Builder()
            .baseUrl(QWeatherGeoApi.BASE_URL)
            .client(qWeatherClient)
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(QWeatherGeoApi::class.java)
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
     */
    suspend fun fetchWeather(): Weather {
        return try {
            val (lat, lon) = getLocation()
            fetchQWeather(lat, lon)
        } catch (e: Exception) {
            Weather.CLEAR
        }
    }

    /**
     * 获取历史天气数据（用于补算，走 Open-Meteo Archive）
     */
    suspend fun fetchHistoricalWeather(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Weather> {
        return try {
            val (lat, lon) = getLocation()
            fetchHistoricalWeatherFromArchive(lat, lon, startDate, endDate)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 通过「真机定位优先 + IP 定位兜底」获取经纬度。
     */
    private suspend fun getLocation(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        // 1. 真机定位（需 ACCESS_FINE/COARSE_LOCATION 权限，已在 Manifest 声明并由 Fragment 运行时申请）
        if (hasLocationPermission()) {
            try {
                val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    return@withContext Pair(loc.latitude, loc.longitude)
                }
            } catch (_: Exception) {
                // 无权限或 Provider 异常，继续走 IP 兜底
            }
        }
        // 2. IP 定位兜底
        try {
            val response = ipGeoApi.getLocation()
            if (response.success != false && response.latitude != null && response.longitude != null) {
                Pair(response.latitude, response.longitude)
            } else {
                DEFAULT_LOCATION
            }
        } catch (_: Exception) {
            DEFAULT_LOCATION
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 调用和风天气：经纬度 -> LocationID -> 实时天气 -> Weather 枚举
     */
    private suspend fun fetchQWeather(lat: Double, lon: Double): Weather = withContext(Dispatchers.IO) {
        // 1. 经纬度转 LocationID（格式：经度,纬度）
        val geo = qWeatherGeoApi.cityLookup("$lon,$lat")
        val locationId = geo.location?.firstOrNull()?.id
        if (geo.code != "200" || locationId.isNullOrBlank()) {
            return@withContext Weather.CLEAR
        }
        // 2. 实时天气
        val resp = qWeatherApi.getWeatherNow(locationId)
        if (resp.code != "200") {
            return@withContext Weather.CLEAR
        }
        mapQWeatherCodeToWeather(resp.now?.code)
    }

    /**
     * 从 Archive API 获取历史天气（Open-Meteo）
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
                        val weather = SeasonEngine.mapWmoCodeToWeather(codes[index])
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

    /**
     * 和风天气现象代码 -> 项目 Weather 枚举映射。
     * 参考：https://dev.qweather.com/docs/resource/weather-code/
     */
    private fun mapQWeatherCodeToWeather(code: String?): Weather {
        return when (code) {
            // 晴
            "100", "150" -> Weather.CLEAR
            // 多云 / 少云 / 晴间多云
            "101", "102", "103" -> Weather.CLOUDY
            // 阴
            "104" -> Weather.OVERCAST
            // 毛毛雨 / 细雨
            "309", "314" -> Weather.DRIZZLE
            // 雨（阵雨/小雨/中雨/大雨/暴雨等，不含雷阵雨）
            "300", "301", "305", "306", "307", "308",
            "310", "311", "312", "315", "316", "317", "318", "399" -> Weather.RAIN
            // 雷阵雨 / 强雷阵雨
            "302", "303" -> Weather.THUNDERSTORM
            // 雪
            "400", "401", "402", "403", "404", "405", "406",
            "407", "408", "409", "410", "499" -> Weather.SNOW
            // 雾 / 霾 / 浮尘 / 沙尘等
            "500", "501", "502", "503", "504", "505", "506",
            "507", "508", "509", "510", "511", "512", "513", "514", "515" -> Weather.FOGGY
            // 风（仅强风/大风/风暴级；弱风视为晴）
            "204", "205", "206", "207", "208", "209", "210", "211", "212", "213", "214" -> Weather.WINDY
            else -> Weather.CLEAR
        }
    }

    companion object {
        /** 默认坐标：北京 */
        private val DEFAULT_LOCATION = Pair(39.9, 116.4)
    }
}
