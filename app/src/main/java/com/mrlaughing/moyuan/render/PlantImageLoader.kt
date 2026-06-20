package com.mrlaughing.moyuan.render

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.mrlaughing.moyuan.util.Constants
import java.util.concurrent.ExecutionException

/**
 * 植物图片加载器：从 assets 加载植物 PNG
 */
object PlantImageLoader {

    /**
     * 加载植物图片
     * @param plantId 植物ID
     * @param level 等级 (1-10)
     * @param witherStage 枯萎阶段 (0=健康, 1=轻枯, 2=中枯, 3=死亡)
     * @return Bitmap 或 null
     */
    fun load(context: Context, plantId: Long, level: Int, witherStage: Int = 0): Bitmap? {
        val assetPath = buildAssetPath(plantId, level, witherStage)
        val bitmap = loadFromAssets(context, assetPath)
        if (bitmap != null) return bitmap

        // 降级策略1：尝试低一级的图片
        if (level > 1) {
            val fallbackPath = buildAssetPath(plantId, level - 1, witherStage)
            val fallback = loadFromAssets(context, fallbackPath)
            if (fallback != null) return fallback
        }

        // 降级策略2：尝试健康状态的同等级图片
        if (witherStage > 0) {
            val healthyPath = buildAssetPath(plantId, level, 0)
            val healthy = loadFromAssets(context, healthyPath)
            if (healthy != null) return applyWitherFilter(healthy, witherStage)
        }

        // 降级策略3：等级1的默认图
        if (level > 1) {
            val defaultPath = buildAssetPath(plantId, 1, 0)
            val default = loadFromAssets(context, defaultPath)
            if (default != null) return default
        }

        return null
    }

    /**
     * 加载剪影图（图鉴未解锁时使用）
     */
    fun loadSilhouette(context: Context, plantId: Long): Bitmap? {
        val path = "${Constants.PLANT_ASSET_PREFIX}${plantId}/silhouette${Constants.PLANT_ASSET_SUFFIX}"
        return loadFromAssets(context, path)
    }

    private fun buildAssetPath(plantId: Long, level: Int, witherStage: Int): String {
        val witherSuffix = if (witherStage > 0) "_w${witherStage}" else ""
        return "${Constants.PLANT_ASSET_PREFIX}${plantId}/lv${level}${witherSuffix}${Constants.PLANT_ASSET_SUFFIX}"
    }

    private fun loadFromAssets(context: Context, assetPath: String): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .load("file:///android_asset/$assetPath")
                .submit()
                .get()
        } catch (e: ExecutionException) {
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
    }

    /**
     * 对健康图片施加枯萎效果：降低对比度、加灰
     */
    private fun applyWitherFilter(source: Bitmap, witherStage: Int): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayShift = witherStage * 40  // 枯萎越重，灰度偏移越大
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = pixel ushr 24 and 0xFF
            val r = pixel ushr 16 and 0xFF
            val g = pixel ushr 8 and 0xFF
            val b = pixel and 0xFF
            // 向灰色混合
            val nr = (r + (128 - r) * grayShift / 120).coerceIn(0, 255)
            val ng = (g + (128 - g) * grayShift / 120).coerceIn(0, 255)
            val nb = (b + (128 - b) * grayShift / 120).coerceIn(0, 255)
            pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
