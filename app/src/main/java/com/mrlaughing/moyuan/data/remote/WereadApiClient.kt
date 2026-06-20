package com.mrlaughing.moyuan.data.remote

import com.mrlaughing.moyuan.BuildConfig
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 微信读书 API 客户端
 * 提供 OkHttp 配置与 Retrofit 实例
 */
@Singleton
class WereadApiClient @Inject constructor(
    private val userPrefs: UserPrefs
) {

    /**
     * 构建 OkHttpClient
     * - Bearer Token 拦截器
     * - 日志拦截器（Debug）
     * - 超时设置
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Bearer Token 拦截器
            .addInterceptor { chain ->
                val original = chain.request()
                // 从 DataStore 获取 Token（使用 runBlocking 仅用于拦截器）
                val token = runBlocking { userPrefs.wereadToken.first() }
                val request = if (!token.isNullOrBlank()) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            // 日志拦截器（仅 Debug 模式启用）
            .apply {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            // 超时设置
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit 实例
     */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WereadApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build()
    }

    /**
     * WereadApi 实例
     */
    val wereadApi: WereadApi by lazy {
        retrofit.create(WereadApi::class.java)
    }

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L
    }
}
