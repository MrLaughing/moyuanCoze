package com.mrlaughing.moyuan.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 *
 * v2.0 Asset路径约定: plants/{中文名}.png  (扁平结构，无等级/枯萎子目录)
 * 例如: plants/芭蕉.png, plants/柏.png
 */
object PlantImageLoader {

    private const val PLACEHOLDER_SIZE = 120

    /**
     * 加载植物图片（使用字符串ID直接指定）
     * v2.0：忽略 level 和 witherStage，统一加载扁平 PNG
     * @param plantStringId 植物字符串ID (如 "changpu", "orchid")
     * @return Bitmap
     */
    fun loadByStringId(context: Context, plantStringId: String, level: Int = 1, witherStage: Int = 0): Bitmap? {
        // v2.0：直接加载扁平 PNG，忽略等级和枯萎
        val assetPath = buildAssetPath(plantStringId)
        val bitmap = loadFromAssets(context, assetPath)
        if (bitmap != null) return bitmap

        // 降级：生成程序化占位图
        return generatePlaceholder(context, plantStringId)
    }

    /**
     * 加载植物图片（兼容旧接口 - 使用 Long plantId）
     * v2.0：忽略 level 和 witherStage
     */
    fun load(context: Context, plantId: Long, level: Int = 1, witherStage: Int = 0): Bitmap? {
        val stringId = resolveStringId(plantId)
        return loadByStringId(context, stringId)
    }

    /**
     * 加载剪影图（图鉴未解锁时使用）- 使用字符串ID
     * v2.0：直接加载扁平 PNG 后施加灰度滤镜
     */
    fun loadSilhouetteByStringId(context: Context, plantStringId: String): Bitmap? {
        // v2.0: 直接加载扁平 PNG，然后施加灰度滤镜
        val pngPath = buildAssetPath(plantStringId)
        val pngBitmap = loadFromAssets(context, pngPath)
        if (pngBitmap != null) {
            return applySilhouetteFilter(pngBitmap)
        }
        // 所有 assets 都找不到，生成程序化剪影占位图
        return generateSilhouettePlaceholder(context, plantStringId)
    }

    /**
     * 加载剪影图（兼容旧接口 - 使用 Long plantId）
     */
    fun loadSilhouette(context: Context, plantId: Long): Bitmap? {
        val stringId = resolveStringId(plantId)
        return loadSilhouetteByStringId(context, stringId)
    }

    /**
     * 将 Long plantId 解析为字符串ID（用于构建asset路径）
     */
    private fun resolveStringId(plantId: Long): String {
        val index = (plantId - 1).toInt()
        return if (index in PlantDefinitions.all.indices) {
            PlantDefinitions.all[index].id
        } else {
            "unknown"
        }
    }

    private fun buildAssetPath(plantStringId: String): String {
        // v2.0: 通过 PlantDefinitions 将英文ID转为中文文件名，无等级/枯萎后缀
        val chineseName = PlantDefinitions.getAssetFileName(plantStringId)
        return "${Constants.PLANT_ASSET_PREFIX}${chineseName}${Constants.PLANT_ASSET_SUFFIX}"
    }

    private fun loadFromAssets(context: Context, assetPath: String): Bitmap? {
        // 方式1：直接用 InputStream 解码，避免 Glide 的 Fragment 生命周期问题
        try {
            context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.let { return it }
            }
        } catch (e: Exception) {
            // Asset 不存在或解码失败，降级到 Glide
        }

        // 方式2：Glide 降级
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
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成程序化占位图：简洁的植物圆形图标
     * v2.0：只显示植物名称，无等级、无枯萎效果
     */
    private fun generatePlaceholder(context: Context, plantStringId: String): Bitmap {
        val size = PLACEHOLDER_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 获取植物名称
        val plantName = getPlantDisplayName(plantStringId)

        // 基础灰度
        val baseGray = 220
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

        // 绘制植物名字（裁剪为两字）
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 100, 100)
            textSize = size / 3f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val displayName = if (plantName.length > 2) plantName.substring(0, 2) else plantName
        canvas.drawText(displayName, size / 2f, size / 2f + namePaint.textSize / 3f, namePaint)

        return bitmap
    }

    /**
     * 生成剪影占位图：灰色圆形 + 问号
     */
    private fun generateSilhouettePlaceholder(context: Context, plantStringId: String): Bitmap {
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
    private fun getPlantDisplayName(plantStringId: String): String {
        return PlantDefinitions.getById(plantStringId)?.name ?: "植物"
    }

    /**
     * 对图片施加剪影效果：大幅降低饱和度、提高亮度，形成半透明剪影
     */
    private fun applySilhouetteFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = pixel ushr 24 and 0xFF
            val r = pixel ushr 16 and 0xFF
            val g = pixel ushr 8 and 0xFF
            val b = pixel and 0xFF

            // 灰度化
            val gray = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()

            // 向浅灰混合，形成剪影效果（高灰度 + 降低对比度）
            val silhouetteGray = (gray + (200 - gray) * 0.75f).toInt().coerceIn(0, 255)

            // 降低不透明度，让剪影更淡
            val newAlpha = (a * 0.6f).toInt().coerceIn(0, 255)

            pixels[i] = (newAlpha shl 24) or (silhouetteGray shl 16) or (silhouetteGray shl 8) or silhouetteGray
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

}
