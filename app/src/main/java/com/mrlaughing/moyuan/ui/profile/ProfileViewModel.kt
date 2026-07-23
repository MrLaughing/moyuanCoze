package com.mrlaughing.moyuan.ui.profile

import android.app.Application
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
import com.mrlaughing.moyuan.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * 个人中心 ViewModel
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class ProfileViewModel @Inject constructor(
    private val wereadRepository: WereadRepository,
    private val gardenRepository: GardenRepository,
    private val userPrefs: UserPrefs,
    private val achievementDao: AchievementDao,
    private val gardenMetaDao: GardenMetaDao,
    private val plantStateDao: PlantStateDao,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()

    private val _unlockedCount = MutableStateFlow(0)
    val unlockedCount: StateFlow<Int> = _unlockedCount.asStateFlow()

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

    private fun observeAchievements() {
        viewModelScope.launch {
            // 按定义顺序排序（ALL_ACHIEVEMENTS 的声明顺序即展示顺序），而非 SQL 的随机顺序
            val orderMap = AchievementDefinitions.ALL_ACHIEVEMENTS
                .mapIndexed { index, def -> def.id to index }
                .toMap()
            achievementDao.getAllAchievements()
                .distinctUntilChanged()
                .debounce(150)
                .collect { list ->
                    _achievements.value = list.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
                }
        }
        viewModelScope.launch {
            achievementDao.getUnlockedCount()
                .distinctUntilChanged()
                .debounce(150)
                .collect { count ->
                    _unlockedCount.value = count
                }
        }
    }

    private fun initializeAchievementsIfNeeded() {
        viewModelScope.launch {
            val existing = achievementDao.getAllAchievements().first()
            val existingIds = existing.map { it.id }.toSet()
            val allIds = AchievementDefinitions.ALL_ACHIEVEMENTS.map { it.id }
            // 仅补种缺失的成就定义（兼容旧版本已有数据，新增如 garden_full 等）
            val missing = AchievementDefinitions.ALL_ACHIEVEMENTS.filter { it.id !in existingIds }
            if (missing.isNotEmpty()) {
                achievementDao.insertAll(
                    missing.map { def ->
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
                )
            }
            // 清理旧版本遗留、已不在定义中的成就（如 reach_lv5）
            achievementDao.deleteExcept(allIds)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            combine(
                gardenRepository.observeGardenState(),
                userPrefs.syncHour,
                userPrefs.syncMinute,
                userPrefs.wereadToken
            ) { gardenState, syncHour, syncMinute, token ->
                val unlockedCount = gardenState.plants.count {
                    !it.unlockDate.isNullOrEmpty()
                }
                val meta = gardenState.meta

                ProfileUiState(
                    gardenName = "墨园",
                    plantCount = gardenState.alivePlantCount,
                    unlockedCount = unlockedCount,
                    totalCount = PlantDefinitions.all.size,
                    wereadAuthorized = !token.isNullOrBlank(),
                    lastSyncTime = meta?.lastSyncDate ?: "从未同步",
                    syncHour = syncHour,
                    syncMinute = syncMinute
                )
            }
            .distinctUntilChanged()
            .debounce(150) // 防抖：避免同步时DB连续写入导致UI频繁刷新
            .collect { state ->
                _uiState.value = state
                checkAndUnlockAchievements()
            }
        }
    }

    private fun checkAndUnlockAchievements() {
        viewModelScope.launch {
            val meta = gardenMetaDao.getMeta().first() ?: return@launch
            val plants = plantStateDao.getAllPlants().first()
            val unlockedPlants = plants.filter { !it.unlockDate.isNullOrEmpty() }
            val unlockedCount = unlockedPlants.size
            val gardenCount = plants.count { it.isInGarden }
            // first_sync 应表示"已完成一次成功同步"，以 lastSyncDate 非空为准，
            // 而非仅检查授权 Token（输入 Key 但未同步不应解锁）
            val hasSynced = meta.lastSyncDate != null
            val booksRead = meta.booksRead
            val readingHours = meta.accumulatedMinutes / 60

            checkAndUpdateAchievement("first_sync", if (hasSynced) 1 else 0)
            checkAndUpdateAchievement("read_10_books", booksRead)
            checkAndUpdateAchievement("read_50_books", booksRead)
            checkAndUpdateAchievement("night_read_30", meta.nightReadDays)
            checkAndUpdateAchievement("read_100_hours", readingHours)
            checkAndUpdateAchievement("read_500_hours", readingHours)
            checkAndUpdateAchievement("first_sprout", if (unlockedCount >= 1) 1 else 0)
            checkAndUpdateAchievement("unlock_10", unlockedCount)
            checkAndUpdateAchievement("unlock_all", unlockedCount)
            checkAndUpdateAchievement("garden_full", gardenCount)
            checkAndUpdateAchievement("streak_7", meta.streakDays)
            checkAndUpdateAchievement("streak_30", meta.streakDays)
        }
    }

    private suspend fun checkAndUpdateAchievement(id: String, currentValue: Int) {
        val achievement = achievementDao.getAchievementById(id) ?: return
        if (achievement.isUnlocked) {
            // 已解锁：继续更新当前进度（如阅读时长持续增长），但保持解锁状态与解锁日期不变
            if (achievement.currentValue != currentValue) {
                achievementDao.updateProgress(
                    id = id,
                    current = currentValue,
                    unlocked = true,
                    date = achievement.unlockedDate
                )
            }
            return
        }
        val isUnlocked = currentValue >= achievement.targetValue
        val unlockedDate = if (isUnlocked) dateFormat.format(Date()) else null
        achievementDao.updateProgress(
            id = id,
            current = currentValue.coerceAtMost(achievement.targetValue),
            unlocked = isUnlocked,
            date = unlockedDate
        )
    }

    fun getAchievementsByCategory(category: String): List<AchievementEntity> {
        return if (category == AchievementDefinitions.CATEGORY_ALL) {
            _achievements.value
        } else {
            _achievements.value.filter { it.category == category }
        }
    }

    fun updateSyncTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPrefs.setSyncHour(hour)
            userPrefs.setSyncMinute(minute)
            gardenMetaDao.getMeta().first()?.let { meta ->
                gardenMetaDao.updateMeta(meta.copy(syncHour = hour, syncMinute = minute))
            }
            if (wereadRepository.isAuthorized()) {
                SyncScheduler.scheduleDailySync(application, hour, minute)
            }
        }
    }

    suspend fun authorize(token: String): UUID {
        wereadRepository.authorize(token)
        val hour = userPrefs.syncHour.first()
        val minute = userPrefs.syncMinute.first()
        SyncScheduler.scheduleDailySync(application, hour, minute)
        return SyncScheduler.enqueueImmediateSync(application, replaceRunning = true)
    }
    fun deauthorize() {
        viewModelScope.launch {
            wereadRepository.deauthorize()
            SyncScheduler.cancelDailySync(application)
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
    val syncMinute: Int = 0
)
