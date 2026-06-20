package com.mrlaughing.moyuan

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mrlaughing.moyuan.data.local.db.entity.GardenMetaEntity
import com.mrlaughing.moyuan.data.local.db.entity.PlantStateEntity
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.render.EinkHelper
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 墨园 Application
 * 
 * 职责：
 * - Hilt 入口点配置
 * - 首次启动花园初始化
 * - 全局墨水屏辅助初始化
 */
@HiltAndroidApp
class MoyuanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 全局初始化：墨水屏辅助禁用动画
        EinkHelper.disableAnimations(this)
        
        // 首次启动初始化
        initFirstLaunch()
    }

    /**
     * 首次启动初始化
     * 
     * 检查 firstLaunch 标志，如果是首次启动：
     * 1. 初始化花园元数据
     * 2. 初始化所有 27 种植物的锁定状态
     * 3. 设置 firstLaunch = false
     */
    private fun initFirstLaunch() {
        try {
            // 通过 EntryPoint 获取依赖
            val entryPoint = EntryPointAccessors.fromApplication(
                this, MoyuanInitEntryPoint::class.java
            )
            val gardenRepository = entryPoint.gardenRepository()
            val plantRepository = entryPoint.plantRepository()
            val userPrefs = entryPoint.userPrefs()

            // 在 Application 中启动协程
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    owner.lifecycleScope.launch {
                        try {
                            // 检查是否首次启动
                            val isFirst = userPrefs.firstLaunch.first()
                            if (isFirst) {
                                performFirstLaunchInit(
                                    gardenRepository = gardenRepository,
                                    plantRepository = plantRepository,
                                    userPrefs = userPrefs
                                )
                            }
                        } catch (e: Exception) {
                            // 初始化失败不应崩溃，下次启动还会重试
                            android.util.Log.e("MoyuanApp", "First launch init failed", e)
                        }
                    }
                    // 只执行一次，移除监听
                    owner.lifecycle.removeObserver(this)
                }
            })
        } catch (e: Exception) {
            // EntryPoint 获取失败不应崩溃
            android.util.Log.e("MoyuanApp", "initFirstLaunch EntryPoint failed", e)
        }
    }

    /**
     * 执行首次启动初始化
     */
    private suspend fun performFirstLaunchInit(
        gardenRepository: GardenRepository,
        plantRepository: PlantRepository,
        userPrefs: UserPrefs
    ) {
        val today = LocalDate.now().toString()

        // 1. 检查是否已有元数据
        val existingMeta = gardenRepository.observeMeta().first()
        if (existingMeta == null) {
            // 初始化花园元数据
            gardenRepository.initMeta(today)
        }

        // 2. 检查是否已有植物数据
        val existingPlants = plantRepository.observePlants().first()
        if (existingPlants.isEmpty()) {
            // 初始化所有 27 种植物的锁定状态
            PlantDefinitions.all.forEach { def ->
                plantRepository.insertPlant(
                    PlantStateEntity(
                        plantId = def.id,
                        path = def.path.name,
                        level = 1,
                        accumulatedMinutes = 0,
                        witherStage = 0,
                        witherStartDate = null,
                        lastReadDate = today,
                        unlockDate = null, // 未解锁
                        justRevived = false,
                        reviveDate = null
                    )
                )
            }
        }

        // 3. 设置首次启动完成
        userPrefs.setFirstLaunch(false)
    }

    companion object {
        @Volatile
        private lateinit var instance: MoyuanApp

        fun getInstance(): MoyuanApp = instance
    }
}

/**
 * Hilt 入口点接口（必须为顶层，KSP 不支持嵌套 @EntryPoint）
 * 用于在 Application 中访问 Hilt 管理的依赖
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MoyuanInitEntryPoint {
    fun gardenRepository(): GardenRepository
    fun plantRepository(): PlantRepository
    fun userPrefs(): UserPrefs
}
