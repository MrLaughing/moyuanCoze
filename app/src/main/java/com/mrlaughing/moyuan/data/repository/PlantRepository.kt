package com.mrlaughing.moyuan.data.repository

import com.mrlaughing.moyuan.data.local.db.dao.PlantStateDao
import com.mrlaughing.moyuan.data.local.db.MoyuanDatabase
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

/**
 * 植物状态管理仓库
 */
@Singleton
class PlantRepository @Inject constructor(
    private val plantStateDao: PlantStateDao,
    private val database: MoyuanDatabase
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
     *
     * 注意：新植物（id=0，尚未在 DB 中）需走 insert；已存在植物（id>0）走 update。
     * 仅用 @Update 会导致新增植物永远无法写入 DB（见 [ensureAllPlantsInitialized]）。
     */
    suspend fun updatePlantAfterRecalculate(results: List<PlantStateEntity>) {
        results.forEach { entity ->
            if (entity.id != 0) {
                plantStateDao.updatePlant(entity)
            } else {
                plantStateDao.insertPlant(entity)
            }
        }
    }

    /**
     * 补种缺失植物：确保 PlantDefinitions 中的所有定义都已存在于 DB。
     * 仅插入 DB 中不存在的植物（锁定状态），不动已有植物的解锁/花园状态。
     * 每次启动调用，兼容"后续版本新增植物定义"的场景。
     */
    suspend fun ensureAllPlantsInitialized() {
        val existingIds = plantStateDao.getAllPlantIds().toSet()
        PlantDefinitions.all.forEach { def ->
            if (def.id !in existingIds) {
                plantStateDao.insertPlant(
                    PlantStateEntity(plantId = def.id, unlockDate = null)
                )
            }
        }
    }

    /**
     * 保证新用户无需授权或联网也能立即看到花园。
     * 仅当当前没有任何已解锁植物时随机赠予 3 株，并按顺序放入自定义花园。
     */
    suspend fun ensureInitialPlantsUnlocked() {
        val plants = plantStateDao.getAllPlants().first()
        if (plants.any { !it.unlockDate.isNullOrEmpty() }) return

        val today = LocalDate.now().toString()
        plants.shuffled().take(3).forEachIndexed { index, plant ->
            plantStateDao.updatePlant(
                plant.copy(
                    unlockDate = today,
                    isInGarden = true,
                    gardenOrder = index + 1
                )
            )
        }
    }

    /**
     * 观察已放入花园（自定义模式）的植物
     */
    fun observePlantsInGarden(): Flow<List<PlantStateEntity>> {
        return plantStateDao.getPlantsInGarden()
    }

    /**
     * 切换植物是否放入花园（自定义摆放）
     * 保留其它字段，仅更新 isInGarden
     */
    suspend fun updateGardenStatus(plantId: String, isInGarden: Boolean): GardenPlacementResult {
        return database.withTransaction {
            val plant = plantStateDao.getPlantById(plantId).firstOrNull()
                ?: return@withTransaction GardenPlacementResult.NOT_FOUND
            if (plant.unlockDate.isNullOrEmpty()) {
                return@withTransaction GardenPlacementResult.LOCKED
            }

            val gardenPlants = plantStateDao.getPlantsInGardenOnce()
            if (isInGarden && gardenPlants.size >= MAX_GARDEN_CAPACITY) {
                return@withTransaction GardenPlacementResult.FULL
            }
            val nextOrder = if (isInGarden) {
                val usedOrders = gardenPlants.map { it.gardenOrder }.toSet()
                (1..MAX_GARDEN_CAPACITY).firstOrNull { it !in usedOrders }
                    ?: return@withTransaction GardenPlacementResult.FULL
            } else 0
            plantStateDao.updatePlant(
                plant.copy(isInGarden = isInGarden, gardenOrder = nextOrder)
            )
            if (isInGarden) GardenPlacementResult.ADDED else GardenPlacementResult.REMOVED
        }
    }

    suspend fun movePlantToSlot(plantId: String, targetSlot: Int): Boolean {
        if (targetSlot !in 0 until MAX_GARDEN_CAPACITY) return false
        return database.withTransaction {
            val plants = plantStateDao.getPlantsInGardenOnce()
            val moving = plants.firstOrNull { it.plantId == plantId }
                ?: return@withTransaction false
            val targetOrder = targetSlot + 1
            if (moving.gardenOrder == targetOrder) return@withTransaction true

            plants.firstOrNull { it.gardenOrder == targetOrder }?.let { occupied ->
                plantStateDao.updatePlant(occupied.copy(gardenOrder = moving.gardenOrder))
            }
            plantStateDao.updatePlant(moving.copy(gardenOrder = targetOrder))
            true
        }
    }

    private companion object {
        const val MAX_GARDEN_CAPACITY = 49
    }
}

enum class GardenPlacementResult { ADDED, REMOVED, FULL, LOCKED, NOT_FOUND }
