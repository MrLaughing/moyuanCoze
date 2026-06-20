package com.mrlaughing.moyuan.data.remote

import com.mrlaughing.moyuan.data.remote.dto.BookResponse
import com.mrlaughing.moyuan.data.remote.dto.ReadDataResponse
import com.mrlaughing.moyuan.data.remote.dto.ShelfResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 微信读书 API 接口
 * 所有请求走统一网关 https://i.weread.qq.com/api/agent/gateway
 */
interface WereadApi {

    /**
     * 获取阅读数据概览
     */
    @FormUrlEncoded
    @POST("agent/gateway")
    suspend fun getReadData(
        @Field("token") token: String,
        @Field("type") type: String = "read_data"
    ): ReadDataResponse

    /**
     * 获取书架列表
     */
    @FormUrlEncoded
    @POST("agent/gateway")
    suspend fun getShelf(
        @Field("token") token: String,
        @Field("type") type: String = "shelf"
    ): ShelfResponse

    /**
     * 获取单本书籍详情
     */
    @FormUrlEncoded
    @POST("agent/gateway")
    suspend fun getBookInfo(
        @Field("token") token: String,
        @Field("type") type: String = "book_info",
        @Field("bookId") bookId: String
    ): BookResponse

    companion object {
        const val BASE_URL = "https://i.weread.qq.com/api/"
    }
}
