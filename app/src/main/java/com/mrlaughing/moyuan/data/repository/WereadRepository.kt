package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.local.db.dao.GardenMetaDao
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.remote.GatewayRequest
import com.mrlaughing.moyuan.data.remote.ReadDataRequest
import com.mrlaughing.moyuan.data.remote.WereadApiClient
import com.mrlaughing.moyuan.data.remote.dto.BookProgressResponse
import com.mrlaughing.moyuan.data.remote.dto.BookResponse
import com.mrlaughing.moyuan.data.remote.dto.ReadDataResponse
import com.mrlaughing.moyuan.data.remote.dto.ShelfResponse
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WereadRepository @Inject constructor(
    private val wereadApiClient: WereadApiClient,
    private val userPrefs: UserPrefs,
    private val gardenMetaDao: GardenMetaDao
) {

    private val api get() = wereadApiClient.wereadApi

    private suspend fun getToken(): String? = userPrefs.wereadToken.first()

    suspend fun isAuthorized(): Boolean = !getToken().isNullOrBlank()

    suspend fun authorize(token: String) {
        userPrefs.setWereadToken(token)
        gardenMetaDao.updateWereadToken(token)
    }

    suspend fun deauthorize() {
        userPrefs.setWereadToken(null)
        gardenMetaDao.updateWereadToken(null)
    }

    suspend fun fetchReadDataOverall(): Result<ReadDataResponse> =
        fetchReadData(ReadDataRequest(mode = "overall"))

    suspend fun fetchReadDataWeekly(): Result<ReadDataResponse> =
        fetchReadData(ReadDataRequest(mode = "weekly"))

    suspend fun fetchReadData(request: ReadDataRequest = ReadDataRequest()): Result<ReadDataResponse> {
        return try {
            val token = getToken()
                ?: return Result.failure(IllegalStateException("未授权：缺少微信读书 Token"))
            val response = api.getReadData("Bearer $token", request)
            if (response.isSuccess) Result.success(response)
            else Result.failure(IOException("API 错误: ${response.errMsg} (code=${response.errCode})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchShelf(): Result<ShelfResponse> {
        return try {
            val token = getToken()
                ?: return Result.failure(IllegalStateException("未授权：缺少微信读书 Token"))
            val response = api.getShelf("Bearer $token", GatewayRequest(apiName = "/shelf/sync"))
            if (response.isSuccess) Result.success(response)
            else Result.failure(IOException("API 错误: ${response.errMsg} (code=${response.errCode})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBookInfo(bookId: String): Result<BookResponse> {
        return try {
            val token = getToken() ?: ""
            val request = mapOf(
                "api_name" to "/book/info",
                "bookId" to bookId,
                "skill_version" to "1.0.3"
            )
            val response = wereadApiClient.retrofit.create(WereadBookApi::class.java)
                .getBookInfo("Bearer $token", request)
            if (response.errCode == 0) Result.success(response)
            else Result.failure(IOException("API 错误: ${response.errMsg} (code=${response.errCode})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBookProgress(bookId: String): Result<BookProgressResponse> {
        return try {
            val token = getToken()
                ?: return Result.failure(IllegalStateException("未授权"))
            val request = mapOf(
                "api_name" to "/book/getprogress",
                "bookId" to bookId,
                "skill_version" to "1.0.3"
            )
            val response = wereadApiClient.retrofit.create(WereadProgressApi::class.java)
                .getProgress("Bearer $token", request)
            if (response.errCode == 0) Result.success(response)
            else Result.failure(IOException("API 错误: ${response.errMsg}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private interface WereadBookApi {
    @retrofit2.http.POST("agent/gateway")
    suspend fun getBookInfo(
        @retrofit2.http.Header("Authorization") auth: String,
        @retrofit2.http.Body body: Map<String, @JvmSuppressWildcards Any>
    ): BookResponse
}

private interface WereadProgressApi {
    @retrofit2.http.POST("agent/gateway")
    suspend fun getProgress(
        @retrofit2.http.Header("Authorization") auth: String,
        @retrofit2.http.Body body: Map<String, @JvmSuppressWildcards Any>
    ): BookProgressResponse
}
