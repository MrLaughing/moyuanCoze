package com.mrlaughing.moyuan.engine.growth

import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.WitherStage
import com.mrlaughing.moyuan.engine.LevelResult

/**
 * 等级判定引擎
 *
 * 等级阈值：LV1(0), LV2(120), LV3(480), LV4(1200), LV5(2400)
 * Lv.5 后 minutesToNext=0，进度条满
 */
object GrowthEngine {

    /**
     * 计算等级及进度信息
     *
     * @param accumulatedMinutes 累计有效灌溉分钟数
     * @return LevelResult 包含等级、当前等级内进度、距离下一等级分钟数、进度百分比
     */
    fun calculateLevel(accumulatedMinutes: Int): LevelResult {
        val currentLevel = GrowthLevel.fromMinutes(accumulatedMinutes)

        // Lv.5 已满级
        if (currentLevel == GrowthLevel.LV5) {
            return LevelResult(
                level = GrowthLevel.LV5,
                progressInLevel = 0,
                minutesToNext = 0,
                progressPercent = 1.0f
            )
        }

        val nextLevel = GrowthLevel.entries.first { it.level == currentLevel.level + 1 }
        val progressInLevel = accumulatedMinutes - currentLevel.thresholdMinutes
        val minutesToNext = nextLevel.thresholdMinutes - accumulatedMinutes
        val levelSpan = nextLevel.thresholdMinutes - currentLevel.thresholdMinutes
        val progressPercent = if (levelSpan > 0) {
            progressInLevel.toFloat() / levelSpan.toFloat()
        } else {
            0f
        }

        return LevelResult(
            level = currentLevel,
            progressInLevel = progressInLevel.coerceAtLeast(0),
            minutesToNext = minutesToNext.coerceAtLeast(0),
            progressPercent = progressPercent.coerceIn(0f, 1f)
        )
    }

    /**
     * 根据植物ID、等级和枯萎阶段选择素材文件名
     *
     * 命名规则：{plantId}_{level}_{witherStage}
     * 例如：orchid_lv3_none, orchid_lv2_fade
     * 枯寂植物使用统一枯寂图：{plantId}_dead
     *
     * @param plantId 植物ID
     * @param level 生长等级
     * @param witherStage 枯萎阶段
     * @return 素材文件名（不含扩展名）
     */
    fun selectPlantImage(plantId: String, level: GrowthLevel, witherStage: WitherStage): String {
        // 枯寂阶段使用统一的枯寂图
        if (witherStage == WitherStage.DEAD) {
            return "${plantId}_dead"
        }

        val levelSuffix = "lv${level.level}"
        val witherSuffix = if (witherStage == WitherStage.NONE) {
            "none"
        } else {
            witherStage.name.lowercase()
        }

        return "${plantId}_${levelSuffix}_${witherSuffix}"
    }

    /**
     * 判断是否满级
     */
    fun isMaxLevel(accumulatedMinutes: Int): Boolean {
        return accumulatedMinutes >= GrowthLevel.LV5.thresholdMinutes
    }

    /**
     * 获取指定等级的阈值范围
     *
     * @return Pair(当前等级阈值, 下一等级阈值)，Lv.5 时 second 为 null
     */
    fun getLevelRange(level: GrowthLevel): Pair<Int, Int?> {
        val nextLevel = GrowthLevel.entries.find { it.level == level.level + 1 }
        return Pair(level.thresholdMinutes, nextLevel?.thresholdMinutes)
    }
}
