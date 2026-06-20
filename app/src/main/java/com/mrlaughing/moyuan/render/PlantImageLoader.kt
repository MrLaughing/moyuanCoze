package com.mrlaughing.moyuan.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.bumptech.glide.Glide
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.util.Constants
import java.util.concurrent.ExecutionException

/**
 * 植物图片加载器：从 assets 加载植物 PNG，失败时生成程序化占位图
 */
object PlantImageLoader {

    private const val PLACEHOLDER_SIZE = 120

    /**
     * 加载植物图片
     * @param plantId 植物ID
     * @param level 等级 (1-5)
     * @param witherStage 枯萎阶段 (0=健康, 1=轻枯, 2=中枯, 3=死亡)
     * @return Bitmap
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

        // 所有 assets 都找不到，生成程序化占位图
        return generatePlaceholder(context, plantId, level, witherStage)
    }

    /**
     * 加载剪影图（图鉴未解锁时使用）
     */
    fun loadSilhouette(context: Context, plantId: Long): Bitmap? {
        val path = "${Constants.PLANT_ASSET_PREFIX}${plantId}/silhouette${Constants.PLANT_ASSET_SUFFIX}"
        val bitmap = loadFromAssets(context, path)
        if (bitmap != null) return bitmap
        // 生成剪影占位图
        return generateSilhouettePlaceholder(context, plantId)
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
     * 生成程序化占位图：墨水屏风格的植物圆形图标
     * 灰度渐变圆 + 植物名字
     */
    private fun generatePlaceholder(context: Context, plantId: Long, level: Int, witherStage: Int): Bitmap {
        val size = (PLACEHOLDER_SIZE * (0.8f + level * 0.05f)).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 获取植物名称
        val plantName = getPlantDisplayName(plantId)

        // 基础灰度色（枯萎越重越灰）
        val baseGray = 220 - witherStage * 50
        val centerColor = Color.rgb(baseGray, baseGray, baseGray)
        val edgeColor = Color.rgb(
            (baseGray * 0.7f).toInt(),
            (baseGray * 0.7f).toInt(),
            (baseGray * 0.7f).toInt()
        )

        // 绘制渐变圆
        val radius = size / 2f - 4f
        val gradient = RadialGradient(
            size / 2f, size / 2f, radius,
            centerColor, edgeColor,
            Shader.TileMode.CLAMP
        )

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, circlePaint)

        // 绘制边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(80, 80, 80)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(size / 2f, size / 2f, radius, borderPaint)

        // 绘制等级文字
        val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 50, 50)
            textSize = size / 4f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("Lv$level", size / 2f, size / 2f + levelPaint.textSize / 3f, levelPaint)

        // 绘制植物名字（裁剪为两字）
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 100, 100)
            textSize = size / 6f
            textAlign = Paint.Align.CENTER
        }
        val displayName = if (plantName.length > 2) plantName.substring(0, 2) else plantName
        canvas.drawText(displayName, size / 2f, size - size / 6f, namePaint)

        return bitmap
    }

    /**
     * 生成剪影占位图：灰色圆形 + 问号
     */
    private fun generateSilhouettePlaceholder(context: Context, plantId: Long): Bitmap {
        val size = PLACEHOLDER_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 绘制灰色圆形
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 200, 200)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, fillPaint)

        // 绘制边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(150, 150, 150)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, borderPaint)

        // 绘制问号
        val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 120, 120)
            textSize = size / 2f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("?", size / 2f, size / 2f + questionPaint.textSize / 3f, questionPaint)

        return bitmap
    }

    /**
     * 获取植物显示名称
     */
    private fun getPlantDisplayName(plantId: Long): String {
        val index = (plantId - 1).toInt()
        return if (index in PlantDefinitions.all.indices) {
            PlantDefinitions.all[index].name
        } else {
            "植物"
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
