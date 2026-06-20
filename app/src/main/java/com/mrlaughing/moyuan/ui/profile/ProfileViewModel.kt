package com.mrlaughing.moyuan.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 个人中心 ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    // 注入 GardenRepository, UserPrefs, WereadRepository（待实现）
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            // TODO: 从 Repository 加载
            _uiState.value = ProfileUiState(
                gardenName = "墨园",
                plantCount = 5,
                unlockedCount = 5,
                totalCount = 10,
                wereadAuthorized = true,
                lastSyncTime = "今天 08:00",
                syncHour = 8,
                syncMinute = 0,
                refreshMode = "局部刷新"
            )
        }
    }

    fun updateSyncTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(syncHour = hour, syncMinute = minute)
    }

    fun updateRefreshMode(mode: String) {
        _uiState.value = _uiState.value.copy(refreshMode = mode)
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
    val refreshMode: String = "局部刷新"
)
