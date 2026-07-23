package com.mrlaughing.moyuan.data.remote

import com.mrlaughing.moyuan.data.remote.dto.ReadDataResponse
import com.mrlaughing.moyuan.data.remote.dto.ShelfResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 微信读书 Agent Gateway API
 * 统一入口: POST https://i.weread.qq.com/api/agent/gateway
 * 鉴权: Authorization: Bearer $WEREAD_API_KEY
 * Body: JSON, api_name 指定接口, skill_version 必带
 */
interface WereadApi {

    /**
     * 获取阅读统计数据
     * mode: weekly=本周, monthly=本月, annually=本年, overall=总计
     */
    @POST("agent/gateway")
    suspend fun getReadData(
        @Header("Authorization") auth: String,
        @Body body: ReadDataRequest
    ): ReadDataResponse

    /**
     * 获取书架列表
     */
    @POST("agent/gateway")
    suspend fun getShelf(
        @Header("Authorization") auth: String,
        @Body body: GatewayRequest
    ): ShelfResponse

    companion object {
        const val BASE_URL = "https://i.weread.qq.com/api/"
        const val SKILL_VERSION = "1.0.3"
    }
}

/**
 * Gateway 基础请求
 */
@JsonClass(generateAdapter = true)
data class GatewayRequest(
    @Json(name = "api_name") val apiName: String,
    @Json(name = "skill_version") val skillVersion: String = WereadApi.SKILL_VERSION
)

/**
 * 阅读数据请求
 */
@JsonClass(generateAdapter = true)
data class ReadDataRequest(
    @Json(name = "api_name") val apiName: String = "/readdata/detail",
    @Json(name = "mode") val mode: String = "overall",
    @Json(name = "baseTime") val baseTime: Int = 0,
    @Json(name = "skill_version") val skillVersion: String = WereadApi.SKILL_VERSION
)
