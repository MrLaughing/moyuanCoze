package com.mrlaughing.moyuan.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.local.db.dao.AchievementDao
import com.mrlaughing.moyuan.data.local.db.dao.GardenMetaDao
import com.mrlaughing.moyuan.data.local.db.dao.PlantStateDao
import com.mrlaughing.moyuan.data.local.db.entity.AchievementEntity
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.model.AchievementDefinitions
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 个人中心 ViewModel
 *
 * 从 Repository 加载真实数据：
 * - WereadRepository.isAuthorized() 获取授权状态
 * - GardenRepository.observeGardenState() 获取花园信息
 * - UserPrefs 获取同步配置
 * - AchievementDao 获取成就数据
 *
 * 已解锁植物数：unlockDate != null 的植物数量
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val wereadRepository: WereadRepository,
    private val gardenRepository: GardenRepository,
    private val userPrefs: UserPrefs,
    private val achievementDao: AchievementDao,
    private val gardenMetaDao: GardenMetaDao,
    private val plantStateDao: PlantStateDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // 成就数据流
    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()

    // 已解锁成就数量
    private val _unlockedCount = MutableStateFlow(0)
    val unlockedCount: StateFlow<Int> = _unlockedCount.asStateFlow()

    // 成就分类
    val achievementCategories = listOf(
        AchievementDefinitions.CATEGORY_ALL,
        AchievementDefinitions.CATEGORY_READING,
        AchievementDefinitions.CATEGORY_GROWTH,
        AchievementDefinitions.CATEGORY_MILESTONE
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadProfile()
        observeAchievements()
        initializeAchievementsIfNeeded()
    }

    /**
     * 观察成就数据
     */
    private fun observeAchievements() {
        viewModelScope.launch {
            achievementDao.getAllAchievements().collect { list ->
                _achievements.value = list
            }
        }
        viewModelScope.launch {
            achievementDao.getUnlockedCount().collect { count ->
                _unlockedCount.value = count
            }
        }
    }

    /**
     * 如果成就表为空，初始化全部成就
     */
    private fun initializeAchievementsIfNeeded() {
        viewModelScope.launch {
            val count = achievementDao.getCount()
            if (count == 0) {
                val achievements = AchievementDefinitions.ALL_ACHIEVEMENTS.map { def ->
                    AchievementEntity(
                        id = def.id,
                        category = def.category,
                        name = def.name,
                        description = def.description,
                        condition = def.condition,
                        targetValue = def.targetValue,
                        currentValue = 0,
                        isUnlocked = false,
                        unlockedDate = null
                    )
                }
                achievementDao.insertAll(achievements)
            }
        }
    }

    /**
     * 从 Repository 加载真实个人数据
     */
    private fun loadProfile() {
        viewModelScope.launch {
            // 组合多个数据流
            combine(
                gardenRepository.observeGardenState(),
                userPrefs.syncHour,
                userPrefs.syncMinute
            ) { gardenState, syncHour, syncMinute ->
                // 获取授权状态
                val isAuthorized = wereadRepository.isAuthorized()

                // 获取已解锁植物数（unlockDate != null）
                val unlockedCount = gardenState.plants.count {
                    !it.unlockDate.isNullOrEmpty()
                }
                
                // 获取枯萎植物数
                val witheredCount = gardenState.witheredPlants.size

                // 获取元数据信息
                val meta = gardenState.meta
                
                // 首次种植日期
                val firstPlantDate = meta?.installDate ?: "无记录"

                ProfileUiState(
                    gardenName = "墨园",
                    plantCount = gardenState.alivePlantCount,
                    unlockedCount = unlockedCount,
                    totalCount = PlantDefinitions.all.size,
                    wereadAuthorized = isAuthorized,
                    lastSyncTime = meta?.lastSyncDate ?: "从未同步",
                    syncHour = syncHour,
                    syncMinute = syncMinute,
                    refreshMode = "局部刷新",
                    firstPlantDate = firstPlantDate,
                    witheredCount = witheredCount
                )
            }.collect { state ->
                _uiState.value = state
                // 加载完成后检查成就
                checkAndUnlockAchievements()
            }
        }
    }

    /**
     * 检查并解锁成就
     */
    private fun checkAndUnlockAchievements() {
        viewModelScope.launch {
            // 获取元数据
            val meta = gardenMetaDao.getMeta().first() ?: return@launch
            
            // 获取植物数据
            val plants = plantStateDao.getAllPlants().first()
            val unlockedPlants = plants.filter { !it.unlockDate.isNullOrEmpty() }
            val unlockedCount = unlockedPlants.size
            
            // 获取最高等级
            val maxLevel = plants.maxOfOrNull { it.level } ?: 0
            
            // 获取是否已授权
            val isAuthorized = wereadRepository.isAuthorized()
            
            // 获取已读书目数
            val booksRead = meta.booksRead
            
            // 获取阅读小时数（分钟转小时）
            val readingHours = meta.accumulatedMinutes / 60

            // 阅读成就检查
            checkAndUpdateAchievement("first_sync", 
                if (isAuthorized) 1 else 0)
            
            checkAndUpdateAchievement("bookworm", booksRead)
            
            checkAndUpdateAchievement("book_collector", booksRead)
            
            checkAndUpdateAchievement("library_master", booksRead)
            
            checkAndUpdateAchievement("night_reader", meta.nightReadDays)
            
            checkAndUpdateAchievement("ink_hours_100", readingHours)
            
            checkAndUpdateAchievement("ink_hours_500", readingHours)

            // 养成成就检查
            checkAndUpdateAchievement("first_sprout", 
                if (unlockedCount >= 1) 1 else 0)
            
            checkAndUpdateAchievement("ink_forest", unlockedCount)
            
            checkAndUpdateAchievement("full_bloom", unlockedCount)
            
            checkAndUpdateAchievement("mohua", 
                if (maxLevel >= 5) 1 else 0)
            
            checkAndUpdateAchievement("revival", 
                if (meta.hasRevivedFromDead) 1 else 0)

            // 里程碑成就检查
            checkAndUpdateAchievement("week_streak", meta.streakDays)
            
            checkAndUpdateAchievement("month_streak", meta.streakDays)
            
            checkAndUpdateAchievement("hundred_streak", meta.maxStreakDays)
        }
    }

    /**
     * 检查并更新单个成就
     */
    private suspend fun checkAndUpdateAchievement(id: String, currentValue: Int) {
        val achievement = achievementDao.getAchievementById(id) ?: return
        
        // 如果已解锁，跳过
        if (achievement.isUnlocked) return
        
        // 判断是否达到目标
        val isUnlocked = currentValue >= achievement.targetValue
        val unlockedDate = if (isUnlocked) dateFormat.format(Date()) else null
        
        // 更新进度
        achievementDao.updateProgress(
            id = id,
            current = currentValue.coerceAtMost(achievement.targetValue),
            unlocked = isUnlocked,
            date = unlockedDate
        )
    }

    /**
     * 获取指定分类的成就
     */
    fun getAchievementsByCategory(category: String): List<AchievementEntity> {
        return if (category == AchievementDefinitions.CATEGORY_ALL) {
            _achievements.value
        } else {
            _achievements.value.filter { it.category == category }
        }
    }

    /**
     * 更新同步时间
     */
    fun updateSyncTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPrefs.setSyncHour(hour)
            userPrefs.setSyncMinute(minute)
            // 同时更新 GardenMeta
            gardenMetaDao.getMeta().first()?.let { meta ->
                gardenMetaDao.updateMeta(meta.copy(syncHour = hour, syncMinute = minute))
            }
        }
    }

    /**
     * 更新刷新模式
     */
    fun updateRefreshMode(mode: String) {
        viewModelScope.launch {
            userPrefs.setRefreshMode(mode)
            _uiState.value = _uiState.value.copy(refreshMode = mode)
        }
    }

    /**
     * 授权微信读书
     */
    fun authorize(token: String) {
        viewModelScope.launch {
            wereadRepository.authorize(token)
            // 重新加载状态
            loadProfile()
        }
    }

    /**
     * 取消授权
     */
    fun deauthorize() {
        viewModelScope.launch {
            wereadRepository.deauthorize()
            // 重新加载状态
            loadProfile()
        }
    }
}

data class ProfileUiState(
    val gardenName: String = "",
    val plantCount: Int = 0,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val wereadAuthorized: Boolean = false,
    val lastSyncTime: String = "",
    val syncHour: Int = 8,
    val syncMinute: Int = 0,
    val refreshMode: String = "局部刷新",
    val firstPlantDate: String = "",
    val witheredCount: Int = 0
)
