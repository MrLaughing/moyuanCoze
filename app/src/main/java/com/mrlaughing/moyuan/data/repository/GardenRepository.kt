package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.local.db.dao.GardenMetaDao
import com.mrlaughing.moyuan.data.local.db.dao.PlantStateDao
import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 花园状态组合数据类
 */
data class GardenState(
    val meta: GardenMetaEntity?,
    val plants: List<PlantStateEntity>,
    val alivePlantCount: Int,
    val witheredPlants: List<PlantStateEntity>
)

/**
 * 花园状态总仓库
 * 组合 meta + plants 提供统一观察
 */
@Singleton
class GardenRepository @Inject constructor(
    private val gardenMetaDao: GardenMetaDao,
    private val plantStateDao: PlantStateDao
) {

    /**
     * 观察花园整体状态
     * 组合 meta + 所有植物 + 存活数 + 枯萎列表
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeGardenState(): Flow<GardenState> {
        return combine(
            gardenMetaDao.getMeta(),
            plantStateDao.getAllPlants(),
            plantStateDao.getAlivePlantCount(),
            plantStateDao.getWitheredPlants()
        ) { meta, plants, aliveCount, withered ->
            GardenState(
                meta = meta,
                plants = plants,
                alivePlantCount = aliveCount,
                witheredPlants = withered
            )
        }
    }

    /**
     * 观察花园元数据
     */
    fun observeMeta(): Flow<GardenMetaEntity?> {
        return gardenMetaDao.getMeta()
    }

    /**
     * 初始化花园元数据（首次安装时调用）
     */
    suspend fun initMeta(installDate: String) {
        val existing = gardenMetaDao.getMeta()
        // 如果已存在则跳过
        // 注意：这里用 Flow 无法直接判断，需要在协程中收集
        // 实际调用处应先判断
        gardenMetaDao.insertMeta(
            GardenMetaEntity(
                id = 1,
                installDate = installDate,
                accumulatedMinutes = 0,
                streakDays = 0,
                maxStreakDays = 0,
                nightReadDays = 0,
                booksRead = 0,
                currentWeather = "CLEAR",
                weatherDate = null,
                lastSyncDate = null,
                hasRevivedFromDead = false,
                wereadToken = null,
                syncHour = 8,
                syncMinute = 0
            )
        )
    }

    /**
     * 更新花园元数据
     */
    suspend fun updateMeta(entity: GardenMetaEntity) {
        gardenMetaDao.updateMeta(entity)
    }

    /**
     * 触发同步（由 WereadRepository 调用后更新本地数据）
     * 此方法作为外部同步完成的回调入口
     */
    suspend fun triggerSync() {
        // 同步逻辑由 WereadRepository 驱动
        // 此处可触发天气刷新等花园内部逻辑
        // 具体实现后续补充
    }

    /**
     * 更新天气
     */
    suspend fun updateWeather(weather: String, date: String) {
        gardenMetaDao.updateWeather(weather, date)
    }

    /**
     * 更新连续天数
     */
    suspend fun updateStreakDays(streak: Int, maxStreak: Int) {
        gardenMetaDao.updateStreakDays(streak, maxStreak)
    }
}
