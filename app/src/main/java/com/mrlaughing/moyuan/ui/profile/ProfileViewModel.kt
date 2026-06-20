package com.mrlaughing.moyuan.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
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
import javax.inject.Inject

/**
 * 个人中心 ViewModel
 *
 * 从 Repository 加载真实数据：
 * - WereadRepository.isAuthorized() 获取授权状态
 * - GardenRepository.observeGardenState() 获取花园信息
 * - UserPrefs 获取同步配置
 *
 * 已解锁植物数：unlockDate != null 的植物数量
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val wereadRepository: WereadRepository,
    private val gardenRepository: GardenRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
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
            }
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
            gardenRepository.observeMeta().first()?.let { meta ->
                gardenRepository.updateMeta(meta.copy(syncHour = hour, syncMinute = minute))
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
