package com.mrlaughing.moyuan.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrlaughing.moyuan.data.local.db.entity.BaseSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * 安装时刻基准快照 DAO
 */
@Dao
interface BaseSnapshotDao {

    /**
     * 观察基准快照（单例）
     */
    @Query("SELECT * FROM base_snapshot WHERE id = 1")
    fun getSnapshot(): Flow<BaseSnapshotEntity?>

    /**
     * 插入基准快照，冲突时替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(entity: BaseSnapshotEntity)

    /**
     * 删除基准快照
     */
    @Query("DELETE FROM base_snapshot WHERE id = 1")
    suspend fun deleteSnapshot()
}
