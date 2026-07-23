package com.mrlaughing.moyuan.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import kotlinx.coroutines.flow.Flow

/**
 * 花园元数据 DAO
 */
@Dao
interface GardenMetaDao {

    /**
     * 观察花园元数据（单例）
     */
    @Query("SELECT * FROM garden_meta WHERE id = 1")
    fun getMeta(): Flow<GardenMetaEntity?>

    /**
     * 插入元数据，冲突时替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(entity: GardenMetaEntity)

    /**
     * 更新元数据
     */
    @Update
    suspend fun updateMeta(entity: GardenMetaEntity)

    /**
     * 删除元数据
     */
    @Query("DELETE FROM garden_meta WHERE id = 1")
    suspend fun deleteMeta()

    /**
     * 更新同步时间
     */
    @Query("UPDATE garden_meta SET syncHour = :hour, syncMinute = :minute WHERE id = 1")
    suspend fun updateSyncTime(hour: Int, minute: Int)

    /**
     * 更新天气
     */
    @Query("UPDATE garden_meta SET currentWeather = :weather, weatherDate = :date WHERE id = 1")
    suspend fun updateWeather(weather: String, date: String)

    /**
     * 更新连续天数
     */
    @Query("UPDATE garden_meta SET streakDays = :streak, maxStreakDays = :maxStreak WHERE id = 1")
    suspend fun updateStreakDays(streak: Int, maxStreak: Int)
}
