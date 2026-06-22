package com.mrlaughing.moyuan.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mrlaughing.moyuan.data.local.db.dao.BaseSnapshotDao
import com.mrlaughing.moyuan.data.local.db.dao.BookTrackingDao
import com.mrlaughing.moyuan.data.local.db.dao.DailyRecordDao
import com.mrlaughing.moyuan.data.local.db.dao.GardenMetaDao
import com.mrlaughing.moyuan.data.local.db.dao.PlantStateDao
import com.mrlaughing.moyuan.data.local.db.dao.AchievementDao
import com.mrlaughing.moyuan.data.local.db.entity.BaseSnapshotEntity
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import com.mrlaughing.moyuan.data.local.db.entity.DailyRecordEntity
import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import com.mrlaughing.moyuan.data.local.db.entity.AchievementEntity

/**
 * 墨园 Room Database
 * 
 * Version History:
 * - v1-v3: 初始版本
 * - v4: 植物素材替换（梅→山茶/古藤→万年青/灵芝→忘忧草/并蒂莲→连理枝）
 * - v5: 首次引入（当前版本号）
 * - v6: 每日记录新增 source 和 weather 字段，支持天气精确补算
 */
@Database(
    entities = [
        BaseSnapshotEntity::class,
        DailyRecordEntity::class,
        PlantStateEntity::class,
        GardenMetaEntity::class,
        BookTrackingEntity::class,
        AchievementEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(MoyuanTypeConverters::class)
abstract class MoyuanDatabase : RoomDatabase() {

    abstract fun baseSnapshotDao(): BaseSnapshotDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun plantStateDao(): PlantStateDao
    abstract fun gardenMetaDao(): GardenMetaDao
    abstract fun bookTrackingDao(): BookTrackingDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val DATABASE_NAME = "moyuan.db"
    }
}
