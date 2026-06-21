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
        val maxCount = minOf(plantCount, Constants.GARDEN_FOREGROUND_COUNT + Constants.GARDEN_BACKGROUND_COUNT)

        // 将植物分成3层：远景、中景、前景
        val farCount = (maxCount * 0.25f).toInt().coerceAtLeast(0)
        val midCount = (maxCount * 0.35f).toInt().coerceAtLeast(0)
        val nearCount = maxCount - farCount - midCount

        // ── 远景植物（小、靠上）──
        for (i in 0 until farCount) {
            val xRatio = spreadPositions(i, farCount, 0.08f, 0.92f)
            val yRatio = 0.18f + (i % 3) * 0.06f
            positions.add(PlantRenderInfo(
                bitmap = null, x = viewWidth * xRatio, y = viewHeight * yRatio,
                scale = 0.45f, plantId = -1L
            ))
        }

        // ── 中景植物（中等大小、中间区域）──
        for (i in 0 until midCount) {
            val xRatio = spreadPositions(i, midCount, 0.05f, 0.95f)
            val yRatio = 0.38f + (i % 3) * 0.08f
            positions.add(PlantRenderInfo(
                bitmap = null, x = viewWidth * xRatio, y = viewHeight * yRatio,
                scale = 0.65f, plantId = -1L
            ))
        }

        // ── 前景植物（大、靠下）──
        for (i in 0 until nearCount) {
            val xRatio = spreadPositions(i, nearCount, 0.05f, 0.95f)
            val yRatio = 0.6f + (i.toFloat() / maxOf(1, nearCount)) * 0.28f
            val scale = 0.85f + (i % 2) * 0.08f
            positions.add(PlantRenderInfo(
                bitmap = null, x = viewWidth * xRatio, y = viewHeight * yRatio,
                scale = scale, plantId = -1L
            ))
        }

        // 按 Y 坐标排序（先画远的，再画近的）
        return positions.sortedBy { it.y }
    }

    /**
     * 均匀分布N个点的X坐标比例
     */
    private fun spreadPositions(index: Int, total: Int, startRatio: Float, endRatio: Float): Float {
        if (total <= 1) return (startRatio + endRatio) / 2f
        return startRatio + (index.toFloat() / (total - 1)) * (endRatio - startRatio)
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
