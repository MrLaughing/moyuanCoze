package com.mrlaughing.moyuan.ui.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.GardenPlacementResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val plantRepository: PlantRepository
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

                            _uiState.value = PlantDetailUiState(
                                plantIdStr = plantStringId,
                                name = plantDef.name,
                                description = plantDef.description,
                                lore = plantDef.lore,
                                isUnlocked = isUnlocked,
                                unlockThreshold = plantDef.unlockThreshold,
                                isInGarden = entity?.isInGarden ?: false,
                                unlockDate = entity?.unlockDate
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
    val unlockDate: String? = null
)
