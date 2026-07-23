package com.mrlaughing.moyuan.ui.garden

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.WeatherRepository
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import com.mrlaughing.moyuan.util.formatCN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

private const val PREFS_NAME = "garden_prefs"
private const val KEY_GRID_INDEX = "grid_layout_index"
private const val KEY_SEASON = "cached_season"
private const val KEY_WEATHER = "cached_weather"
private const val KEY_GARDEN_MODE = "garden_mode"

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenRepository: GardenRepository,
    private val plantRepository: PlantRepository,
    private val weatherRepository: WeatherRepository,
    private val userPrefs: UserPrefs,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = _uiState.asStateFlow()

    private val _gardenMode = MutableStateFlow(GardenMode.AUTO)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages

    init {
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 立即设置：真实季节（LocalDate 计算无IO延迟）+ 上次缓存的天气
        val todaySeason = SeasonEngine.getSeason(LocalDate.now())
        val cachedSeason = try { Season.valueOf(prefs.getString(KEY_SEASON, todaySeason.name) ?: todaySeason.name) } catch (_: Exception) { todaySeason }
        val cachedWeather = try { Weather.valueOf(prefs.getString(KEY_WEATHER, Weather.CLEAR.name) ?: Weather.CLEAR.name) } catch (_: Exception) { Weather.CLEAR }
        val savedIndex = prefs.getInt(KEY_GRID_INDEX, DEFAULT_GRID_INDEX)
        val savedMode = try {
            GardenMode.valueOf(prefs.getString(KEY_GARDEN_MODE, GardenMode.AUTO.name) ?: GardenMode.AUTO.name)
        } catch (_: Exception) {
            GardenMode.AUTO
        }
        _gardenMode.value = savedMode

        _uiState.value = GardenUiState(
            season = cachedSeason,
            weather = cachedWeather,
            gridLayoutIndex = savedIndex.coerceIn(0, GRID_LAYOUTS.lastIndex),
            gardenMode = savedMode
        )

        // 并行启动：花园数据（Room 数据库）和天气刷新
        loadGardenData()
        refreshWeather()
    }

    /**
     * 从 Room DB 加载花园数据（发射快，无网络延迟）
     * 自动模式：展示全部已解锁植物（按解锁时间倒序）
     * 自定义模式：仅展示用户"放入花园"的植物（按 gardenOrder）
     */
    private fun loadGardenData() {
        viewModelScope.launch {
            combine(
                gardenRepository.observeGardenState(),
                _gardenMode,
                userPrefs.wereadToken
            ) { gardenState, mode, token ->
                val meta = gardenState.meta
                val todayReadMinutes = meta?.todayReadMinutes ?: 0
                val streakDays = meta?.streakDays ?: 0
                val accumulatedMinutes = meta?.accumulatedMinutes ?: 0
                val unlockedPlants = gardenState.plants.filter { !it.unlockDate.isNullOrEmpty() }
                val totalUnlocked = unlockedPlants.size
                val displayEntities = if (mode == GardenMode.CUSTOM) {
                    unlockedPlants.filter { it.isInGarden }.sortedBy { it.gardenOrder }
                } else {
                    unlockedPlants
                        .sortedWith(compareBy({ it.unlockDate }, { it.id }))
                        .take(GRID_LAYOUTS.last().totalSlots)
                }
                // 随机解锁模型下，"下一株"指累计阅读分钟即将跨过的下一个阈值
                val nextThreshold = PlantDefinitions.all
                    .map { it.unlockThreshold }
                    .distinct()
                    .filter { it > accumulatedMinutes }
                    .minOrNull()
                val plants = displayEntities.mapIndexed { index, entity ->
                    val plantDef = PlantDefinitions.getById(entity.plantId)
                    PlantUiItem(
                        plantId = (PlantDefinitions.all.indexOfFirst { it.id == entity.plantId } + 1L).coerceAtLeast(1L),
                        name = plantDef?.name ?: entity.plantId,
                        level = 1,
                        gardenSlot = if (mode == GardenMode.CUSTOM) {
                            (entity.gardenOrder - 1).coerceAtLeast(index)
                        } else {
                            null
                        }
                    )
                }
                val requiredSlots = if (mode == GardenMode.CUSTOM) {
                    displayEntities.maxOfOrNull { it.gardenOrder } ?: 0
                } else {
                    plants.size
                }.coerceAtMost(GRID_LAYOUTS.last().totalSlots)
                val requiredGridIndex = GRID_LAYOUTS.indexOfFirst {
                    it.totalSlots >= requiredSlots
                }.takeIf { it >= 0 } ?: GRID_LAYOUTS.lastIndex
                val effectiveGridIndex = maxOf(_uiState.value.gridLayoutIndex, requiredGridIndex)
                GardenUiState(
                    season = _uiState.value.season,
                    weather = _uiState.value.weather,
                    gridLayoutIndex = effectiveGridIndex,
                    gardenMode = mode,
                    plants = plants,
                    todayReadMinutes = todayReadMinutes,
                    streakDays = streakDays,
                    dateText = LocalDate.now().formatCN(),
                    totalUnlocked = totalUnlocked,
                    totalPlants = PlantDefinitions.all.size,
                    accumulatedMinutes = accumulatedMinutes,
                    nextUnlockThreshold = nextThreshold,
                    isAuthorized = !token.isNullOrBlank(),
                    requiredSlots = requiredSlots
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * 切换花园模式（自动 / 自定义）
     */
    fun setGardenMode(mode: GardenMode) {
        _gardenMode.value = mode
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GARDEN_MODE, mode.name)
            .apply()
    }

    /**
     * 异步刷新天气 + 缓存到本地（公开方法，供外部点击触发）
     */
    fun refreshWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val weather = weatherRepository.fetchWeather()
                val season = SeasonEngine.getSeason(LocalDate.now())
                // 缓存到 SharedPreferences
                application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_SEASON, season.name)
                    .putString(KEY_WEATHER, weather.name)
                    .apply()
                _uiState.value = _uiState.value.copy(season = season, weather = weather)
            } catch (_: Exception) {
                // 网络失败时保留缓存值
            }
        }
    }

    fun setGridLayout(index: Int) {
        val clamped = index.coerceIn(0, GRID_LAYOUTS.lastIndex)
        val config = GRID_LAYOUTS[clamped]
        val state = _uiState.value
        if (config.totalSlots < state.requiredSlots) {
            _messages.tryEmit("当前植物数量需要更大的花圃")
            return
        }
        if (state.totalUnlocked < config.minUnlockedPlants) {
            _messages.tryEmit("再发现一些植物后，这座花圃会自然展开")
            return
        }
        _uiState.value = _uiState.value.copy(gridLayoutIndex = clamped)
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_GRID_INDEX, clamped)
            .apply()
    }

    fun refresh() {
        refreshWeather()
    }

    fun movePlantToSlot(plantId: Long, targetSlot: Int) {
        val plantStringId = PlantDefinitions.getByLongIndex(plantId)?.id ?: return
        viewModelScope.launch {
            if (plantRepository.movePlantToSlot(plantStringId, targetSlot)) {
                _messages.emit("花圃布局已保存")
            }
        }
    }
}
