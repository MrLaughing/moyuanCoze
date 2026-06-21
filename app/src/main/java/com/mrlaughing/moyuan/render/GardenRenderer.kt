package com.mrlaughing.moyuan.render

import android.graphics.*
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.util.Constants
import kotlin.math.cos
import kotlin.math.sin

/**
 * 植物渲染信息
 */
data class PlantRenderInfo(
    val bitmap: Bitmap?,
    val x: Float,
    val y: Float,
    val scale: Float,
    val plantId: Long,
    val plantName: String = "",
    val level: Int = 1,
    val witherStage: Int = 0,
    val pathType: Int = Constants.PATH_JIMO
)

/**
 * 花园渲染器：计算植物在画布上的布局位置，并绘制水墨风格背景和植物
 */
object GardenRenderer {

    /**
     * 四季配色
     */
    data class SeasonColors(
        val sky: Int,
        val wash: Int,
        val mountain: Int,
        val accent: Int,
        val foliage: Int,
        val ground: Int,
        val mountainFar: Int
    )

    private val SEASON_COLORS = mapOf(
        Season.SPRING to SeasonColors(
            sky = Color.parseColor("#F0EBE0"),
            wash = Color.parseColor("#E8E4D9"),
            mountain = Color.parseColor("#C4B998"),
            accent = Color.parseColor("#D4BFA0"),
            foliage = Color.parseColor("#A8B89A"),
            ground = Color.parseColor("#D6D3CC"),
            mountainFar = Color.parseColor("#D6D3CC")
        ),
        Season.SUMMER to SeasonColors(
            sky = Color.parseColor("#EDECE3"),
            wash = Color.parseColor("#E0E0D2"),
            mountain = Color.parseColor("#8FA895"),
            accent = Color.parseColor("#7F9A7F"),
            foliage = Color.parseColor("#6B8F6B"),
            ground = Color.parseColor("#C8C8B8"),
            mountainFar = Color.parseColor("#C8D6C8")
        ),
        Season.AUTUMN to SeasonColors(
            sky = Color.parseColor("#F2EDE0"),
            wash = Color.parseColor("#EBE0CC"),
            mountain = Color.parseColor("#B89878"),
            accent = Color.parseColor("#C49A6C"),
            foliage = Color.parseColor("#A08050"),
            ground = Color.parseColor("#D0C4B0"),
            mountainFar = Color.parseColor("#D0C8B8")
        ),
        Season.WINTER to SeasonColors(
            sky = Color.parseColor("#E8E8EC"),
            wash = Color.parseColor("#E0E0E8"),
            mountain = Color.parseColor("#9EA8B8"),
            accent = Color.parseColor("#B0B8C8"),
            foliage = Color.parseColor("#8890A0"),
            ground = Color.parseColor("#C8C8D0"),
            mountainFar = Color.parseColor("#C8CCD8")
        )
    )

    // 墨色常量
    private val INK_DARK = Color.parseColor("#2C2416")
    private val INK_MEDIUM = Color.parseColor("#57534E")
    private val INK_LIGHT = Color.parseColor("#A8A29E")
    private val INK_WASH = Color.parseColor("#D6D3CC")

    // 装饰色
    private val PETAL_COLOR = Color.parseColor("#E8C4C4")
    private val LEAF_COLOR = Color.parseColor("#C49A6C")
    private val SNOW_COLOR = Color.parseColor("#F0F0F4")

    /**
     * 绘制完整花园画面（背景 + 天气效果 + 植物）
     */
    fun drawGarden(
        canvas: Canvas,
        width: Int,
        height: Int,
        season: Season,
        weather: Weather,
        plants: List<PlantRenderInfo>
    ) {
        val colors = SEASON_COLORS[season] ?: SEASON_COLORS[Season.SPRING]!!

        // 1. 绘制背景层
        drawBackgroundLayer(canvas, width, height, colors, season)

        // 2. 绘制天气效果
        drawWeatherEffect(canvas, width, height, weather, colors)

        // 3. 绘制植物（Canvas绘制水墨风）
        drawPlants(canvas, plants, width, height)
    }

