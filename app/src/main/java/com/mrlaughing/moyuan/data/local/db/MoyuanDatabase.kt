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
    version = 4,
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
