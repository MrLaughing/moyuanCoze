package com.mrlaughing.moyuan.data.remote

import com.mrlaughing.moyuan.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为和风天气请求注入 JWT 鉴权头：Authorization: Bearer <jwt>。
 *
 * 每次请求动态生成 JWT（iat = 当前时间 - 30s，exp = iat + 900s），满足官方建议。
 * 若 BuildConfig 中三项凭据有任一为空（用户尚未配置），则不添加鉴权头，
 * 请求将返回 401，由上层 [com.mrlaughing.moyuan.data.repository.WeatherRepository] 回退到 [com.mrlaughing.moyuan.data.model.Weather.CLEAR]。
 */
class QWeatherAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val projectId = BuildConfig.QWEATHER_PROJECT_ID
        val keyId = BuildConfig.QWEATHER_KEY_ID
        val privateKey = BuildConfig.QWEATHER_PRIVATE_KEY

        val request = if (projectId.isBlank() || keyId.isBlank() || privateKey.isBlank()) {
            chain.request()
        } else {
            val jwt = QWeatherAuth.createToken(projectId, keyId, privateKey)
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $jwt")
                .build()
        }
        return chain.proceed(request)
    }
}
