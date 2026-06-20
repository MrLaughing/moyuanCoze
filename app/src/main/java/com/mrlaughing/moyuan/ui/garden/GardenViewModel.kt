package com.mrlaughing.moyuan.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.mrlaughing.moyuan.util.Constants
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.util.formatCN
import kotlin.random.Random
import javax.inject.Inject

/**
 * 花园 ViewModel：管理花园界面数据
 */
@HiltViewModel
class GardenViewModel @Inject constructor(
    // 注入 GardenRepository, GardenEngine（待实现）
    // private val gardenRepository: GardenRepository,
    // private val gardenEngine: GardenEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(GardenUiState())
    val uiState: StateFlow<GardenUiState> = _uiState.asStateFlow()

    init {
        loadGardenData()
    }

    private fun loadGardenData() {
        viewModelScope.launch {
            // TODO: 从 Repository 加载真实数据
            // 目前使用模拟数据
            val season = determineSeason()
            val weather = determineWeather()
            val todayReadMinutes = 45  // 模拟数据
            val streakDays = 7

            _uiState.value = GardenUiState(
                plants = generateMockPlants(),
                season = season,
                weather = weather,
                todayReadMinutes = todayReadMinutes,
                streakDays = streakDays,
                bonusMultiplier = calculateBonus(streakDays, weather),
                dateText = LocalDate.now().formatCN()
            )
        }
    }

    /**
     * 根据月份判断季节
     */
    private fun determineSeason(): Season {
        val month = LocalDate.now().monthValue
        return when (month) {
            3, 4, 5 -> Season.SPRING
            6, 7, 8 -> Season.SUMMER
            9, 10, 11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    /**
     * 根据概率决定天气
     */
    private fun determineWeather(): Weather {
        val rand = Random.nextFloat()
        var cumulative = 0f
        for (weather in Weather.entries) {
            cumulative += weather.probability
            if (rand < cumulative) return weather
        }
        return Weather.CLEAR
    }

    /**
     * 计算加成倍率
     */
    private fun calculateBonus(streakDays: Int, weather: Weather): Float {
        var bonus = 1.0f
        if (streakDays >= 30) {
            bonus *= 1.2f
        } else if (streakDays >= 7) {
            bonus *= 1.1f
        }
        bonus *= weather.multiplier
        return bonus
    }

    /**
     * 生成模拟植物数据
     */
    private fun generateMockPlants(): List<PlantUiItem> {
        return listOf(
            PlantUiItem(1L, "墨兰", 3, 0, Constants.PATH_JIMO),
            PlantUiItem(2L, "青竹", 5, 0, Constants.PATH_SUIHAN),
            PlantUiItem(3L, "紫藤", 2, 1, Constants.PATH_XUNFANG),
            PlantUiItem(4L, "白莲", 1, 0, Constants.PATH_BINGZHU),
            PlantUiItem(5L, "苍松", 4, 0, Constants.PATH_SUIHAN)
        )
    }

    /**
     * 刷新花园数据
     */
    fun refresh() {
        loadGardenData()
    }
}
