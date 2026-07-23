package com.mrlaughing.moyuan.data.remote

import com.mrlaughing.moyuan.BuildConfig
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
 * Weread API HTTP 客户端
 * 
 * 注意：Authorization header 由 Repository 层通过 @Header("Authorization") 显式传递，
 * 此处仅保留日志拦截器用于调试。
 */
@Singleton
class WereadApiClient @Inject constructor() {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
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

    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WereadApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }

    val wereadApi: WereadApi by lazy {
        retrofit.create(WereadApi::class.java)
    }
}
