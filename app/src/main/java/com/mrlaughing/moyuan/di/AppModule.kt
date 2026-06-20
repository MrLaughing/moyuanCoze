package com.mrlaughing.moyuan.di

import android.content.Context
import androidx.room.Room
import com.mrlaughing.moyuan.data.local.db.MoyuanDatabase
import com.mrlaughing.moyuan.data.local.db.dao.BaseSnapshotDao
import com.mrlaughing.moyuan.data.local.db.dao.BookTrackingDao
import com.mrlaughing.moyuan.data.local.db.dao.DailyRecordDao
import com.mrlaughing.moyuan.data.local.db.dao.GardenMetaDao
import com.mrlaughing.moyuan.data.local.db.dao.PlantStateDao
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.remote.WereadApi
import com.mrlaughing.moyuan.data.remote.WereadApiClient
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.ReadStatsRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块
 *
 * 提供：
 * - Room Database 及所有 DAO
 * - UserPrefs (DataStore)
 * - WereadApi (Retrofit)
 * - 所有 Repository（Hilt 自动注入，WereadApiClient 已有 @Singleton @Inject）
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Room Database ──────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoyuanDatabase {
        return Room.databaseBuilder(
            context,
            MoyuanDatabase::class.java,
            "moyuan.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBaseSnapshotDao(db: MoyuanDatabase): BaseSnapshotDao = db.baseSnapshotDao()

    @Provides
    fun provideDailyRecordDao(db: MoyuanDatabase): DailyRecordDao = db.dailyRecordDao()

    @Provides
    fun providePlantStateDao(db: MoyuanDatabase): PlantStateDao = db.plantStateDao()

    @Provides
    fun provideGardenMetaDao(db: MoyuanDatabase): GardenMetaDao = db.gardenMetaDao()

    @Provides
    fun provideBookTrackingDao(db: MoyuanDatabase): BookTrackingDao = db.bookTrackingDao()

    // ─── UserPrefs ──────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUserPrefs(@ApplicationContext context: Context): UserPrefs {
        return UserPrefs(context)
    }

    // ─── WereadApi ──────────────────────────────────────────

    @Provides
    @Singleton
    fun provideWereadApi(client: WereadApiClient): WereadApi {
        return client.wereadApi
    }
}
