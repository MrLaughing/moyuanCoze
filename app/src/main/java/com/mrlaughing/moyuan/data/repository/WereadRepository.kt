package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.local.db.dao.GardenMetaDao
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.remote.WereadApiClient
import com.mrlaughing.moyuan.data.remote.dto.BookResponse
import com.mrlaughing.moyuan.data.remote.dto.ReadDataResponse
import com.mrlaughing.moyuan.data.remote.dto.ShelfResponse
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 微信读书数据仓库
 * 封装 API 调用与本地 Token 管理
 */
@Singleton
class WereadRepository @Inject constructor(
    private val wereadApiClient: WereadApiClient,
    private val userPrefs: UserPrefs,
    private val gardenMetaDao: GardenMetaDao
) {

    private val api get() = wereadApiClient.wereadApi

    /**
     * 获取当前 Token
     */
    private suspend fun getToken(): String? {
        return userPrefs.wereadToken.first()
    }

    /**
     * 判断是否已授权
     */
    suspend fun isAuthorized(): Boolean {
        return !getToken().isNullOrBlank()
    }

    /**
     * 授权：保存 Token
     */
    suspend fun authorize(token: String) {
        userPrefs.setWereadToken(token)
        // 同步更新 GardenMeta 中的 Token
        gardenMetaDao.updateWereadToken(token)
    }

    /**
     * 取消授权
     */
    suspend fun deauthorize() {
        userPrefs.setWereadToken(null)
        gardenMetaDao.updateWereadToken(null)
    }

    /**
     * 获取阅读数据概览
     */
    suspend fun fetchReadData(): Result<ReadDataResponse> {
        return try {
            val token = getToken()
                ?: return Result.failure(IllegalStateException("未授权：缺少微信读书 Token"))

            val response = api.getReadData(token)
            if (response.isSuccess) {
                Result.success(response)
            } else {
                Result.failure(IOException("API 返回错误: ${response.errMsg} (code=${response.errCode})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取书架列表
     */
    suspend fun fetchShelf(): Result<ShelfResponse> {
        return try {
            val token = getToken()
                ?: return Result.failure(IllegalStateException("未授权：缺少微信读书 Token"))

            val response = api.getShelf(token)
            if (response.isSuccess) {
                Result.success(response)
            } else {
                Result.failure(IOException("API 返回错误: ${response.errMsg} (code=${response.errCode})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取单本书籍详情
     */
    suspend fun fetchBookInfo(bookId: String): Result<BookResponse> {
        return try {
            val token = getToken()
                ?: return Result.failure(IllegalStateException("未授权：缺少微信读书 Token"))

            val response = api.getBookInfo(token, bookId = bookId)
            if (response.isSuccess) {
                Result.success(response)
            } else {
                Result.failure(IOException("API 返回错误: ${response.errMsg} (code=${response.errCode})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