    /**
     * 绘制水墨风格背景
     */
    private fun drawBackgroundLayer(
        canvas: Canvas,
        width: Int,
        height: Int,
        colors: SeasonColors,
        season: Season
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        // 创建渐变背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bgGradient = LinearGradient(
            0f, 0f, 0f, h,
            colors.sky, colors.wash,
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgGradient
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 绘制远山层1（最远，最淡）
        drawMountainLayer(canvas, w, h, colors, isFarLayer = true)

        // 绘制近山层
        drawMountainLayer(canvas, w, h, colors, isFarLayer = false)

        // 绘制地面线
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.mountain
            strokeWidth = 0.5f
            alpha = 150
        }
        canvas.drawLine(0f, h * 0.78f, w, h * 0.78f, groundPaint)

        // 绘制散石
        drawStone(canvas, w * 0.08f, h * 0.82f, colors, 12f)
        drawStone(canvas, w * 0.88f, h * 0.84f, colors, 10f)

        // 绘制苔点
        drawMoss(canvas, w * 0.12f, h * 0.8f, colors)
        drawMoss(canvas, w * 0.18f, h * 0.79f, colors, 0.5f)
        drawMoss(canvas, w * 0.85f, h * 0.8f, colors, 0.6f)

        // 绘制季节装饰
        when (season) {
            Season.SPRING -> drawSpringPetals(canvas, w, h)
            Season.AUTUMN -> drawAutumnLeaves(canvas, w, h)
            Season.WINTER -> drawWinterSnowCap(canvas, w, h, colors)
            Season.SUMMER -> { /* 无特殊装饰 */ }
        }
    }

    private fun drawMountainLayer(canvas: Canvas, w: Float, h: Float, colors: SeasonColors, isFarLayer: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (isFarLayer) {
                color = colors.mountainFar
                alpha = 64
            } else {
                color = colors.mountain
                alpha = 100
            }
        }

        val path = Path()
        if (isFarLayer) {
            // 远山轮廓
            path.moveTo(0f, h * 0.45f)
            path.quadTo(w * 0.15f, h * 0.28f, w * 0.25f, h * 0.35f)
            path.quadTo(w * 0.4f, h * 0.22f, w * 0.55f, h * 0.32f)
            path.quadTo(w * 0.7f, h * 0.2f, w * 0.85f, h * 0.3f)
            path.quadTo(w * 0.95f, h * 0.26f, w, h * 0.35f)
            path.lineTo(w, h * 0.45f)
            path.quadTo(w * 0.85f, h * 0.42f, w * 0.7f, h * 0.45f)
            path.quadTo(w * 0.5f, h * 0.38f, w * 0.3f, h * 0.44f)
            path.quadTo(w * 0.15f, h * 0.4f, 0f, h * 0.45f)
        } else {
            // 近山轮廓
            path.moveTo(0f, h * 0.52f)
            path.quadTo(w * 0.12f, h * 0.35f, w * 0.2f, h * 0.42f)
            path.quadTo(w * 0.35f, h * 0.32f, w * 0.5f, h * 0.4f)
            path.quadTo(w * 0.65f, h * 0.3f, w * 0.78f, h * 0.38f)
            path.quadTo(w * 0.9f, h * 0.34f, w, h * 0.42f)
            path.lineTo(w, h * 0.52f)
            path.quadTo(w * 0.85f, h * 0.49f, w * 0.7f, h * 0.51f)
            path.quadTo(w * 0.5f, h * 0.46f, w * 0.3f, h * 0.5f)
            path.quadTo(w * 0.15f, h * 0.48f, 0f, h * 0.52f)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawStone(canvas: Canvas, x: Float, y: Float, colors: SeasonColors, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colors.ground
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = colors.mountain
            strokeWidth = 0.5f
        }

        val path = Path()
        path.moveTo(x - size * 0.4f, y)
        path.quadTo(x - size * 0.5f, y - size * 0.3f, x - size * 0.2f, y - size * 0.4f)
        path.quadTo(x + size * 0.2f, y - size * 0.45f, x + size * 0.4f, y - size * 0.2f)
        path.quadTo(x + size * 0.5f, y + size * 0.1f, x + size * 0.3f, y + size * 0.1f)
        path.quadTo(x, y + size * 0.15f, x - size * 0.4f, y)
        path.close()
        canvas.drawPath(path, paint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawMoss(canvas: Canvas, x: Float, y: Float, colors: SeasonColors, size: Float = 0.8f) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colors.mountain
            alpha = 77
        }
        canvas.drawCircle(x, y, size, paint)
    }

    private fun drawSpringPetals(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = PETAL_COLOR
            alpha = 128
        }
        val petalPositions = listOf(
            Pair(w * 0.2f, h * 0.2f),
            Pair(w * 0.35f, h * 0.15f),
            Pair(w * 0.7f, h * 0.18f),
            Pair(w * 0.5f, h * 0.1f),
            Pair(w * 0.85f, h * 0.22f)
        )
        petalPositions.forEach { (x, y) ->
            canvas.drawCircle(x, y, 1.5f, paint)
        }
    }

    private fun drawAutumnLeaves(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = LEAF_COLOR
            alpha = 102
        }
        val leafPositions = listOf(
            Pair(w * 0.15f, h * 0.5f),
            Pair(w * 0.4f, h * 0.55f),
            Pair(w * 0.65f, h * 0.48f),
            Pair(w * 0.8f, h * 0.52f)
        )
        leafPositions.forEach { (x, y) ->
            canvas.drawCircle(x, y, 1.8f, paint)
        }
    }

    private fun drawWinterSnowCap(canvas: Canvas, w: Float, h: Float, colors: SeasonColors) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = SNOW_COLOR
            alpha = 153
        }

        val path = Path()
        path.moveTo(0f, h * 0.42f)
        path.quadTo(w * 0.15f, h * 0.28f, w * 0.25f, h * 0.35f)
        path.quadTo(w * 0.4f, h * 0.22f, w * 0.55f, h * 0.32f)
        path.quadTo(w * 0.7f, h * 0.2f, w * 0.85f, h * 0.3f)
        path.quadTo(w * 0.95f, h * 0.26f, w, h * 0.35f)
        path.lineTo(w, h * 0.32f)
        path.quadTo(w * 0.85f, h * 0.23f, w * 0.7f, h * 0.18f)
        path.quadTo(w * 0.55f, h * 0.28f, w * 0.4f, h * 0.2f)
        path.quadTo(w * 0.25f, h * 0.3f, w * 0.15f, h * 0.25f)
        path.quadTo(0f, h * 0.38f, 0f, h * 0.42f)
        path.close()
        canvas.drawPath(path, paint)
    }

    /**
     * 绘制天气效果
     */
    private fun drawWeatherEffect(canvas: Canvas, width: Int, height: Int, weather: Weather, colors: SeasonColors) {
        val w = width.toFloat()
        val h = height.toFloat()

        when (weather) {
            Weather.SPRING_RAIN -> drawRainEffect(canvas, w, h)
            Weather.OVERCAST -> drawCloudEffect(canvas, w, h)
            Weather.FOGGY -> drawFogEffect(canvas, w, h)
            Weather.MOONLIT -> drawMoonEffect(canvas, w, h)
            Weather.FIRST_SNOW -> drawSnowEffect(canvas, w, h)
            Weather.CLEAR -> drawClearEffect(canvas, w, h)
        }
    }

    private fun drawRainEffect(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#78716C")
            strokeWidth = 0.5f
            strokeCap = Paint.Cap.ROUND
            alpha = 64
        }

        val rainLines = listOf(
            floatArrayOf(w * 0.1f, h * 0.1f, w * 0.08f, h * 0.2f),
            floatArrayOf(w * 0.25f, h * 0.05f, w * 0.23f, h * 0.18f),
            floatArrayOf(w * 0.4f, h * 0.12f, w * 0.38f, h * 0.25f),
            floatArrayOf(w * 0.55f, h * 0.08f, w * 0.53f, h * 0.2f),
            floatArrayOf(w * 0.7f, h * 0.15f, w * 0.68f, h * 0.28f),
            floatArrayOf(w * 0.85f, h * 0.06f, w * 0.83f, h * 0.18f),
            floatArrayOf(w * 0.18f, h * 0.3f, w * 0.16f, h * 0.42f),
            floatArrayOf(w * 0.5f, h * 0.35f, w * 0.48f, h * 0.48f),
            floatArrayOf(w * 0.75f, h * 0.28f, w * 0.73f, h * 0.4f)
        )

        rainLines.forEach { line ->
            canvas.drawLine(line[0], line[1], line[2], line[3], paint)
        }
    }

    private fun drawCloudEffect(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#78716C")
            alpha = 38
        }

        val path = Path()
        path.moveTo(0f, h * 0.08f)
        path.quadTo(w * 0.15f, h * 0.04f, w * 0.25f, h * 0.07f)
        path.quadTo(w * 0.35f, h * 0.03f, w * 0.45f, h * 0.06f)
        path.quadTo(w * 0.55f, h * 0.02f, w * 0.65f, h * 0.05f)
        path.quadTo(w * 0.75f, h * 0.03f, w * 0.85f, h * 0.06f)
        path.quadTo(w * 0.95f, h * 0.04f, w, h * 0.08f)
        path.lineTo(w, h * 0.12f)
        path.quadTo(w * 0.9f, h * 0.1f, w * 0.8f, h * 0.13f)
        path.quadTo(w * 0.65f, h * 0.09f, w * 0.5f, h * 0.12f)
        path.quadTo(w * 0.35f, h * 0.08f, w * 0.2f, h * 0.11f)
        path.quadTo(w * 0.1f, h * 0.09f, 0f, h * 0.12f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawFogEffect(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#A8A29E")
            strokeCap = Paint.Cap.ROUND
            alpha = 30
        }

        // 多层雾带
        listOf(
            Pair(h * 0.35f, 2f),
            Pair(h * 0.45f, 1.5f),
            Pair(h * 0.55f, 1f)
        ).forEach { (y, strokeWidth) ->
            paint.strokeWidth = strokeWidth
            val path = Path()
            path.moveTo(0f, y)
            path.quadTo(w * 0.3f, y - h * 0.03f, w * 0.5f, y)
            path.quadTo(w * 0.7f, y + h * 0.02f, w, y)
            canvas.drawPath(path, paint)
        }
    }

    private fun drawMoonEffect(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            alpha = 51
        }

        // 月亮
        paint.color = Color.parseColor("#D6D3CC")
        canvas.drawCircle(w * 0.85f, h * 0.08f, 8f, paint)

        // 晕染
        paint.color = Color.parseColor("#2C2416")
        paint.alpha = 20
        canvas.drawCircle(w * 0.85f, h * 0.08f, 14f, paint)
    }

    private fun drawSnowEffect(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#C4B998")
            alpha = 89
        }

        val snowPositions = listOf(
            Pair(w * 0.08f, h * 0.05f), Pair(w * 0.2f, h * 0.12f),
            Pair(w * 0.35f, h * 0.08f), Pair(w * 0.5f, h * 0.15f),
            Pair(w * 0.65f, h * 0.06f), Pair(w * 0.78f, h * 0.14f),
            Pair(w * 0.92f, h * 0.09f), Pair(w * 0.15f, h * 0.28f),
            Pair(w * 0.42f, h * 0.32f), Pair(w * 0.7f, h * 0.25f),
            Pair(w * 0.3f, h * 0.45f), Pair(w * 0.55f, h * 0.48f),
            Pair(w * 0.82f, h * 0.5f), Pair(w * 0.12f, h * 0.6f),
            Pair(w * 0.38f, h * 0.58f), Pair(w * 0.62f, h * 0.65f),
            Pair(w * 0.88f, h * 0.6f)
        )

        snowPositions.forEachIndexed { index, (x, y) ->
            val size = 0.5f + (index % 3) * 0.3f
            canvas.drawCircle(x, y, size, paint)
        }
    }

    private fun drawClearEffect(canvas: Canvas, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            alpha = 20
        }

        paint.color = INK_DARK
        canvas.drawCircle(w * 0.88f, h * 0.06f, 6f, paint)

        paint.alpha = 8
        canvas.drawCircle(w * 0.88f, h * 0.06f, 10f, paint)
    }

    /**
     * 根据植物数量和视图尺寸计算每株植物的渲染位置
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

        // 远景植物（小、靠上）
        for (i in 0 until farCount) {
            val xRatio = spreadPositions(i, farCount, 0.08f, 0.92f)
            val yRatio = 0.18f + (i % 3) * 0.06f
            positions.add(PlantRenderInfo(
                bitmap = null, x = viewWidth * xRatio, y = viewHeight * yRatio,
                scale = 0.45f, plantId = -1L
            ))
        }

        // 中景植物（中等大小、中间区域）
        for (i in 0 until midCount) {
            val xRatio = spreadPositions(i, midCount, 0.05f, 0.95f)
            val yRatio = 0.38f + (i % 3) * 0.08f
            positions.add(PlantRenderInfo(
                bitmap = null, x = viewWidth * xRatio, y = viewHeight * yRatio,
                scale = 0.65f, plantId = -1L
            ))
        }

        // 前景植物（大、靠下）
        for (i in 0 until nearCount) {
            val xRatio = spreadPositions(i, nearCount, 0.05f, 0.95f)
            val yRatio = 0.6f + (i.toFloat() / maxOf(1, nearCount)) * 0.28f
            val scale = 0.85f + (i % 2) * 0.08f
            positions.add(PlantRenderInfo(
                bitmap = null, x = viewWidth * xRatio, y = viewHeight * yRatio,
                scale = scale, plantId = -1L
            ))
        }

        return positions.sortedBy { it.y }
    }

    private fun spreadPositions(index: Int, total: Int, startRatio: Float, endRatio: Float): Float {
        if (total <= 1) return (startRatio + endRatio) / 2f
        return startRatio + (index.toFloat() / (total - 1)) * (endRatio - startRatio)
    }

    /**
     * 将植物列表绑定到预计算的位置上
     */
    fun bindPlantsToPositions(
        plants: List<Triple<Long, Bitmap?, Pair<Int, Pair<Int, Int>>>>,  // (plantId, bitmap, (level, (pathType, witherStage)))
        positions: List<PlantRenderInfo>
    ): List<PlantRenderInfo> {
        val bound = mutableListOf<PlantRenderInfo>()
        val count = minOf(plants.size, positions.size)
        for (i in 0 until count) {
            val pos = positions[i]
            val (plantId, bitmap, extra) = plants[i]
            val (level, pathWither) = extra
            val (pathType, witherStage) = pathWither
            val levelBonus = (level - 1) * 0.03f
            bound.add(
                pos.copy(
                    bitmap = bitmap,
                    plantId = plantId,
                    scale = pos.scale + levelBonus,
                    level = level,
                    pathType = pathType,
                    witherStage = witherStage
                )
            )
        }
        return bound
    }

    /**
     * 获取植物画风类型
     */
    fun getPlantArchetype(plantName: String, pathType: Int): PlantArchetype {
        // 根据植物名称判断
        if (plantName.contains("竹")) return PlantArchetype.BAMBOO
        if (plantName.contains("松") || plantName.contains("柏") || plantName.contains("银杏") || plantName.contains("藤")) return PlantArchetype.PINE
        if (plantName.contains("兰") || plantName.contains("蒲") || plantName.contains("文") || plantName.contains("仙")) return PlantArchetype.GRASS
        if (plantName.contains("花") || plantName.contains("菊") || plantName.contains("荷") || plantName.contains("莲") || plantName.contains("牡丹") || plantName.contains("棠") || plantName.contains("梅") || plantName.contains("桂") || plantName.contains("芝")) return PlantArchetype.FLOWER
        if (plantName.contains("藤")) return PlantArchetype.VINE
        if (plantName.contains("蕉") || plantName.contains("菩提")) return PlantArchetype.BROADLEAF

        // 根据路径类型判断
        return when (pathType) {
            Constants.PATH_JIMO -> PlantArchetype.GRASS  // 积墨多为草叶
            Constants.PATH_BINGZHU -> PlantArchetype.FLOWER  // 秉烛多为花卉
            Constants.PATH_SUIHAN -> PlantArchetype.PINE  // 岁寒多为松柏
            Constants.PATH_XUNFANG -> PlantArchetype.FLOWER  // 寻芳多为花卉
            Constants.PATH_HIDDEN -> PlantArchetype.FLOWER  // 隐藏多为忘忧草/彼岸花
            else -> PlantArchetype.GRASS
        }
    }

    enum class PlantArchetype {
        BAMBOO, PINE, GRASS, FLOWER, VINE, BROADLEAF
    }

    /**
     * 使用Canvas绘制水墨风格植物
     */
    private fun drawPlants(canvas: Canvas, plants: List<PlantRenderInfo>, canvasWidth: Int, canvasHeight: Int) {
        // 按Y坐标排序（先画远的，再画近的）
        val sortedPlants = plants.sortedBy { it.y }

        sortedPlants.forEach { plant ->
            val archetype = getPlantArchetype(plant.plantName, plant.pathType)
            val opacity = (255 * (1 - plant.witherStage * 0.2f)).toInt().coerceIn(50, 255)
            val density = maxOf(0, 3 - plant.witherStage)

            when (archetype) {
                PlantArchetype.BAMBOO -> drawBamboo(canvas, plant.x, plant.y, plant.scale, plant.level, opacity, density)
                PlantArchetype.PINE -> drawPine(canvas, plant.x, plant.y, plant.scale, plant.level, opacity, density)
                PlantArchetype.GRASS -> drawGrass(canvas, plant.x, plant.y, plant.scale, plant.level, opacity, density)
                PlantArchetype.FLOWER -> drawFlower(canvas, plant.x, plant.y, plant.scale, plant.level, opacity, density)
                PlantArchetype.VINE -> drawVine(canvas, plant.x, plant.y, plant.scale, plant.level, opacity, density)
                PlantArchetype.BROADLEAF -> drawBroadleaf(canvas, plant.x, plant.y, plant.scale, plant.level, opacity, density)
            }
        }
    }

    /**
     * 绘制墨竹：主干 + 竹枝 + 竹叶
     */
    private fun drawBamboo(canvas: Canvas, x: Float, y: Float, scale: Float, level: Int, opacity: Int, density: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = opacity
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val h = 28 * scale
        val trunkSegments = minOf(level + 2, 5)

        // 绘制主干
        paint.color = INK_DARK
        paint.strokeWidth = 1.2f * scale
        for (i in 0 until trunkSegments) {
            val segY = y - (h / trunkSegments) * i
            val segH = h / trunkSegments
            canvas.drawLine(x, segY, x, segY - segH, paint)
        }

        // 绘制枝条 + 竹叶
        paint.color = INK_MEDIUM
        paint.strokeWidth = 0.5f * scale
        val branchCount = density * minOf(level + 1, 4)
        for (i in 0 until branchCount) {
            val branchY = y - (h * 0.3f) - (h * 0.6f / 4) * i
            val side = if (i % 2 == 0) 1f else -1f
            val bx = x + side * (4 + i * 2) * scale

            // 枝条
            val path = Path()
            path.moveTo(x, branchY)
            path.quadTo(x + side * 2 * scale, branchY - 2 * scale, bx, branchY - 3 * scale)
            canvas.drawPath(path, paint)

            // 竹叶（3片一组）
            for (j in 0..2) {
                val leafAngle = (j - 1) * 0.3f + side * 0.2f
                val lx = bx + cos(leafAngle).toFloat() * 5 * scale
                val ly = branchY - 3 * scale + sin(leafAngle).toFloat() * 5 * scale

                val leafPath = Path()
                leafPath.moveTo(bx, branchY - 3 * scale)
                leafPath.quadTo((bx + lx) / 2, (branchY - 3 * scale + ly) / 2 - 1.5f * scale, lx, ly)
                canvas.drawPath(leafPath, paint)
            }
        }
    }

    /**
     * 绘制松柏：主干 + 针叶簇
     */
    private fun drawPine(canvas: Canvas, x: Float, y: Float, scale: Float, level: Int, opacity: Int, density: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = opacity
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val h = 30 * scale

        // 主干
        paint.color = INK_DARK
        paint.strokeWidth = 1.5f * scale
        val trunkPath = Path()
        trunkPath.moveTo(x, y)
        trunkPath.quadTo(x + 0.5f, y - h * 0.4f, x - 0.3f, y - h)
        canvas.drawPath(trunkPath, paint)

        // 针叶簇
        paint.color = INK_MEDIUM
        paint.strokeWidth = 0.5f * scale
        val layers = minOf(level + 2, 5)
        for (l in 0 until layers) {
            for (i in 0 until density) {
                val ly = y - (h * 0.85f / layers) * (l + 0.5f)
                val angle = (i.toFloat() / density) * Math.PI + l * 0.5
                val len = (6 + l * 1.5f) * scale
                val ex = x + cos(angle).toFloat() * len
                val ey = ly + sin(angle).toFloat() * len * 1.5f

                val needlePath = Path()
                needlePath.moveTo(x + cos(angle).toFloat() * 1.5f * scale, ly)
                needlePath.quadTo((x + ex) / 2, (ly + ey) / 2 - 2f * scale, ex, ey)
                canvas.drawPath(needlePath, paint)
            }
        }
    }

    /**
     * 绘制草叶：飘逸的弧形叶片
     */
    private fun drawGrass(canvas: Canvas, x: Float, y: Float, scale: Float, level: Int, opacity: Int, density: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = opacity
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val count = maxOf(0, minOf(level + 2, 5) - density)
        if (count <= 0) return

        for (i in 0 until count) {
            val side = (i - (count - 1) / 2f) * 4 * scale
            val bladeH = (12 + i * 3) * scale
            val curveDir = if (i % 2 == 0) 1f else -0.8f

            paint.color = if (i == 0 || i == count - 1) INK_LIGHT else INK_MEDIUM
            paint.strokeWidth = 0.7f * scale

            val path = Path()
            path.moveTo(x + side * 0.3f, y)
            path.quadTo(x + side * 0.5f + curveDir * 3 * scale, y - bladeH * 0.6f, x + side + curveDir * 5 * scale, y - bladeH)
            canvas.drawPath(path, paint)
        }
    }

    /**
     * 绘制花卉：花茎 + 花瓣 + 花蕊
     */
    private fun drawFlower(canvas: Canvas, x: Float, y: Float, scale: Float, level: Int, opacity: Int, density: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = opacity
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val petalCount = maxOf(0, minOf(level + 3, 8) - density * 2)
        val h = 20 * scale

        // 花茎
        paint.color = INK_MEDIUM
        paint.strokeWidth = 0.6f * scale
        val stemPath = Path()
        stemPath.moveTo(x, y)
        stemPath.quadTo(x + 1f, y - h * 0.6f, x, y - h)
        canvas.drawPath(stemPath, paint)

        if (petalCount > 0) {
            val cx = x
            val cy = y - h
            val r = (4 + level * 1.2f) * scale

            // 花瓣
            paint.style = Paint.Style.FILL
            paint.color = INK_WASH
            for (i in 0 until petalCount) {
                val angle = (i.toFloat() / petalCount) * Math.PI * 2
                val px = cx + cos(angle).toFloat() * r * 0.5f
                val py = cy + sin(angle).toFloat() * r * 0.5f

                canvas.save()
                canvas.rotate((angle * 180 / Math.PI).toFloat(), px, py)
                val oval = RectF(
                    px - r * 0.45f, py - r * 0.55f,
                    px + r * 0.45f, py + r * 0.55f
                )
                canvas.drawOval(oval, paint)
                canvas.restore()
            }

            // 花瓣边框
            paint.style = Paint.Style.STROKE
            paint.color = INK_LIGHT
            paint.strokeWidth = 0.3f * scale
            for (i in 0 until petalCount) {
                val angle = (i.toFloat() / petalCount) * Math.PI * 2
                val px = cx + cos(angle).toFloat() * r * 0.5f
                val py = cy + sin(angle).toFloat() * r * 0.5f

                canvas.save()
                canvas.rotate((angle * 180 / Math.PI).toFloat(), px, py)
                val oval = RectF(
                    px - r * 0.45f, py - r * 0.55f,
                    px + r * 0.45f, py + r * 0.55f
                )
                canvas.drawOval(oval, paint)
                canvas.restore()
            }

            // 花蕊
            paint.style = Paint.Style.FILL
            paint.color = INK_DARK
            canvas.drawCircle(cx, cy, 1.2f * scale, paint)
            paint.color = Color.parseColor("#F4F1EA")
            canvas.drawCircle(cx, cy, 0.6f * scale, paint)
        }
    }

    /**
     * 绘制藤蔓：蜿蜒的藤条 + 小叶
     */
    private fun drawVine(canvas: Canvas, x: Float, y: Float, scale: Float, level: Int, opacity: Int, density: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = opacity
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val h = 25 * scale

        // 主干藤（S形曲线）
        paint.color = INK_DARK
        paint.strokeWidth = 0.8f * scale
        val vinePath = Path()
        vinePath.moveTo(x, y)
        vinePath.cubicTo(
            x + 3 * scale, y - h * 0.7f,
            x - 3 * scale, y - h * 0.4f,
            x, y - h
        )
        canvas.drawPath(vinePath, paint)

        // 侧枝 + 小叶
        paint.color = INK_LIGHT
        paint.strokeWidth = 0.4f * scale
        for (i in 0 until density) {
            val t = ((i + 1).toFloat() / density) * h
            val side = if (i % 2 == 0) 1f else -1f
            val bx = x + side * (3 + i * 2) * scale
            val by = y - t

            val branchPath = Path()
            branchPath.moveTo(x + side * 1 * scale, by)
            branchPath.quadTo((x + bx) / 2 + side * 2 * scale, by - 2 * scale, bx, by - 3 * scale)
            canvas.drawPath(branchPath, paint)

            // 小叶子
            paint.style = Paint.Style.FILL
            paint.color = INK_WASH
            for (j in 0..2) {
                val lx = bx + (j - 1) * 2 * scale
                val ly = by - 3 * scale + j * 1.5f * scale
                canvas.drawCircle(lx, ly, 1.2f * scale, paint)
            }
        }
    }

    /**
     * 绘制阔叶：宽大的叶片 + 叶脉
     */
    private fun drawBroadleaf(canvas: Canvas, x: Float, y: Float, scale: Float, level: Int, opacity: Int, density: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = opacity
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val count = maxOf(0, minOf(level + 1, 4) - density)
        if (count <= 0) return

        for (i in 0 until count) {
            val angle = ((i - (count - 1) / 2f) * Math.PI / 6).toFloat()
            val leafH = (16 + level * 2) * scale
            val leafW = (8 + level * 1.5f) * scale
            val ex = x + sin(angle) * leafW * 0.5f
            val ey = y - leafH * 0.5f + kotlin.math.abs(sin(angle)) * leafH * 0.3f

            // 叶柄
            paint.color = INK_MEDIUM
            paint.strokeWidth = 0.5f * scale
            paint.style = Paint.Style.STROKE
            val stemPath = Path()
            stemPath.moveTo(x, y)
            stemPath.quadTo((x + ex) / 2, y - leafH * 0.2f, ex, ey)
            canvas.drawPath(stemPath, paint)

            // 叶面
            paint.style = Paint.Style.FILL
            paint.color = INK_WASH
            canvas.save()
            canvas.rotate((angle * 180 / Math.PI).toFloat() - 20f, ex, ey - leafH * 0.35f)
            val leafOval = RectF(
                ex - leafW * 0.5f, ey - leafH * 0.35f - leafH * 0.4f,
                ex + leafW * 0.5f, ey - leafH * 0.35f + leafH * 0.4f
            )
            canvas.drawOval(leafOval, paint)
            canvas.restore()

            // 主叶脉
            paint.style = Paint.Style.STROKE
            paint.color = INK_LIGHT
            paint.strokeWidth = 0.3f * scale
            val veinPath = Path()
            veinPath.moveTo(ex, ey)
            veinPath.quadTo(ex, ey - leafH * 0.2f, ex, ey - leafH * 0.7f)
            canvas.drawPath(veinPath, paint)
        }
    }
}
