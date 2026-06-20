package com.mrlaughing.moyuan.render

import android.graphics.Bitmap
import com.mrlaughing.moyuan.util.Constants

/**
 * 植物渲染信息
 */
data class PlantRenderInfo(
    val bitmap: Bitmap?,
    val x: Float,
    val y: Float,
    val scale: Float,
    val plantId: Long
)

/**
 * 花园渲染器：计算植物在画布上的布局位置
 */
object GardenRenderer {

    /**
     * 根据植物数量和视图尺寸计算每株植物的渲染位置
     * 前景 [GARDEN_FOREGROUND_COUNT] 株 + 远景 [GARDEN_BACKGROUND_COUNT] 株
     * Y 坐标越大越靠前（近景）
     */
    fun calculatePositions(
        plantCount: Int,
        viewWidth: Int,
        viewHeight: Int
    ): List<PlantRenderInfo> {
        if (plantCount == 0 || viewWidth <= 0 || viewHeight <= 0) return emptyList()

        val positions = mutableListOf<PlantRenderInfo>()
        val foregroundCount = minOf(Constants.GARDEN_FOREGROUND_COUNT, plantCount)
        val backgroundCount = minOf(Constants.GARDEN_BACKGROUND_COUNT, plantCount - foregroundCount)

        // ── 远景植物 ──
        for (i in 0 until backgroundCount) {
            val xRatio = 0.15f + (i.toFloat() / maxOf(1, backgroundCount - 1).coerceAtLeast(1)) * 0.7f
            val yRatio = 0.25f + (i % 2) * 0.08f
            val x = viewWidth * xRatio
            val y = viewHeight * yRatio
            val scale = 0.55f  // 远景小
            positions.add(
                PlantRenderInfo(
                    bitmap = null,
                    x = x,
                    y = y,
                    scale = scale,
                    plantId = -1L  // 待绑定
                )
            )
        }

        // ── 前景植物 ──
        for (i in 0 until foregroundCount) {
            val xRatio = when (foregroundCount) {
                1 -> 0.5f
                2 -> if (i == 0) 0.3f else 0.7f
                else -> 0.2f + (i.toFloat() / (foregroundCount - 1)) * 0.6f
            }
            val yRatio = 0.55f + (i.toFloat() / foregroundCount) * 0.3f
            val x = viewWidth * xRatio
            val y = viewHeight * yRatio
            val scale = 0.85f + (i % 2) * 0.1f  // 前景大
            positions.add(
                PlantRenderInfo(
                    bitmap = null,
                    x = x,
                    y = y,
                    scale = scale,
                    plantId = -1L  // 待绑定
                )
            )
        }

        // 按 Y 坐标排序（先画远的，再画近的）
        return positions.sortedBy { it.y }
    }

    /**
     * 将植物列表绑定到预计算的位置上
     */
    fun bindPlantsToPositions(
        plants: List<Triple<Long, Bitmap?, Int>>,  // (plantId, bitmap, level)
        positions: List<PlantRenderInfo>
    ): List<PlantRenderInfo> {
        val bound = mutableListOf<PlantRenderInfo>()
        val count = minOf(plants.size, positions.size)
        for (i in 0 until count) {
            val pos = positions[i]
            val (plantId, bitmap, level) = plants[i]
            // 根据等级微调 scale
            val levelBonus = (level - 1) * 0.03f
            bound.add(
                pos.copy(
                    bitmap = bitmap,
                    plantId = plantId,
                    scale = pos.scale + levelBonus
                )
            )
        }
        return bound
    }
}
