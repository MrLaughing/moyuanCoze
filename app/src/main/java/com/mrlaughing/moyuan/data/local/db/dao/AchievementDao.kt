package com.mrlaughing.moyuan.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrlaughing.moyuan.data.local.db.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * 成就 DAO
 */
@Dao
interface AchievementDao {
    
    /**
     * 获取所有成就，按分类和已解锁状态排序
     */
    @Query("SELECT * FROM achievement ORDER BY category, isUnlocked DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>
    
    /**
     * 按分类获取成就
     */
    @Query("SELECT * FROM achievement WHERE category = :category ORDER BY isUnlocked DESC")
    fun getAchievementsByCategory(category: String): Flow<List<AchievementEntity>>
    
    /**
     * 获取已解锁成就数量
     */
    @Query("SELECT COUNT(*) FROM achievement WHERE isUnlocked = 1")
    fun getUnlockedCount(): Flow<Int>
    
    /**
     * 获取单个成就
     */
    @Query("SELECT * FROM achievement WHERE id = :id")
    suspend fun getAchievementById(id: String): AchievementEntity?
    
    /**
     * 批量插入成就
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)
    
    /**
     * 更新成就
     */
    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)
    
    /**
     * 更新成就进度
     */
    @Query("UPDATE achievement SET currentValue = :current, isUnlocked = :unlocked, unlockedDate = :date WHERE id = :id")
    suspend fun updateProgress(id: String, current: Int, unlocked: Boolean, date: String?)
    
    /**
     * 检查成就表是否为空（用于首次初始化）
     */
    @Query("SELECT COUNT(*) FROM achievement")
    suspend fun getCount(): Int

    /**
     * 删除不在给定 id 集合内的成就（清理旧版本遗留定义，如 reach_lv5）
     */
    @Query("DELETE FROM achievement WHERE id NOT IN (:ids)")
    suspend fun deleteExcept(ids: List<String>)
}
