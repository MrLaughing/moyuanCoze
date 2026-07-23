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
 * - v7: 植物状态新增 isInGarden / gardenOrder，支持"放入花园"自定义摆放
 * - v8: 移除 1.0 废弃字段（path/level/accumulatedMinutes/witherStage/witherStartDate/lastReadDate/justRevived/reviveDate），重建 plant_state
 * - v9: garden_meta 新增 lastNightReadDate（夜读按天去重）
 * - v10: garden_meta 新增 totalReadDays（持久化累计总阅读天数，权威值来自微信读书 API）
 * - v11: garden_meta 新增 lastReadDate，并清理历史明文微信读书 Token
 * - v12: 从数据库结构中彻底移除废弃的 wereadToken 列
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
    version = 12,
    exportSchema = true
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

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_record ADD COLUMN source TEXT NOT NULL DEFAULT 'sync'")
                db.execSQL("ALTER TABLE daily_record ADD COLUMN weather TEXT")
            }
        }

        /**
         * v6 → v7：plant_state 新增 isInGarden / gardenOrder 两列
         */
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plant_state ADD COLUMN isInGarden INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plant_state ADD COLUMN gardenOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v7 → v8：重建 plant_state，移除废弃字段，保留解锁状态与花园摆放状态
         */
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE plant_state_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plantId TEXT NOT NULL,
                        unlockDate TEXT,
                        isInGarden INTEGER NOT NULL DEFAULT 0,
                        gardenOrder INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """INSERT INTO plant_state_new (id, plantId, unlockDate, isInGarden, gardenOrder)
                        SELECT id, plantId, unlockDate, isInGarden, gardenOrder FROM plant_state"""
                )
                db.execSQL("DROP TABLE plant_state")
                db.execSQL("ALTER TABLE plant_state_new RENAME TO plant_state")
                db.execSQL("CREATE UNIQUE INDEX index_plant_state_plantId ON plant_state (plantId)")
            }
        }

        /**
         * v8 → v9：garden_meta 新增 lastNightReadDate，避免夜间多次同步导致夜读天数重复计数
         */
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE garden_meta ADD COLUMN lastNightReadDate TEXT")
            }
        }

        /**
         * v9 → v10：garden_meta 新增 totalReadDays，持久化累计总阅读天数
         */
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE garden_meta ADD COLUMN totalReadDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v10 -> v11: 分离最后阅读日与最后同步日，并清理旧版数据库中的明文 Token。
         */
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE garden_meta ADD COLUMN lastReadDate TEXT")
                db.execSQL("UPDATE garden_meta SET wereadToken = NULL")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE garden_meta_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        installDate TEXT NOT NULL,
                        accumulatedMinutes INTEGER NOT NULL,
                        streakDays INTEGER NOT NULL,
                        maxStreakDays INTEGER NOT NULL,
                        nightReadDays INTEGER NOT NULL,
                        booksRead INTEGER NOT NULL,
                        currentWeather TEXT NOT NULL,
                        weatherDate TEXT,
                        lastSyncDate TEXT,
                        lastReadDate TEXT,
                        lastNightReadDate TEXT,
                        syncHour INTEGER NOT NULL,
                        syncMinute INTEGER NOT NULL,
                        todayReadMinutes INTEGER NOT NULL,
                        totalReadDays INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """INSERT INTO garden_meta_new (
                        id, installDate, accumulatedMinutes, streakDays, maxStreakDays,
                        nightReadDays, booksRead, currentWeather, weatherDate, lastSyncDate,
                        lastReadDate, lastNightReadDate, syncHour, syncMinute,
                        todayReadMinutes, totalReadDays
                    ) SELECT
                        id, installDate, accumulatedMinutes, streakDays, maxStreakDays,
                        nightReadDays, booksRead, currentWeather, weatherDate, lastSyncDate,
                        lastReadDate, lastNightReadDate, syncHour, syncMinute,
                        todayReadMinutes, totalReadDays
                    FROM garden_meta"""
                )
                db.execSQL("DROP TABLE garden_meta")
                db.execSQL("ALTER TABLE garden_meta_new RENAME TO garden_meta")
            }
        }
    }
}
