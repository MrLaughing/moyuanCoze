package com.mrlaughing.moyuan.ui.profile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
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
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            combine(
                gardenRepository.observeGardenState(),
                userPrefs.wereadToken
            ) { gardenState, token ->
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
                    lastSyncTime = meta?.lastSyncDate ?: "从未同步"
                )
            }
            .distinctUntilChanged()
            .debounce(150) // 防抖：避免同步时DB连续写入导致UI频繁刷新
            .collect { state ->
                _uiState.value = state
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
    val lastSyncTime: String = ""
)
