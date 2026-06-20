package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.local.db.dao.PlantStateDao
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 植物状态管理仓库
 */
@Singleton
class PlantRepository @Inject constructor(
    private val plantStateDao: PlantStateDao
) {

    /**
     * 观察所有植物
     */
    fun observePlants(): Flow<List<PlantStateEntity>> {
        return plantStateDao.getAllPlants()
    }

    /**
     * 观察单棵植物
     */
    fun observePlant(plantId: String): Flow<PlantStateEntity?> {
        return plantStateDao.getPlantById(plantId)
    }

    /**
     * 观察存活植物数量
     */
    fun observeAlivePlantCount(): Flow<Int> {
        return plantStateDao.getAlivePlantCount()
    }

    /**
     * 观察枯萎植物列表
     */
    fun observeWitheredPlants(): Flow<List<PlantStateEntity>> {
        return plantStateDao.getWitheredPlants()
    }

    /**
     * 插入新植物
     */
    suspend fun insertPlant(entity: PlantStateEntity) {
        plantStateDao.insertPlant(entity)
    }

    /**
     * 更新植物状态
     */
    suspend fun updatePlant(entity: PlantStateEntity) {
        plantStateDao.updatePlant(entity)
    }

    /**
     * 重新计算后批量更新植物状态
     * @param results 计算后的植物状态列表
     */
    suspend fun updatePlantAfterRecalculate(results: List<PlantStateEntity>) {
        results.forEach { entity ->
            plantStateDao.updatePlant(entity)
        }
    }

    /**
     * 解锁新植物
     */
    suspend fun unlockPlant(plantId: String, path: String, unlockDate: String) {
        plantStateDao.insertPlant(
            PlantStateEntity(
                plantId = plantId,
                path = path,
                level = 1,
                accumulatedMinutes = 0,
                witherStage = 0,
                witherStartDate = null,
                lastReadDate = unlockDate,
                unlockDate = unlockDate,
                justRevived = false,
                reviveDate = null
            )
        )
    }

    /**
     * 枯寂复活
     */
    suspend fun revivePlant(plantId: String, reviveDate: String) {
        plantStateDao.getPlantById(plantId).firstOrNull()?.let { plant ->
            plantStateDao.updatePlant(
                plant.copy(
                    level = 1,
                    witherStage = 0,
                    witherStartDate = null,
                    justRevived = true,
                    reviveDate = reviveDate,
                    lastReadDate = reviveDate,
                    accumulatedMinutes = 0
                )
            )
        }
    }

    /**
     * 按路径观察植物
     */
    fun observePlantsByPath(path: String): Flow<List<PlantStateEntity>> {
        return plantStateDao.getPlantsByPath(path)
    }
}
