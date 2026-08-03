package com.mrlaughing.moyuan.ui.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.repository.GardenPlacementResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * 植物详情 ViewModel
 *
 * v2.0：简化，只展示植物名、大图、描述、诗文引用（lore）、解锁条件
 */
@HiltViewModel
@OptIn(kotlinx.coroutines.FlowPreview::class)
class PlantDetailViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val wereadRepository: WereadRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun loadPlant(plantStringId: String) {
        viewModelScope.launch {
            try {
                val plantDef = PlantDefinitions.getById(plantStringId)
                if (plantDef == null) {
                    Log.e("PlantDetailVM", "未找到植物定义: $plantStringId")
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = "未知植物"
                    )
                    return@launch
                }

                Log.d("PlantDetailVM", "加载植物: $plantStringId (${plantDef.name})")

                plantRepository.observePlant(plantStringId)
                    .distinctUntilChanged()
                    .debounce(150)
                    .collect { entity ->  // entity: PlantStateEntity?
                        try {
                            val isUnlocked = entity != null && !entity.unlockDate.isNullOrEmpty()

                            // 阅读时光印记：结合发现时间 + 应用陪伴 + 微信读书阅读量
                            val discoveryDate = entity?.unlockDate
                            val discoveryLine = if (discoveryDate != null) {
                                "你于 $discoveryDate 发现这株「${plantDef.name}」"
                            } else {
                                "这株植物尚未与你相遇"
                            }
                            val (wereadLine, readNoteLine) = buildWereadContext()
                            val appDaysLine = buildAppDaysLine()

                            _uiState.value = PlantDetailUiState(
                                plantIdStr = plantStringId,
                                name = plantDef.name,
                                description = plantDef.description,
                                lore = plantDef.lore,
                                isUnlocked = isUnlocked,
                                unlockThreshold = plantDef.unlockThreshold,
                                isInGarden = entity?.isInGarden ?: false,
                                unlockDate = entity?.unlockDate,
                                discoveryLine = discoveryLine,
                                wereadLine = wereadLine,
                                appDaysLine = appDaysLine,
                                readNoteLine = readNoteLine
                            )
                        } catch (e: Exception) {
                            Log.e("PlantDetailVM", "处理植物状态失败", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e("PlantDetailVM", "加载植物详情失败 plantStringId=$plantStringId", e)
                val plantDef = PlantDefinitions.getById(plantStringId)
                if (plantDef != null) {
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = plantDef.name,
                        description = plantDef.description,
                        lore = plantDef.lore,
                        isUnlocked = false,
                        unlockThreshold = plantDef.unlockThreshold
                    )
                } else {
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = "未知植物"
                    )
                }
            }
        }
    }

    /**
     * 切换植物是否放入花园（自定义摆放）
     */
    fun toggleGardenStatus() {
        val plantId = _uiState.value.plantIdStr
        if (plantId.isBlank()) return
        viewModelScope.launch {
            val newStatus = !(_uiState.value.isInGarden)
            try {
                when (plantRepository.updateGardenStatus(plantId, newStatus)) {
                    GardenPlacementResult.ADDED -> _messages.emit("已放入花园")
                    GardenPlacementResult.REMOVED -> _messages.emit("已移出花园")
                    GardenPlacementResult.FULL -> _messages.emit("满园最多陈列 49 株，请先移出一株植物")
                    GardenPlacementResult.LOCKED -> _messages.emit("这株植物尚未被发现")
                    GardenPlacementResult.NOT_FOUND -> _messages.emit("未找到植物状态")
                }
            } catch (e: Exception) {
                Log.e("PlantDetailVM", "切换花园状态失败: $plantId", e)
            }
        }
    }

    /**
     * 阅读时光印记 · 微信读书累计阅读量 + 书摘拾遗文案
     * 一次取数，返回（陪伴行，书摘拾遗行）两句话
     */
    private suspend fun buildWereadContext(): Pair<String, String> {
        val resp = try {
            wereadRepository.fetchReadDataOverall().getOrNull()
        } catch (e: Exception) {
            null
        }
        val totalSec = resp?.totalReadTime ?: 0L
        val readDays = resp?.readDays ?: 0
        val minutes = (totalSec / 60).toInt()
        val hours = minutes / 60
        val remMin = minutes % 60
        val timeText = if (hours > 0) "${hours}小时${remMin}分" else "${remMin}分"
        val wereadLine = "微信读书已陪伴你读过 $readDays 天 · 累计 $timeText"

        // 书摘拾遗：由阅读数据生成的轻养成文案，区别于「文化小传」
        val readNoteLine = when {
            readDays <= 0 -> "墨园静候，待你在书页间拾得第一枚落款"
            hours >= 100 -> "百小时的书香，已在这座花园里长成看不见的根须"
            hours >= 24 -> "廿四小时的阅读，足够让一株草木记住你的温度"
            readDays >= 30 -> "三十个读书的夜，是这座花园最绵长的春雨"
            else -> "每一次翻开书页，都为这座花园添了一缕墨香"
        }
        return wereadLine to readNoteLine
    }

    /**
     * 阅读时光印记 · 应用陪伴天数
     */
    private suspend fun buildAppDaysLine(): String {
        val installTime = try {
            userPrefs.firstLaunchTime.first()
        } catch (e: Exception) {
            0L
        }
        val days = if (installTime > 0) {
            ((System.currentTimeMillis() - installTime) / 86_400_000L).coerceAtLeast(0)
        } else {
            0L
        }
        return "墨园已陪你走过 $days 天"
    }
}

/**
 * 植物详情 UI 状态
 * v2.0：精简，移除等级、稀有度、枯萎等废弃字段
 */
data class PlantDetailUiState(
    val plantIdStr: String = "",
    val name: String = "",
    val description: String = "",
    val lore: String = "",
    val isUnlocked: Boolean = false,
    val unlockThreshold: Int = 0,
    val isInGarden: Boolean = false,
    val unlockDate: String? = null,
    val discoveryLine: String = "",
    val wereadLine: String = "",
    val appDaysLine: String = "",
    val readNoteLine: String = ""
)
