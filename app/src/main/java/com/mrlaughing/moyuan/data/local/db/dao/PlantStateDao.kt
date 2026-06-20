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
     * 获取非枯寂（witherStage < 4）的植物数量
     */
    @Query("SELECT COUNT(*) FROM plant_state WHERE witherStage < 4")
    fun getAlivePlantCount(): Flow<Int>

    /**
     * 获取枯萎中的植物（witherStage > 0）
     */
    @Query("SELECT * FROM plant_state WHERE witherStage > 0 ORDER BY witherStartDate ASC")
    fun getWitheredPlants(): Flow<List<PlantStateEntity>>

    /**
     * 获取枯寂（witherStage = 4）的植物
     */
    @Query("SELECT * FROM plant_state WHERE witherStage = 4")
    fun getDeadPlants(): Flow<List<PlantStateEntity>>

    /**
     * 按路径查询植物
     */
    @Query("SELECT * FROM plant_state WHERE path = :path ORDER BY unlockDate ASC")
    fun getPlantsByPath(path: String): Flow<List<PlantStateEntity>>
}
