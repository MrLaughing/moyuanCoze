package com.mrlaughing.moyuan.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 植物状态 DAO
 */
@Dao
interface PlantStateDao {

    /**
     * 获取所有植物
     */
    @Query("SELECT * FROM plant_state ORDER BY unlockDate ASC")
    fun getAllPlants(): Flow<List<PlantStateEntity>>

    /**
     * 获取所有已存在植物的 plantId 列表（用于补种缺失定义）
     */
    @Query("SELECT plantId FROM plant_state")
    suspend fun getAllPlantIds(): List<String>

    /**
     * 按plantId查询植物
     */
    @Query("SELECT * FROM plant_state WHERE plantId = :plantId")
    fun getPlantById(plantId: String): Flow<PlantStateEntity?>

    /**
     * 插入植物记录，冲突时替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(entity: PlantStateEntity)

    /**
     * 更新植物记录
     */
    @Update
    suspend fun updatePlant(entity: PlantStateEntity)

    /**
     * 删除植物记录
     */
    @Query("DELETE FROM plant_state WHERE plantId = :plantId")
    suspend fun deletePlant(plantId: String)

    /**
     * 获取已解锁植物数量（unlockDate 不为空）
     */
    @Query("SELECT COUNT(*) FROM plant_state WHERE unlockDate IS NOT NULL")
    fun getAlivePlantCount(): Flow<Int>

    /**
     * 查询已放入花园的植物（自定义模式），按 gardenOrder 排序
     */
    @Query("SELECT * FROM plant_state WHERE isInGarden = 1 ORDER BY gardenOrder ASC")
    fun getPlantsInGarden(): Flow<List<PlantStateEntity>>

    @Query("SELECT * FROM plant_state WHERE isInGarden = 1 ORDER BY gardenOrder ASC")
    suspend fun getPlantsInGardenOnce(): List<PlantStateEntity>
}
