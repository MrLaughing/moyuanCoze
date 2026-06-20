package com.mrlaughing.moyuan.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 每日阅读记录 DAO
 */
@Dao
interface DailyRecordDao {

    /**
     * 按日期查询记录
     */
    @Query("SELECT * FROM daily_record WHERE date = :date")
    fun getRecordByDate(date: String): Flow<DailyRecordEntity?>

    /**
     * 获取所有记录
     */
    @Query("SELECT * FROM daily_record ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyRecordEntity>>

    /**
     * 获取日期范围内的记录
     */
    @Query("SELECT * FROM daily_record WHERE date >= :start AND date <= :end ORDER BY date ASC")
    fun getRecordsBetween(start: String, end: String): Flow<List<DailyRecordEntity>>

    /**
     * 插入记录，冲突时替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(entity: DailyRecordEntity)

    /**
     * 更新记录
     */
    @Update
    suspend fun updateRecord(entity: DailyRecordEntity)

    /**
     * 删除记录
     */
    @Query("DELETE FROM daily_record WHERE id = :id")
    suspend fun deleteRecord(id: Int)

    /**
     * 统计有夜读的日期数
     */
    @Query("SELECT COUNT(*) FROM daily_record WHERE hasNightRead = 1")
    fun getNightReadDayCount(): Flow<Int>

    /**
     * 获取最近N天的记录
     */
    @Query("SELECT * FROM daily_record ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<DailyRecordEntity>>

    /**
     * 获取总阅读分钟数
     */
    @Query("SELECT COALESCE(SUM(readMinutes), 0) FROM daily_record")
    fun getTotalReadMinutes(): Flow<Int>
}
