package com.mrlaughing.moyuan.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.engine.season.SeasonEngine
import com.mrlaughing.moyuan.util.Constants
import com.mrlaughing.moyuan.util.formatCN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 花园 ViewModel：管理花园界面数据
 *
 * 从 Repository 加载真实数据：
 * - GardenRepository.observeGardenState() 获取花园状态
 * - PlantRepository.observePlants() 获取植物列表
 *
 * 只显示已解锁的植物（unlockDate != null）
 */
@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenRepository: GardenRepository,
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = _uiState.asStateFlow()

    companion object {
        const val IRRIGATION_GOAL_HOURS = 40 // 灌溉目标小时数
    }

    init {
        loadGardenData()
    }

    /**
     * 从 Repository 加载真实花园数据
     */
    private fun loadGardenData() {
        viewModelScope.launch {
            gardenRepository.observeGardenState().collect { gardenState ->
                val season = _uiState.value.season  // 保持用户选择的季节
                val meta = gardenState.meta
                val weather = _uiState.value.weather  // 保持用户选择的天气

                // 从 GardenState 获取 meta 信息
                val todayReadMinutes = meta?.todayReadMinutes ?: 0
                val streakDays = meta?.streakDays ?: 0

                // 计算灌溉进度（分钟转换为小时）
                val irrigationMinutes = meta?.accumulatedMinutes ?: 0
                val irrigationHours = irrigationMinutes / 60

                // 从 PlantState 映射到 PlantUiItem
                val plants = gardenState.plants
                    .filter { !it.unlockDate.isNullOrEmpty() }
                    .mapIndexed { index, entity ->
                        val pathType = pathToConstant(entity.path)
                        PlantUiItem(
                            plantId = (PlantDefinitions.all.indexOfFirst { it.id == entity.plantId } + 1L).coerceAtLeast(1L),
                            name = getPlantName(entity.plantId),
                            level = entity.level,
                            witherStage = entity.witherStage,
                            pathType = pathType
                        )
                    }

                _uiState.value = GardenUiState(
                    plants = plants,
                    season = season,
                    weather = weather,
                    todayReadMinutes = todayReadMinutes,
                    streakDays = streakDays,
                    bonusMultiplier = calculateBonus(streakDays, weather, season),
                    dateText = LocalDate.now().formatCN(),
                    irrigationHours = irrigationHours,
                    irrigationGoal = IRRIGATION_GOAL_HOURS
                )
            }
        }
    }

    /**
     * 切换季节
     */
    fun cycleSeason() {
        val currentSeason = _uiState.value.season
        val seasons = Season.entries.toTypedArray()
        val currentIndex = seasons.indexOf(currentSeason)
        val nextIndex = (currentIndex + 1) % seasons.size
        val newSeason = seasons[nextIndex]

        _uiState.value = _uiState.value.copy(
            season = newSeason,
            bonusMultiplier = calculateBonus(_uiState.value.streakDays, _uiState.value.weather, newSeason)
        )
    }

    /**
     * 切换天气
     */
    fun cycleWeather() {
        val currentWeather = _uiState.value.weather
        val currentSeason = _uiState.value.season
        val isNight = SeasonEngine.isNightHour(java.time.LocalTime.now().hour)

        // 获取当前季节可用的天气
        val availableWeathers = Weather.entries.filter { it.isAvailableIn(currentSeason, isNight) }
        if (availableWeathers.isEmpty()) return

        val currentIndex = availableWeathers.indexOf(currentWeather)
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % availableWeathers.size
        val newWeather = availableWeathers[nextIndex]

        _uiState.value = _uiState.value.copy(
            weather = newWeather,
            bonusMultiplier = calculateBonus(_uiState.value.streakDays, newWeather, currentSeason)
        )
    }

    /**
     * 浇灌植物
     */
    fun waterPlants() {
        // 触发灌溉逻辑（通过刷新数据实现）
        refresh()
    }

    /**
     * 计算加成倍率
     */
    private fun calculateBonus(streakDays: Int, weather: Weather, season: Season): Float {
        var bonus = 1.0f

        // 连续天数加成
        if (streakDays >= 30) {
            bonus *= 1.2f
        } else if (streakDays >= 7) {
            bonus *= 1.1f
        }

        // 天气加成
        bonus *= weather.multiplier

        // 季节加成
        bonus *= season.multiplier

        return bonus
    }

    /**
     * 将路径字符串转换为常量
     */
    private fun pathToConstant(path: String): Int {
        return when (path) {
            "JIMO" -> Constants.PATH_JIMO
            "BINGZHU" -> Constants.PATH_BINGZHU
            "SUIHAN" -> Constants.PATH_SUIHAN
            "XUNFANG" -> Constants.PATH_XUNFANG
            "HIDDEN" -> Constants.PATH_HIDDEN
            else -> Constants.PATH_JIMO
        }
    }

    /**
     * 获取植物名称
     */
    private fun getPlantName(plantId: String): String {
        return when (plantId) {
            "changpu" -> "菖蒲"
            "wenzhu" -> "文竹"
            "orchid" -> "兰草"
            "bajiao" -> "芭蕉"
            "shuixian" -> "水仙"
            "mozhu" -> "墨竹"
            "yelaixiang" -> "夜来香"
            "guihua" -> "桂花"
            "yehehua" -> "夜荷花"
            "lamei" -> "蜡梅"
            "tanhua" -> "昙花"
            "yuejiancao" -> "月见草"
            "pine" -> "青松"
            "cypress" -> "翠柏"
            "plum" -> "寒梅"
            "bamboo" -> "修竹"
            "ginkgo" -> "银杏"
            "guteng" -> "古藤"
            "chrysanthemum" -> "墨菊"
            "ziteng" -> "紫藤"
            "lotus" -> "青莲"
            "haitang" -> "海棠"
            "peony" -> "墨牡丹"
            "bodhi" -> "菩提"
            "lingzhi" -> "灵芝"
            "bianhua" -> "彼岸花"
            "bingtilian" -> "并蒂莲"
            else -> plantId
        }
    }

    /**
     * 刷新花园数据
     */
    fun refresh() {
        loadGardenData()
    }
}
