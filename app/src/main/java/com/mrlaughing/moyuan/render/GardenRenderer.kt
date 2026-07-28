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
    // --- 园圃布局（外部可修改） ---
    var gridCols: Int = 3
    var gridRows: Int = 3

    private const val PLOT_GAP = 8f
    private const val PLOT_MARGIN_RATIO = 0.05f
    private const val PLOT_TOP_OFFSET = 0.36f
    private const val PLOT_BOTTOM_OFFSET = 0.82f

    private data class PlotLayout(val startX: Float, val plotW: Float, val plotH: Float)
    private fun computePlotLayout(totalWidth: Float, useCols: Int, totalHeight: Float, useRows: Int): PlotLayout {
        val marginH = totalWidth * PLOT_MARGIN_RATIO
        val availableW = totalWidth - 2 * marginH
        val plotW = (availableW - PLOT_GAP * (useCols - 1)) / useCols
        val startX = marginH
        val startY = totalHeight * PLOT_TOP_OFFSET + 6f
        val endY = totalHeight * PLOT_BOTTOM_OFFSET
        val availableH = endY - startY - PLOT_GAP * (useRows - 1)
        val plotH = availableH / useRows
        return PlotLayout(startX, plotW, plotH)
    }

    // --- 2.5D PNG 位图缓存 ---
    private val plantPngCache = mutableMapOf<String, Bitmap>()
    fun setPlantPng(name: String, bitmap: Bitmap) { plantPngCache[name] = bitmap }
    fun loadPlantPngFromResource(res: android.content.res.Resources, name: String, resId: Int) {
        val bmp = BitmapFactory.decodeResource(res, resId)
        if (bmp != null) plantPngCache[name] = bmp
    }
    fun clearPlantPngCache() { plantPngCache.clear(); seasonPngCache.clear() }

    // --- 四季背景 PNG 缓存 ---
    private val seasonPngCache = mutableMapOf<Season, Bitmap>()
    fun setSeasonPng(season: Season, bitmap: Bitmap) { seasonPngCache[season] = bitmap }

    fun isAssetCacheReady(): Boolean {
        return plantPngCache.size >= 50 && seasonPngCache.size == Season.entries.size
    }
    fun calculateGridPositions(plantCount: Int, viewWidth: Int, viewHeight: Int): List<PlantRenderInfo> {
        if (plantCount == 0 || viewWidth <= 0 || viewHeight <= 0) return emptyList()

        val cells = GardenLayout.calculate(gridCols, gridRows, viewWidth, viewHeight)
            .sortedBy { it.fillRank }
            .take(plantCount.coerceAtMost(gridCols * gridRows))
        return cells.map { cell ->
                PlantRenderInfo(
                    bitmap = null,
                    x = cell.centerX,
                    y = cell.centerY,
                    scale = cell.tileSize * 0.60f / 10f,
                    plantId = -1L
                )
        }
    }
    /** 绘制植物 PNG 位图 */
    fun drawPlantBitmap(canvas: Canvas, bmp: Bitmap, x: Float, y: Float, scale: Float, alpha: Int = 255) {
        val size = 10f * scale
        val left = x - size / 2f
        val top = y - size * 0.80f
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
            this.alpha = alpha
        }
        canvas.drawBitmap(
            bmp,
            Rect(0, 0, bmp.width, bmp.height),
            RectF(left, top, left + size, top + size),
            bitmapPaint
        )
    }

    /** 内部：按条件绘制天气装饰元素 */
    private fun drawWeatherDecorations(canvas: Canvas, width: Int, height: Int, weather: Weather, season: Season, currentHour: Int, moonPhase: Float = 0.5f) {
        val w = width.toFloat(); val h = height.toFloat()
        val isClearOrCloudy = weather == Weather.CLEAR || weather == Weather.CLOUDY

        // 太阳 | 晴/多云天 6:00~16:59（避免 17:00 与落日同帧重叠）
        val showSun = isClearOrCloudy && currentHour >= 6 && currentHour < 17
        if (showSun) drawSun(canvas, w, h, currentHour, weather)

        // 落日 | 17:00~19:29（对齐原型 h>=17 && h<19.5，整数小时即 17/18/19）
        val showSunset = currentHour >= 17 && currentHour < 20
        if (showSunset) drawSunset(canvas, w, h, currentHour)

        // 月亮 | 夜 20:00~04:59（对齐原型 h>=19.5 || h<5.5，整数小时即 {0..5,20..23}）
        val showMoon = currentHour >= 20 || currentHour < 6
        if (showMoon) drawMoon(canvas, w, h, moonPhase)

        // 飞鸟 / 花瓣 / 落叶 / 萤火虫：逐帧动画，改由 drawAtmosphere 在 onDraw 中调用
    }

    /** 公开 API：供外部调用的装饰绘制入口 */
    fun drawWeatherAndDecorations(canvas: Canvas, width: Int, height: Int, weather: Weather, season: Season, currentHour: Int = java.time.LocalTime.now().hour, moonPhase: Float = 0.5f) {
        drawWeatherDecorations(canvas, width, height, weather, season, currentHour, moonPhase)
    }

    // ── 视觉辅助：颜色混合 / 变暗 / 昼夜亮度 ──
    private val INK = Color.parseColor("#2A2A2A") // 墨色（夜色混合用）
    // 夜空配色（移植 web 原型 SKY_KEYS 的 0:00 / 4:30 关键帧）
    private val NIGHT_TOP = Color.parseColor("#3A4250")
    private val NIGHT_HOR = Color.parseColor("#4A5258")
    private val GROUND_LIGHT = Color.parseColor("#F2EFE4")
    private fun mixColor(c1: Int, c2: Int, t: Float): Int {
        val r1 = Color.red(c1); val g1 = Color.green(c1); val b1 = Color.blue(c1)
        val r2 = Color.red(c2); val g2 = Color.green(c2); val b2 = Color.blue(c2)
        return Color.rgb(
            (r1 + (r2 - r1) * t).toInt().coerceIn(0, 255),
            (g1 + (g2 - g1) * t).toInt().coerceIn(0, 255),
            (b1 + (b2 - b1) * t).toInt().coerceIn(0, 255)
        )
    }
    private fun darken(c: Int, ratio: Float) = mixColor(c, INK, ratio)
    // 昼夜亮度关键帧（移植 web 原型 SKY_KEYS 第4列 light，0~24h）
    private val SKY_LIGHT_HOURS = floatArrayOf(0f, 4.5f, 6f, 7.5f, 12f, 16.5f, 18f, 19.3f, 20.5f, 24f)
    private val SKY_LIGHT_VALS = floatArrayOf(0.28f, 0.30f, 0.72f, 0.97f, 1.00f, 0.96f, 0.80f, 0.52f, 0.32f, 0.28f)
    private fun skyBrightness(hour: Int): Float {
        val h = ((hour % 24) + 24) % 24
        var i = 0
        while (i < SKY_LIGHT_HOURS.size - 1 && h > SKY_LIGHT_HOURS[i + 1]) i++
        val a = SKY_LIGHT_HOURS[i]
        val b = SKY_LIGHT_HOURS[Math.min(i + 1, SKY_LIGHT_HOURS.size - 1)]
        val t = if (b > a) (h - a) / (b - a) else 0f
        val va = SKY_LIGHT_VALS[i]
        val vb = SKY_LIGHT_VALS[Math.min(i + 1, SKY_LIGHT_VALS.size - 1)]
        return va + (vb - va) * t
    }
    // 夜边界（对齐原型 isNight：h>=19.5 || h<5.5，整数小时即 {0..5,20..23}）
    private fun isNightHour(hour: Int): Boolean = hour >= 20 || hour < 6
    private fun dayLight(hour: Int): Float = skyBrightness(hour)

    /** 太阳：缩小至满月大小 + 渐变光晕（对齐 web 原型 v3.4） */
    private fun drawSun(canvas: Canvas, w: Float, h: Float, currentHour: Int, weather: Weather) {
        val p = ((currentHour - 6).coerceIn(0, 11)) / 11f
        val cx = w * (0.80f + 0.06f * sin(p * Math.PI.toFloat()))
        val cy = h * (0.085f - 0.02f * sin(p * Math.PI.toFloat()))
        val dim = if (weather == Weather.CLOUDY || weather == Weather.OVERCAST) 0.55f else 1f
        val sf = w / 800f // 原型以 800 宽为基准
        // 外圈大气晕
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, 140f * sf,
                intArrayOf(Color.argb((0.55f * dim * 255).toInt(), 252, 228, 168),
                            Color.argb((0.30f * dim * 255).toInt(), 248, 216, 148),
                            Color.argb(0, 248, 216, 148)),
                floatArrayOf(0f, 0.18f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, 140f * sf, haloPaint)
        // 中圈柔光
        val midPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, 55f * sf,
                intArrayOf(Color.argb((0.95f * dim * 255).toInt(), 252, 236, 180),
                            Color.argb((0.55f * dim * 255).toInt(), 244, 212, 138),
                            Color.argb(0, 244, 212, 138)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, 55f * sf, midPaint)
        // 主体（饱和金黄）—— 缩小至满月大小
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((dim * 255).toInt(), 232, 180, 96) }
        canvas.drawCircle(cx, cy, 24f * sf, body)
        // 高光
        val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((0.95f * dim * 255).toInt(), 252, 240, 210) }
        canvas.drawCircle(cx - 7f * sf, cy - 7f * sf, 9f * sf, hi)
        // 暖色天空加亮（向下晕，顶/底 alpha→0）
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h * 0.04f, 0f, h * 0.22f,
                Color.argb((0.16f * dim * 255).toInt(), 248, 222, 160),
                Color.argb(0, 248, 222, 160), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, h * 0.04f, w, h * 0.22f, band)
    }

    /** 月亮：标准月相算法（外圆半周 + 半椭圆弧，凸向随 phase）—— 对齐 web 原型 v3.4f */
    private fun drawMoon(canvas: Canvas, w: Float, h: Float, moonPhase: Float) {
        val mx = w * 0.82f; val my = h * 0.085f; val r = w * 0.038f
        val breath = 0.92f
        // 光晕（收敛，避免糊掉月牙轮廓）
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(mx, my, 50f * (w / 800f),
                intArrayOf(Color.argb((0.30f * breath * 255).toInt(), 236, 232, 214),
                            Color.argb((0.10f * breath * 255).toInt(), 236, 232, 214),
                            Color.argb(0, 236, 232, 214)),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(mx, my, 50f * (w / 800f), glow)

        val phase = moonPhase.coerceIn(0f, 1f)
        val age = phase * 29.53f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.94f * breath * 255).toInt(), 232, 228, 206)
        }
        if (age > 0.5f && age < 29f) {
            val rx = r * kotlin.math.abs(cos(phase * 2 * Math.PI.toFloat()))
            val path = Path()
            path.moveTo(mx, my - r)
            if (kotlin.math.abs(phase - 0.5f) < 1e-3f) {
                // 满月：整圆
                path.addCircle(mx, my, r, Path.Direction.CW)
            } else if (phase < 0.5f) {
                // 盈月（亮在右）：外圆右半 + 椭圆弧
                path.arcTo(RectF(mx - r, my - r, mx + r, my + r), -90f, 180f, false)
                val sweep = if (phase < 0.25f) -180f else 180f
                path.arcTo(RectF(mx - rx, my - r, mx + rx, my + r), 90f, sweep, false)
            } else {
                // 亏月（亮在左）：外圆左半 + 椭圆弧
                path.arcTo(RectF(mx - r, my - r, mx + r, my + r), -90f, 180f, true)
                val sweep = if (phase < 0.75f) -180f else 180f
                path.arcTo(RectF(mx - rx, my - r, mx + rx, my + r), 90f, sweep, false)
            }
            path.close()
            canvas.drawPath(path, paint)
        } else {
            // 接近新月：画淡淡轮廓（仍可辨识月亮位置）
            paint.alpha = (0.18f * breath * 255).toInt()
            canvas.drawCircle(mx, my, r * 0.95f, paint)
        }
    }

    /** 落日：坐落在远山左峰，完整圆形 + 淡红晕（对齐 web 原型 v3.4） */
    private fun drawSunset(canvas: Canvas, w: Float, h: Float, currentHour: Int) {
        val p = ((currentHour - 17).coerceIn(0, 2)) / 2.5f
        val cx = w * 0.16f
        val cy = h * (0.145f + p * 0.05f)
        val r = w * 0.032f
        // 天空红晕
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 3.5f,
                intArrayOf(Color.argb(128, 224, 120, 88),
                            Color.argb(46, 230, 150, 100),
                            Color.argb(0, 230, 150, 100)),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r * 3.5f, glow)
        // 完整圆形落日
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(217, 214, 104, 74) }
        canvas.drawCircle(cx, cy, r, body)
        // 高光
        val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 240, 160, 110) }
        canvas.drawCircle(cx - r * 0.2f, cy - r * 0.25f, r * 0.4f, hi)
        // 淡淡光影（顶/底 alpha→0）
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h * 0.06f, 0f, h * 0.30f,
                intArrayOf(Color.argb(0, 232, 148, 96),
                    Color.argb((0.16f * (1 - p * 0.5f) * 255).toInt(), 232, 148, 96),
                    Color.argb(0, 232, 148, 96)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, h * 0.06f, w, h * 0.30f, band)
    }

    /** 飞鸟：小身体 + 上下拍动双翅，沿弧线斜掠爬升（对齐 web 原型 drawBirds，逐帧） */
    private fun drawBirds(canvas: Canvas, w: Float, h: Float, t: Float) {
        val cyc = 22f
        val p = (t % cyc) / cyc
        if (p > 0.38f) return
        val ease = p / 0.38f
        val bx = w * (-0.12f + ease * 1.24f)
        val by = h * (0.17f - ease * 0.07f - (kotlin.math.sin(ease * Math.PI.toFloat())) * 0.015f)
        val k = w / 800f * 2.2f
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#56524C"); alpha = 184 }
        val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.parseColor("#56524C"); alpha = 173; strokeCap = Paint.Cap.ROUND }
        val flock = listOf(Triple(0f, 0f, 1f), Triple(40f, 16f, 0.82f), Triple(-36f, 22f, 0.72f), Triple(74f, 34f, 0.58f))
        for ((idx, triad) in flock.withIndex()) {
            val (dx, dy, s) = triad
            val x = bx + dx * k
            val y = by + dy * k + (kotlin.math.sin(t * 3.2f + idx * 1.7f) * 2.5f) * k
            val flap = kotlin.math.sin(t * 9f + idx * 1.3f)
            val wing = 13f * s * k
            // 身体（小水滴形，头朝右）
            canvas.drawOval(RectF(x - 6.5f * s * k, y - 2.8f * s * k, x + 6.5f * s * k, y + 2.8f * s * k), body)
            // 头
            canvas.drawCircle(x + 6f * s * k, y - 2.4f * s * k, 2.4f * s * k, body)
            // 喙
            val beak = Path().apply {
                moveTo(x + 8.2f * s * k, y - 2.6f * s * k)
                lineTo(x + 10.6f * s * k, y - 1.9f * s * k)
                lineTo(x + 8.2f * s * k, y - 1.2f * s * k)
                close()
            }
            canvas.drawPath(beak, body)
            // 双翅（一上一下拍动）
            wingPaint.strokeWidth = 2.6f * s * k
            val leftWing = Path().apply {
                moveTo(x - 1f * s * k, y - 1f * s * k)
                quadTo(x - wing * 0.45f, y - wing * (0.55f + 0.5f * flap), x - wing * 1.05f, y - wing * (0.30f + 0.62f * flap))
            }
            canvas.drawPath(leftWing, wingPaint)
            val rightWing = Path().apply {
                moveTo(x + 1f * s * k, y - 1f * s * k)
                quadTo(x + wing * 0.30f, y - wing * (0.50f + 0.42f * flap), x + wing * 0.78f, y - wing * (0.22f + 0.55f * flap))
            }
            canvas.drawPath(rightWing, wingPaint)
            // 尾羽
            wingPaint.strokeWidth = 1.8f * s * k
            val tail = Path().apply {
                moveTo(x - 6f * s * k, y + 0.5f * s * k)
                lineTo(x - 10f * s * k, y + 2.6f * s * k)
            }
            canvas.drawPath(tail, wingPaint)
        }
    }

    /** 飘落花瓣（春）：随 t 飘落 + 旋转（对齐 web 原型 drawPetals，逐帧） */
    private fun drawFallingPetals(canvas: Canvas, w: Float, h: Float, t: Float) {
        val k = w / 800f
        val rng = java.util.Random(333)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#E8CFCB"); alpha = 153 }
        for (i in 0 until 16) {
            val x0 = rng.nextFloat() * w
            val sp = (56f + rng.nextFloat() * 36f) * k
            val ph = rng.nextFloat() * h
            val sway = (30f + rng.nextFloat() * 30f) * k
            val swp = rng.nextFloat() * 6.28f
            val sz = (7f + rng.nextFloat() * 7f) * k
            val rp = rng.nextFloat() * 6.28f
            val y = ((t * sp + ph) % (h * 0.92f)) + h * 0.04f
            val x = x0 + kotlin.math.sin(t * 0.7f + swp + y * 0.006f) * sway
            val rot = t * (0.8f + rng.nextFloat() * 0.8f) + rp
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(Math.toDegrees(rot.toDouble()).toFloat())
            val path = Path().apply {
                moveTo(0f, -sz * 0.5f)
                quadTo(-sz * 0.55f, -sz * 0.62f, -sz * 0.5f, -sz * 0.05f)
                quadTo(-sz * 0.5f, sz * 0.42f, 0f, sz * 0.55f)
                quadTo(sz * 0.5f, sz * 0.42f, sz * 0.5f, -sz * 0.05f)
                quadTo(sz * 0.55f, -sz * 0.62f, 0f, -sz * 0.5f)
                close()
            }
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }

    /** 落叶（秋）：随 t 飘落 + 摇曳（对齐 web 原型 drawLeaves，逐帧） */
    private fun drawFallingLeaves(canvas: Canvas, w: Float, h: Float, t: Float) {
        val k = w / 800f
        val rng = java.util.Random(444)
        val leafColors = intArrayOf(Color.parseColor("#C89A5E"), Color.parseColor("#B9854C"), Color.parseColor("#D2A968"))
        val vein = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.parseColor("#8C6438"); alpha = 128; strokeWidth = 1f * k }
        for (i in 0 until 14) {
            val x0 = rng.nextFloat() * w
            val sp = (66f + rng.nextFloat() * 44f) * k
            val ph = rng.nextFloat() * h
            val sway = (36f + rng.nextFloat() * 34f) * k
            val swp = rng.nextFloat() * 6.28f
            val sz = (8f + rng.nextFloat() * 8f) * k
            val y = ((t * sp + ph) % (h * 0.92f)) + h * 0.04f
            val x = x0 + kotlin.math.sin(t * 0.9f + swp + y * 0.007f) * sway
            val rot = kotlin.math.sin(t * 1.4f + swp) * 1.2f
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(Math.toDegrees(rot.toDouble()).toFloat())
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = leafColors[i % 3]; alpha = 158 }
            val path = Path().apply {
                moveTo(0f, -sz)
                quadTo(sz * 0.62f, -sz * 0.2f, 0f, sz * 0.7f)
                quadTo(-sz * 0.62f, -sz * 0.2f, 0f, -sz)
                close()
            }
            canvas.drawPath(path, fill)
            canvas.drawLine(0f, -sz * 0.7f, 0f, sz * 0.5f, vein)
            canvas.restore()
        }
    }

    /** 萤火虫（夏夜）：明灭 + 漂浮（对齐 web 原型 drawFireflies，逐帧） */
    private fun drawFireflies(canvas: Canvas, w: Float, h: Float, t: Float) {
        val k = w / 800f
        val rng = java.util.Random(888)
        for (i in 0 until 22) {
            val bx = w * (0.1f + rng.nextFloat() * 0.8f)
            val by = h * (0.50f + rng.nextFloat() * 0.42f)
            val ax = (26f + rng.nextFloat() * 44f) * k
            val ay = (16f + rng.nextFloat() * 26f) * k
            val p1 = rng.nextFloat() * 6.28f
            val p2 = rng.nextFloat() * 6.28f
            val fs = 0.25f + rng.nextFloat() * 0.4f
            val x = bx + (kotlin.math.sin(t * fs + p1) * ax) + (kotlin.math.sin(t * fs * 2.3f + p2) * ax * 0.3f)
            val y = by + (kotlin.math.cos(t * fs * 0.8f + p2) * ay)
            val glowBase = kotlin.math.abs(kotlin.math.sin(t * (0.7f + fs) + p1))
            val glow = (0.35f + 0.65f * Math.pow(glowBase.toDouble(), 3.0).toFloat()).coerceIn(0f, 1f)
            val r = 14f * k
            val g = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(x, y, r,
                    intArrayOf(
                        Color.argb((0.9f * glow * 255).toInt(), 238, 230, 164),
                        Color.argb((0.34f * glow * 255).toInt(), 226, 216, 140),
                        Color.argb(0, 226, 216, 140)),
                    floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(x, y, r, g)
            val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb((glow * 255).toInt(), 248, 244, 204) }
            canvas.drawCircle(x, y, 2.2f * k, core)
        }
    }

    /**
     * 逐帧大气粒子（飞鸟 / 花瓣 / 落叶 / 萤火虫），对齐 web 原型的主循环调度。
     * 由 View.onDraw 在绘制静态场景缓存之后调用，使这些元素真正"活"起来。
     */
    fun drawAtmosphere(canvas: Canvas, w: Float, h: Float, season: Season, weather: Weather, currentHour: Int, t: Float) {
        val noPrecip = weather == Weather.CLEAR || weather == Weather.CLOUDY
        val night = isNightHour(currentHour)
        // 飞鸟：晴/多云 白天 6:00~16:59
        if (noPrecip && currentHour >= 6 && currentHour < 17) drawBirds(canvas, w, h, t)
        // 花瓣：春季 + 非雨雪（对齐原型 noPrecip = CLEAR/CLOUDY）
        if (season == Season.SPRING && noPrecip) drawFallingPetals(canvas, w, h, t)
        // 落叶：秋季 + 非雨雪
        if (season == Season.AUTUMN && noPrecip) drawFallingLeaves(canvas, w, h, t)
        // 萤火虫：夏季 + 夜 + 非雨雪
        if (season == Season.SUMMER && night && noPrecip) drawFireflies(canvas, w, h, t)
    }

    /**
     * 逐帧天气粒子（雨/雪/风/雾/闪电/云），按 weather 分派 —— 对齐 web 原型的主循环调度。
     * 由 View.onDraw 在绘制静态场景缓存之后调用，使天气效果真正"活"起来。
     */
    fun drawWeatherParticles(canvas: Canvas, w: Float, h: Float, weather: Weather, t: Float) {
        when (weather) {
            Weather.CLEAR -> Unit
            Weather.CLOUDY, Weather.OVERCAST -> drawCloudEffect(canvas, w, h, t)
            Weather.DRIZZLE -> drawRainEffect(canvas, w, h, isDrizzle = true, t = t)
            Weather.RAIN -> drawRainEffect(canvas, w, h, isDrizzle = false, t = t)
            Weather.THUNDERSTORM -> { drawRainEffect(canvas, w, h, isDrizzle = false, t = t); drawLightningEffect(canvas, w, h, t) }
            Weather.SNOW -> drawSnowEffect(canvas, w, h, t)
            Weather.FOGGY -> drawFogEffect(canvas, w, h, t)
            Weather.WINDY -> drawWindEffect(canvas, w, h, t)
        }
    }


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
        plants: List<PlantRenderInfo>,
        currentHour: Int = java.time.LocalTime.now().hour,
        ambientPhase: Float = 0f
    ) {
        drawScene(canvas, width, height, season, weather, currentHour)
        drawPlants(canvas, plants, width, height, season, ambientPhase)
    }

    fun drawScene(
        canvas: Canvas,
        width: Int,
        height: Int,
        season: Season,
        weather: Weather,
        currentHour: Int = java.time.LocalTime.now().hour,
        moonPhase: Float = 0.5f
    ) {
        val colors = SEASON_COLORS[season] ?: SEASON_COLORS[Season.SPRING]!!
        val light = dayLight(currentHour)
        drawPremiumBackdrop(canvas, width, height, colors, currentHour, weather)
        // 天气粒子（雨/雪/风/雾/闪电/云）已从静态缓存移出，改由 View.onDraw 每帧调用 drawWeatherParticles
        drawMountainLayer(canvas, width.toFloat(), height.toFloat(), colors, isFarLayer = true, light = light)
        drawMountainLayer(canvas, width.toFloat(), height.toFloat(), colors, isFarLayer = false, light = light)
        // 轻微环境地面雾（顶/底 alpha→0，不压远山）
        drawAmbientFog(canvas, width.toFloat(), height.toFloat())
        drawWeatherDecorations(canvas, width, height, weather, season, currentHour, moonPhase)
        drawGardenLawns(canvas, width, height, season)
    }
    private fun drawPremiumBackdrop(
        canvas: Canvas,
        width: Int,
        height: Int,
        colors: SeasonColors,
        currentHour: Int = 12,
        weather: Weather = Weather.CLEAR
    ) {
        val w = width.toFloat()
        val h = height.toFloat()
        val bright = skyBrightness(currentHour)
        val dark = (1f - bright).coerceIn(0f, 1f)
        // 天空顶/中：白天用季节亮色，夜里向夜蓝压暗；并混入季节霞色 accent
        var topC = mixColor(colors.sky, NIGHT_TOP, dark)
        var horC = mixColor(colors.sky, NIGHT_HOR, dark * 0.92f)
        horC = mixColor(horC, colors.accent, 0.18f * bright)
        // 天气染色（对齐 web 原型 drawSky 的 RAIN/SNOW/FOG 处理）
        when (weather) {
            Weather.RAIN -> { topC = mixColor(topC, Color.parseColor("#6E7674"), 0.22f); horC = mixColor(horC, Color.parseColor("#7C8480"), 0.20f) }
            Weather.SNOW -> { topC = mixColor(topC, Color.parseColor("#C9CFD6"), 0.18f); horC = mixColor(horC, Color.parseColor("#D2D8DE"), 0.16f) }
            Weather.FOGGY -> { topC = mixColor(topC, Color.parseColor("#C9C9C0"), 0.25f); horC = mixColor(horC, Color.parseColor("#D2D2C8"), 0.28f) }
            else -> {}
        }
        val bottomC = mixColor(horC, GROUND_LIGHT, 0.45f)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(topC, horC, bottomC),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, background)

        // 地平线高光：白天明显、夜里随亮度收敛
        val horizon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f,
                h * 0.43f,
                w * 0.72f,
                intArrayOf(Color.argb((70 * bright).toInt().coerceIn(0, 255), 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, h * 0.12f, w, h * 0.82f, horizon)

        // 夜空星星（静态，位置由种子确定；对齐原型夜支 26 颗）
        if (isNightHour(currentHour)) drawStars(canvas, w, h)
    }

    private fun drawStars(canvas: Canvas, w: Float, h: Float) {
        val rng = java.util.Random(42)
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val base = w / 800f
        for (i in 0 until 26) {
            val sx = rng.nextFloat() * w
            val sy = rng.nextFloat() * h * 0.30f
            val sz = (0.8f + rng.nextFloat() * 1.4f) * base
            val a = 0.30f + rng.nextFloat() * 0.45f
            starPaint.color = Color.argb((a * 255).toInt(), 238, 236, 222)
            canvas.drawCircle(sx, sy, sz, starPaint)
        }
    }

    private fun drawGardenLawns(canvas: Canvas, width: Int, height: Int, season: Season) {
        val bitmap = seasonPngCache[season]?.takeUnless { it.isRecycled } ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
            alpha = 242
        }
        GardenLayout.calculate(gridCols, gridRows, width, height)
            .sortedWith(compareBy<GardenCell> { it.depth }.thenBy { it.centerX })
            .forEach { cell ->
                val left = cell.centerX - cell.tileSize / 2f
                val top = cell.centerY - cell.tileSize / 2f
                canvas.drawBitmap(
                    bitmap,
                    Rect(0, 0, bitmap.width, bitmap.height),
                    RectF(left, top, left + cell.tileSize, top + cell.tileSize),
                    paint
                )
            }
    }
    // (drawBackgroundLayer 已移除：远山改为由 drawScene 直接调用 drawMountainLayer，
    //  避免与 drawPremiumBackdrop 重复绘制背景；天气粒子已移至 View.onDraw 逐帧调用 drawWeatherParticles)

    /** 远山：噪声错落山脊 + 竖向渐变填充(顶/底 alpha→0) + 山脚薄雾 + 山脊描边 —— 对齐 web 原型 v3.4k */
    private fun drawMountainLayer(canvas: Canvas, w: Float, h: Float, colors: SeasonColors, isFarLayer: Boolean, light: Float) {
        val peak: Float; val amp: Float; val baseColor: Int; val layerAlpha: Float
        if (isFarLayer) {
            peak = h * 0.22f; amp = h * 0.07f; baseColor = colors.mountainFar
            layerAlpha = 0.38f + 0.06f * (1f - light)
        } else {
            peak = h * 0.27f; amp = h * 0.06f; baseColor = colors.mountain
            layerAlpha = 0.55f + 0.07f * (1f - light)
        }
        val seed = if (isFarLayer) 17f else 41f
        val baseY = h * 0.40f
        val groundBlend = baseY + h * 0.05f

        // 带符号多频噪声山脊（错落有致：长波大势 + 3/5 峰拍频 + 8 细节，非 abs 翻转）
        val segs = 20
        val pts = ArrayList<PointF>(segs + 1)
        for (i in 0..segs) {
            val px = (i.toFloat() / segs) * w
            val u = i.toFloat() / segs
            val TAU = 2f * Math.PI.toFloat()
            val noise = (
                sin(u * TAU * 1f + seed) * 0.22f +
                    sin(u * TAU * 3f + seed * 1.3f) * 0.38f +
                    sin(u * TAU * 5f + seed * 2.1f) * 0.25f +
                    sin(u * TAU * 8f + seed * 2.7f) * 0.15f
                )
            val n = ((noise + 1f) * 0.5f).coerceIn(0f, 1f)
            pts.add(PointF(px, peak - amp * (0.08f + 0.95f * n)))
        }

        val cr = Color.red(baseColor); val cg = Color.green(baseColor); val cb = Color.blue(baseColor)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        fillPaint.shader = LinearGradient(0f, peak - amp, 0f, groundBlend + h * 0.04f,
            intArrayOf(Color.argb(0, cr, cg, cb),
                Color.argb((layerAlpha * 255).toInt(), cr, cg, cb),
                Color.argb(0, cr, cg, cb)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        val footAmp = h * 0.035f
        fun footY(u: Float) = groundBlend + (sin(u * 2.3f + seed * 0.5f) * 0.7f + sin(u * 4.7f + seed * 1.3f) * 0.30f) * footAmp

        // 填充山体（山脊 quad 平滑 + 山脚低频起伏，与地平线自然衔接）
        val path = Path()
        for (i in pts.indices) {
            val cur = pts[i]
            if (i == 0) path.moveTo(cur.x, cur.y)
            else {
                val prev = pts[i - 1]
                val mxC = (prev.x + cur.x) / 2f
                path.quadTo(mxC, (if (prev.y < cur.y) prev.y else cur.y) - amp * 0.05f, cur.x, cur.y)
            }
        }
        for (i in pts.lastIndex downTo 0) {
            path.lineTo(pts[i].x, footY(i.toFloat() / segs))
        }
        path.close()
        canvas.drawPath(path, fillPaint)

        // 清晰秀美的山脊描边（仅顶部轮廓，不影响山脚与地平线衔接）
        val ridge = Path()
        for (i in pts.indices) {
            val cur = pts[i]
            if (i == 0) ridge.moveTo(cur.x, cur.y)
            else {
                val prev = pts[i - 1]
                val mxC = (prev.x + cur.x) / 2f
                ridge.quadTo(mxC, (if (prev.y < cur.y) prev.y else cur.y) - amp * 0.05f, cur.x, cur.y)
            }
        }
        val ridgeW = (h * 0.001f).coerceAtLeast(1.1f)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = ridgeW; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            color = darken(baseColor, 0.30f); alpha = (0.55f * 255).toInt()
        }
        canvas.drawPath(ridge, strokePaint)

        // 山脚薄雾：顶/底 alpha→0（柔化山脚与下方空白带的衔接，无水平硬边）
        val hzTop = baseY - h * 0.015f
        val hzBot = baseY + h * 0.14f
        val hz = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, hzTop, 0f, hzBot,
                intArrayOf(Color.argb(0, 243, 240, 231),
                    Color.argb(((0.18f + 0.05f * light) * 255).toInt(), 243, 240, 231),
                    Color.argb(0, 243, 240, 231)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, hzTop, w, hzBot - hzTop, hz)
    }

    /** 轻微环境地面雾（顶/底 alpha→0，仅轻覆花园底部，不压远山）—— 对齐 web 原型 v3.4i */
    private fun drawAmbientFog(canvas: Canvas, w: Float, h: Float) {
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h * 0.55f, 0f, h,
                intArrayOf(Color.argb(0, 228, 228, 219),
                    Color.argb((0.12f * 255).toInt(), 228, 228, 219),
                    Color.argb((0.18f * 255).toInt(), 228, 228, 219)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, h * 0.55f, w, h * 0.45f, base)
    }

    // ── 蝴蝶（春/夏/秋 昼间，侧视飞行 + 停留状态机）—— 对齐 web 原型 v3.4i/k ──
    private data class ButterflyState(
        var mode: String = "fly",
        var until: Float = 0f,
        var px: Float = 0f,
        var py: Float = 0f,
        var amt: Float = 0f,
        var dir: Float = 1f
    )
    private val bfState = List(2) { i -> ButterflyState(until = 3f + i * 2.5f) }

    /** 蝴蝶：在 GardenRendererView.onDraw 中逐帧调用（背景场景为静态缓存），t 为秒。
     *  造型：四翅对称（上下左右），从身体向斜上/斜下展开；振翅 = y 方向缩放。
     *  状态机：飞行 ↔ 停留 平滑过渡。 */
    fun drawButterflies(canvas: Canvas, w: Float, h: Float, season: Season, weather: Weather, t: Float,
                        restTargets: List<Pair<Float, Float>> = emptyList()) {
        if (weather == Weather.RAIN || weather == Weather.DRIZZLE || weather == Weather.SNOW ||
            weather == Weather.THUNDERSTORM || weather == Weather.FOGGY) return
        val cols = if (season == Season.SPRING) arrayOf("#F2A8C4", "#E090B4") else arrayOf("#8CC0F0", "#6FA8E0")
        val k = (w / 720f).coerceIn(1.0f, 1.30f) * 1.05f
        for (i in 0..1) {
            val st = bfState[i]
            // 飞行：频率更低 → 整体更慢、更悠游
            val fx = w * (0.30f + 0.40f * i) + sin(t * 0.28f + i * 2.4f) * w * 0.13f + sin(t * 0.8f + i) * 10f * (w / 800f)
            val fy = h * (0.50f + 0.05f * i) + cos(t * 0.40f + i * 1.8f) * h * 0.045f
            if (st.mode == "fly") {
                if (t > st.until) {
                    st.mode = "rest"; st.until = t + 1.0f + kotlin.random.Random.nextFloat() * 0.6f
                    // 停在某一株植物上（取植物坐标；无植物则落在花园区域）
                    if (restTargets.isNotEmpty()) {
                        val (tx, ty) = restTargets[kotlin.random.Random.nextInt(restTargets.size)]
                        st.px = tx
                        st.py = ty - h * 0.03f
                    } else {
                        st.px = w * (0.22f + 0.56f * kotlin.random.Random.nextFloat())
                        st.py = h * (0.55f + 0.12f * kotlin.random.Random.nextFloat())
                    }
                    st.dir = if (fx < st.px) 1f else -1f
                }
            } else {
                if (t > st.until) { st.mode = "fly"; st.until = t + 3f + kotlin.random.Random.nextFloat() * 4f }
            }
            val target = if (st.mode == "rest") 1f else 0f
            st.amt += (target - st.amt) * 0.07f
            if (st.amt < 0.001f) st.amt = 0f else if (st.amt > 0.999f) st.amt = 1f
            val x = fx + (st.px - fx) * st.amt
            val y = fy + (st.py - fy) * st.amt
            val dir = if (st.mode == "rest") st.dir else if (cos(t * 0.28f + i * 2.4f) > 0) 1f else -1f
            val fly = 1f - st.amt
            val flap = 0.5f + 0.5f * sin(t * 9f + i * 2.7f)              // [0..1] 振翅

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(0.10f * (0.4f + 0.6f * fly))
            canvas.scale(dir * k, k)

            val wingCol = Color.parseColor(cols[i % 2])
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(235, 244, 239, 224) }   // 浅米色、近乎不透明，身子换浅色
            val nearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wingCol; alpha = 255 }            // 近翅：实色不淡
            val farPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wingCol; alpha = 215 }            // 远翅：加实，不再发淡
            val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(210, 255, 255, 255) }
            val antPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 150, 140, 124)
                style = Paint.Style.STROKE; strokeWidth = 0.7f; strokeCap = Paint.Cap.ROUND
            }

            // 翅膀开合：飞行时 y 方向随振翅 [0.55..1.0]；停留时翅膀收拢（像停栖）
            val wingY = if (fly > 0.5f) (0.55f + 0.45f * flap) else (0.80f - 0.42f * st.amt)
            val wingX = if (fly > 0.5f) 1f else (1f - 0.12f * st.amt)

            // 触角（先画，避免被翅膀盖住）—— 缩短、变细
            canvas.drawLine(-5f, -0.5f, -8.5f, -4.5f, antPaint)
            canvas.drawLine(-5f,  0.5f, -8.5f,  4.5f, antPaint)

            // 上左翅（远翅，淡）：身体左上，向外斜展（中心 x=-7，y=-3.5）
            canvas.save()
            canvas.scale(wingX, wingY)
            canvas.rotate(-0.20f)
            canvas.drawOval(RectF(-14f, -7.5f, 0f, 0.5f), farPaint)
            canvas.restore()

            // 下左翅（远翅，淡，小）：身体左下（中心 x=-5，y=3.5）
            canvas.save()
            canvas.scale(wingX * 0.78f, wingY * 0.82f)
            canvas.rotate(0.18f)
            canvas.drawOval(RectF(-10f, 0.5f, 0f, 6f), farPaint)
            canvas.restore()

            // 上右翅（近翅，实）：身体右上（中心 x=+7，y=-3.5）
            canvas.save()
            canvas.scale(wingX, wingY)
            canvas.rotate(0.20f)
            canvas.drawOval(RectF(0f, -7.5f, 14f, 0.5f), nearPaint)
            canvas.drawCircle(4f, -3f, 1.8f, spotPaint)                   // 翅斑
            canvas.restore()

            // 下右翅（近翅，实，小）：身体右下（中心 x=+5，y=3.5）
            canvas.save()
            canvas.scale(wingX * 0.78f, wingY * 0.82f)
            canvas.rotate(-0.18f)
            canvas.drawOval(RectF(0f, 0.5f, 10f, 6f), nearPaint)
            canvas.restore()

            // 身体（缩短的纺锤，-x 是头）+ 头（在翅膀之后画，确保在中央清晰可见）
            canvas.drawOval(RectF(-5f, -1.2f, 5f, 1.2f), bodyPaint)
            canvas.drawCircle(-4.5f, 0f, 1.4f, bodyPaint)

            canvas.restore()
        }
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
            Pair(w * 0.15f, h * 0.15f),
            Pair(w * 0.4f, h * 0.22f),
            Pair(w * 0.65f, h * 0.18f),
            Pair(w * 0.8f, h * 0.28f)
        )
        leafPositions.forEach { (x, y) ->
            canvas.drawCircle(x, y, 1.8f, paint)
        }
    }

    private fun drawWinterSnowCap(canvas: Canvas, w: Float, h: Float, colors: SeasonColors) {
        // 白色雪山盖在远山之上（底部h*0.12，在园圃之上）
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = SNOW_COLOR; alpha = 153
        }
        val path = Path()
        path.moveTo(0f, h * 0.14f)
        path.quadTo(w * 0.15f, h * 0.05f, w * 0.25f, h * 0.10f)
        path.quadTo(w * 0.4f, h * 0.04f, w * 0.55f, h * 0.09f)
        path.quadTo(w * 0.7f, h * 0.05f, w * 0.85f, h * 0.10f)
        path.quadTo(w, h * 0.06f, w, h * 0.12f)
        path.lineTo(w, h * 0.14f)
        path.quadTo(w * 0.8f, h * 0.12f, w * 0.6f, h * 0.14f)
        path.quadTo(w * 0.4f, h * 0.11f, w * 0.2f, h * 0.13f)
        path.quadTo(w * 0.1f, h * 0.12f, 0f, h * 0.14f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawRainEffect(canvas: Canvas, w: Float, h: Float, isDrizzle: Boolean, t: Float) {
        // 雨滴：sin-hash 替代模排列，每滴独立速度/角度/长度/透明度 → 打散锁步平行线
        fun hash(n: Int, salt: Float): Float {
            val v = sin(n * 12.9898f + salt * 78.233f) * 43758.5453f
            return v - kotlin.math.floor(v)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A534C")
            strokeWidth = if (isDrizzle) 2.6f else 4.0f
            strokeCap = Paint.Cap.ROUND
        }
        val count = if (isDrizzle) 55 else 70
        val fallRange = h * 0.92f
        val baseSpeed = if (isDrizzle) 230f else 360f
        val baseLen = if (isDrizzle) 30f else 45f
        val baseAlpha = if (isDrizzle) 160 else 210
        for (i in 0 until count) {
            val r1 = hash(i, 1.0f)            // [0,1) x 起点
            val r2 = hash(i, 2.0f)            // [0,1) 相位
            val r3 = hash(i, 3.0f)            // [0,1) 长度抖动
            val r4 = hash(i, 4.0f)            // [0,1) 速度抖动
            val r5 = hash(i, 5.0f)            // [0,1) 角度抖动
            val r6 = hash(i, 6.0f)            // [0,1) alpha 抖动
            val speed = (0.55f + r4 * 0.90f) * baseSpeed   // 0.55x ~ 1.45x
            val len = (0.65f + r3 * 0.55f) * baseLen       // 0.65x ~ 1.20x
            val ang = 0.12f + (r5 - 0.5f) * 0.06f          // ±0.03 rad
            val sinA = sin(ang)
            val cosA = cos(ang)
            val phase = r2 * fallRange
            val y = ((t * speed + phase) % fallRange) + h * 0.02f
            val driftX = sin(t * 1.2f + i * 0.7f) * (3f + r4 * 4f)
            val x = r1 * w + driftX
            paint.alpha = (baseAlpha * (0.55f + r6 * 0.55f)).toInt().coerceIn(0, 255)
            canvas.drawLine(x, y, x + sinA * len, y + cosA * len, paint)
        }
    }

    private fun drawLightningEffect(canvas: Canvas, w: Float, h: Float, t: Float) {
        // 闪电：周期性闪烁 —— 每 3.5s 闪一次，持续 ~200ms（前 60ms 渐亮，后 140ms 渐熄）
        val cycle = 3.5f
        val dur = 0.20f
        val phase = t % cycle
        if (phase > dur) return
        val brightness = if (phase < 0.06f) phase / 0.06f else (1f - (phase - 0.06f) / (dur - 0.06f)).coerceIn(0f, 1f)
        val b = brightness
        // 缩短的雷电：从天上劈下，2-3个折角，更垂直
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.parseColor("#FFF5CC")
            strokeWidth = 7.0f; alpha = (200 * b).toInt().coerceIn(0, 255); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val branchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.parseColor("#FFE880")
            strokeWidth = 3.0f; alpha = (140 * b).toInt().coerceIn(0, 255); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.parseColor("#FFFFFF")
            strokeWidth = 2.5f; alpha = (220 * b).toInt().coerceIn(0, 255); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.parseColor("#FFE880")
            strokeWidth = 1.5f; alpha = (100 * b).toInt().coerceIn(0, 255); strokeCap = Paint.Cap.ROUND
        }

        // 闪电1（左侧主闪）：3个折角，减小水平摆动
        val b1 = Path()
        b1.moveTo(w * 0.20f, h * 0.04f)
        b1.lineTo(w * 0.18f, h * 0.08f)
        b1.lineTo(w * 0.22f, h * 0.07f)
        b1.lineTo(w * 0.17f, h * 0.13f)
        b1.lineTo(w * 0.21f, h * 0.12f)
        b1.lineTo(w * 0.16f, h * 0.19f)
        canvas.drawPath(b1, mainPaint)
        canvas.drawPath(b1, corePaint)
        // 分叉（短促）
        val br1 = Path(); br1.moveTo(w * 0.21f, h * 0.12f); br1.lineTo(w * 0.25f, h * 0.14f)
        canvas.drawPath(br1, branchPaint)
        val br2 = Path(); br2.moveTo(w * 0.17f, h * 0.13f); br2.lineTo(w * 0.14f, h * 0.16f)
        canvas.drawPath(br2, tipPaint)

        // 闪电2（右侧）：2个折角，减小水平摆动
        val b2 = Path()
        b2.moveTo(w * 0.75f, h * 0.04f)
        b2.lineTo(w * 0.72f, h * 0.08f)
        b2.lineTo(w * 0.77f, h * 0.07f)
        b2.lineTo(w * 0.73f, h * 0.13f)
        canvas.drawPath(b2, mainPaint)
        canvas.drawPath(b2, corePaint)
        val br3 = Path(); br3.moveTo(w * 0.72f, h * 0.08f); br3.lineTo(w * 0.76f, h * 0.10f)
        canvas.drawPath(br3, tipPaint)

        // 闪电3（左上角小闪）：2个折角
        val b3 = Path()
        b3.moveTo(w * 0.06f, h * 0.04f)
        b3.lineTo(w * 0.05f, h * 0.07f)
        b3.lineTo(w * 0.08f, h * 0.06f)
        b3.lineTo(w * 0.05f, h * 0.10f)
        canvas.drawPath(b3, tipPaint)
        canvas.drawPath(b3, corePaint)

        // 光晕
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor("#FFF8DC"); alpha = 15
        }
        canvas.drawCircle(w * 0.19f, h * 0.12f, w * 0.035f, glowPaint)
        canvas.drawCircle(w * 0.74f, h * 0.08f, w * 0.025f, glowPaint)
    }

    private fun drawSnowEffect(canvas: Canvas, w: Float, h: Float, t: Float) {
        // 雪花：随 t 下落 + 横向 sin 摆动；后三组（园圃/边缘/地面积雪斑块）为装饰积雪位置固定
        val fallRange = h * 0.88f
        val speedSmall = 70f
        val speedBig = 55f
        val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#FFFFFF"); alpha = 200 }
        for (i in 0 until 120) {
            val seedX = ((i * 41 + 13) % 97) / 97f
            val seedPhase = ((i * 89 + 5) % 97) / 97f * fallRange
            val seedSway = 14f + ((i * 37 + 19) % 30)
            val seedR = 0.6f + ((i * 17 + 3) % 14) / 10f
            val r = 2.5f + seedR * 2.5f
            val y = ((t * speedSmall + seedPhase) % fallRange) + h * 0.04f
            val x = seedX * w + sin(t * 1.1f + i * 0.45f) * seedSway
            canvas.drawCircle(x, y, r, snowPaint)
        }
        val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#FFFFFF"); alpha = 160 }
        for (i in 0 until 30) {
            val seedX = ((i * 67 + 29) % 89) / 89f
            val seedPhase = ((i * 53 + 11) % 89) / 89f * fallRange
            val seedSway = 20f + ((i * 19 + 7) % 30)
            val seedR = 0.5f + ((i * 23 + 5) % 18) / 10f
            val r = 4.5f + seedR * 3.5f
            val y = ((t * speedBig + seedPhase) % fallRange) + h * 0.06f
            val x = seedX * w + sin(t * 0.85f + i * 0.7f) * seedSway
            canvas.drawCircle(x, y, r, bigPaint)
        }
        // 花圃积雪斑块（位置固定，保留为静态装饰）
        val snowCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#F0F0F8"); alpha = 180 }
        val rngCap = java.util.Random(98765)
        for (i in 0 until 50) {
            val sx = rngCap.nextFloat() * w
            val sy = h * 0.38f + rngCap.nextFloat() * h * 0.42f
            canvas.drawCircle(sx, sy, 3f + rngCap.nextFloat() * 7f, snowCapPaint)
        }
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#F0F0F8"); alpha = 200 }
        val rngEdge = java.util.Random(87654)
        for (i in 0 until 20) {
            val ex = rngEdge.nextFloat() * w
            val ey = h * 0.36f + rngEdge.nextFloat() * h * 0.04f
            canvas.drawCircle(ex, ey, 5f + rngEdge.nextFloat() * 8f, edgePaint)
        }
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#F0F0F4"); alpha = 130 }
        canvas.drawRect(RectF(0f, h * 0.78f, w, h * 0.82f), groundPaint)
    }

    private fun drawWindEffect(canvas: Canvas, w: Float, h: Float, t: Float) {
        // 风：风线段与散点随 t 水平扫过（无尽平移），波形 sin 用 t 做相位流动
        val windSpeed = 180f
        val drift = (t * windSpeed) % (w + 200f)
        val baseShift = drift - 100f
        val rng = java.util.Random(77)
        val gustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.parseColor("#78716C")
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; alpha = 130
        }
        val lightGust = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.parseColor("#A8A29E")
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; alpha = 70
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#78716C"); alpha = 150 }
        val lightDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#A8A29E"); alpha = 80 }

        // 3道主风旋（S形扫过，起始粗末端细）
        val gusts = listOf(
            Triple(h * 0.08f, 2.0f, 0.8f),
            Triple(h * 0.22f, 1.5f, 0.6f),
            Triple(h * 0.38f, 2.0f, 1.0f)
        )
        for ((gy, startW, endW) in gusts) {
            val segments = 6
            for (s in 0 until segments) {
                val u = s.toFloat() / segments
                val u2 = (s + 1).toFloat() / segments
                gustPaint.strokeWidth = startW + (endW - startW) * u
                val x1 = w * u + baseShift
                val x2 = w * u2 + baseShift
                val offset = h * 0.015f * sin(u * Math.PI.toFloat() * 4f - t * 2.5f)
                val offset2 = h * 0.015f * sin(u2 * Math.PI.toFloat() * 4f - t * 2.5f)
                canvas.drawLine(x1, gy + offset, x2, gy + offset2, gustPaint)
            }
            for (d in 0 until 8) {
                val dx = rng.nextFloat() * w + baseShift
                val dy = gy + h * 0.015f * sin((dx / w) * Math.PI.toFloat() * 4f) + (rng.nextFloat() - 0.5f) * h * 0.02f
                val dotSize = 1.5f + rng.nextFloat() * 2.5f
                val p = if (rng.nextBoolean()) dotPaint else lightDot
                canvas.drawCircle(dx, dy, dotSize, p)
            }
        }

        // 2道辅风（更淡更细，交错位置）
        val subGusts = listOf(h * 0.15f, h * 0.30f, h * 0.45f)
        for (sy in subGusts) {
            for (s in 0 until 5) {
                val u = s.toFloat() / 5
                val u2 = (s + 1).toFloat() / 5
                lightGust.strokeWidth = 0.8f + u * 0.6f
                val x1 = w * u + baseShift * 0.7f
                val x2 = w * u2 + baseShift * 0.7f
                val offset = h * 0.012f * sin(u * Math.PI.toFloat() * 5f + 1.0f - t * 1.8f)
                val offset2 = h * 0.012f * sin(u2 * Math.PI.toFloat() * 5f + 1.0f - t * 1.8f)
                canvas.drawLine(x1, sy + offset, x2, sy + offset2, lightGust)
            }
        }

        // 飘散小旋风（位置固定，本身为装饰性螺旋）
        for (i in 0 until 4) {
            val cx = w * (0.15f + rng.nextFloat() * 0.7f)
            val cy = h * (0.06f + rng.nextFloat() * 0.46f)
            val swirlR = h * 0.01f + rng.nextFloat() * h * 0.015f
            val swirlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = Color.parseColor("#A8A29E")
                strokeWidth = 0.8f; alpha = 60; strokeCap = Paint.Cap.ROUND
            }
            val swirl = Path()
            swirl.moveTo(cx - swirlR * 0.5f, cy)
            swirl.quadTo(cx, cy - swirlR, cx + swirlR * 0.3f, cy - swirlR * 0.3f)
            swirl.quadTo(cx + swirlR * 0.6f, cy + swirlR * 0.2f, cx + swirlR * 0.1f, cy + swirlR * 0.5f)
            canvas.drawPath(swirl, swirlPaint)
        }
    }

    /** 云：蓬松白云（多椭圆簇），对齐 web 原型 drawClouds 风格 —— 随 t 缓慢水平漂移（每朵速度不同） */
    private fun drawCloudEffect(canvas: Canvas, w: Float, h: Float, t: Float) {
        val rng = java.util.Random(99)
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FFFFFF")
            alpha = 130
        }
        val lobes = listOf(
            Triple(0f, 0f, 60f),
            Triple(45f, 8f, 44f),
            Triple(-48f, 10f, 40f),
            Triple(20f, -14f, 38f)
        )
        val cloudSpeeds = listOf(18f, 12f, 22f)
        val span = w * 1.6f
        for (i in 0 until 3) {
            val scale = 0.7f + i * 0.25f
            val baseCx = w * (0.18f + rng.nextFloat() * 0.62f)
            val cy = h * (0.06f + i * 0.05f)
            val raw = baseCx + cloudSpeeds[i] * t
            val cx = ((raw % span) + span) % span - w * 0.3f
            for ((dx, dy, r) in lobes) {
                val rx = r * scale
                val ry = r * scale * 0.62f
                canvas.drawOval(
                    RectF(cx + dx * scale - rx, cy + dy * scale - ry, cx + dx * scale + rx, cy + dy * scale + ry),
                    cloudPaint
                )
            }
        }
    }

    /** 雾（FOGGY 天气）：地面柔雾 + 多团径向软雾（每团随 t 缓慢水平漂移，无硬横条）—— 对齐 web 原型 v3.4i */
    private fun drawFogEffect(canvas: Canvas, w: Float, h: Float, t: Float) {
        // 地面柔雾：竖向渐变，近地浓、向上淡出（顶/底 alpha→0，无水平硬带）
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, h * 0.30f, 0f, h,
                intArrayOf(Color.argb(0, 228, 228, 219),
                    Color.argb((0.12f * 255).toInt(), 228, 228, 219),
                    Color.argb((0.20f * 255).toInt(), 228, 228, 219)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, h * 0.30f, w, h * 0.70f, base)
        // 多团软雾：x 随 t 缓慢水平漂移（每团速度不同）
        val drifts = listOf(22f, 16f, 28f, 14f, 24f, 18f)
        val span = w * 0.8f
        for (i in 0..5) {
            val driftSpeed = drifts[i]
            val y = h * (0.34f + 0.11f * i)
            val x0 = w * (0.10f + i * 0.21f)
            val raw = x0 + driftSpeed * t
            val x = ((raw % span) + span) % span - w * 0.05f
            val rw = w * (0.30f + (i % 3) * 0.06f)
            val rh = rw * (0.34f + (i % 2) * 0.05f)
            val a = (0.15f - i * 0.015f).coerceAtLeast(0.04f)
            canvas.save()
            canvas.translate(x, y)
            canvas.scale(1f, rh / rw)
            val g = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(0f, 0f, rw,
                    intArrayOf(Color.argb((a * 255).toInt(), 233, 233, 225),
                                Color.argb(((a * 0.5f) * 255).toInt(), 230, 230, 221),
                                Color.argb(0, 230, 230, 221)),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(0f, 0f, rw, g)
            canvas.restore()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 卡通花园元素：竹篱笆、园圃格子、地面线
    // ═══════════════════════════════════════════════════════════════

    /**
     * 木篱笆：粗厚木桩 + 纵向木纹 + 横梁 + 钉子 + 高低错落
     */
    private fun drawFence(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val rng = java.util.Random(99)
        val baseY = h * 0.355f

        val postPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#8B7355"); alpha = 200 }
        val postLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#A89068"); alpha = 180 }
        val postDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#6B5A3E"); alpha = 200 }
        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#7A6548"); alpha = 180 }
        val beamEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f; color = Color.parseColor("#5A4A32"); alpha = 100; strokeCap = Paint.Cap.ROUND }
        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.6f; color = Color.parseColor("#6B5A3E"); alpha = 70; strokeCap = Paint.Cap.ROUND }
        val nailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#3C3226"); alpha = 220 }
        val nailHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#C8BCA8"); alpha = 80 }

        val postCount = 11
        val spacing = w / postCount
        // 每根木桩的高度：高高低低，模拟手工砍伐
        val postHeights = FloatArray(postCount) { h * 0.025f + rng.nextFloat() * h * 0.015f }
        // 木桩宽度也略微随机
        val postWidths = FloatArray(postCount) { w * 0.028f + rng.nextFloat() * w * 0.008f }
        // 左右偏移：微微偏离中心线，看起来更自然
        val postOffsets = FloatArray(postCount) { (rng.nextFloat() - 0.5f) * spacing * 0.08f }

        // 横梁Y位置（跟随木桩高度起伏）
        val beam1Y = FloatArray(postCount)
        val beam2Y = FloatArray(postCount)
        for (i in 0 until postCount) {
            val topY = baseY - postHeights[i]
            beam1Y[i] = topY + postHeights[i] * 0.35f
            beam2Y[i] = topY + postHeights[i] * 0.68f
        }

        // ─── 绘制横梁（在木桩后面先画，避免遮盖木桩纹理） ───
        // 上横梁
        val beamTopPath = Path()
        beamTopPath.moveTo(0f, beam1Y[0] - 2f)
        for (i in 0 until postCount) {
            val cx = spacing * (i + 0.5f)
            beamTopPath.lineTo(cx, beam1Y[i] - 2f)
        }
        beamTopPath.lineTo(w, beam1Y[postCount - 1] - 2f)
        beamTopPath.lineTo(w, beam1Y[postCount - 1] + 3f)
        for (i in postCount - 1 downTo 0) {
            val cx = spacing * (i + 0.5f)
            beamTopPath.lineTo(cx, beam1Y[i] + 3f)
        }
        beamTopPath.lineTo(0f, beam1Y[0] + 3f)
        beamTopPath.close()
        canvas.drawPath(beamTopPath, beamPaint)
        canvas.drawPath(beamTopPath, beamEdge)

        // 下横梁
        val beamBottomPath = Path()
        beamBottomPath.moveTo(0f, beam2Y[0] - 2f)
        for (i in 0 until postCount) {
            val cx = spacing * (i + 0.5f)
            beamBottomPath.lineTo(cx, beam2Y[i] - 2f)
        }
        beamBottomPath.lineTo(w, beam2Y[postCount - 1] - 2f)
        beamBottomPath.lineTo(w, beam2Y[postCount - 1] + 3f)
        for (i in postCount - 1 downTo 0) {
            val cx = spacing * (i + 0.5f)
            beamBottomPath.lineTo(cx, beam2Y[i] + 3f)
        }
        beamBottomPath.lineTo(0f, beam2Y[0] + 3f)
        beamBottomPath.close()
        canvas.drawPath(beamBottomPath, beamPaint)
        canvas.drawPath(beamBottomPath, beamEdge)

        // 横梁木纹（细横线）
        for (i in 0 until postCount) {
            val cx = spacing * (i + 0.5f)
            if (i > 0) {
                val prevCx = spacing * (i - 1 + 0.5f)
                val midY1 = (beam1Y[i] + beam1Y[i - 1]) / 2f
                canvas.drawLine(prevCx, midY1 + 1f, cx, midY1 + 1f, grainPaint)
                val midY2 = (beam2Y[i] + beam2Y[i - 1]) / 2f
                canvas.drawLine(prevCx, midY2 + 1f, cx, midY2 + 1f, grainPaint)
            }
        }

        // ─── 绘制木桩（每个不同高度/宽度/偏移，粗糙边缘） ───
        for (i in 0 until postCount) {
            val cx = spacing * (i + 0.5f) + postOffsets[i]
            val postH = postHeights[i]
            val postW = postWidths[i] / 2f  // 半宽
            val topY = baseY - postH
            val left = cx - postW
            val right = cx + postW

            // 木桩主体（用Path模拟粗糙木材边缘，不是圆角矩形）
            val postPath = Path()
            // 左侧边缘：略微凸凹
            postPath.moveTo(left + rng.nextFloat() * 1.5f, topY)
            postPath.lineTo(left + rng.nextFloat() * 2f, topY + postH * 0.15f)
            postPath.lineTo(left - rng.nextFloat() * 1f, topY + postH * 0.30f)
            postPath.lineTo(left + rng.nextFloat() * 1.5f, topY + postH * 0.50f)
            postPath.lineTo(left - rng.nextFloat() * 1f, topY + postH * 0.70f)
            postPath.lineTo(left + rng.nextFloat() * 2f, topY + postH * 0.85f)
            postPath.lineTo(left + rng.nextFloat() * 1f, baseY)
            // 底部
            postPath.lineTo(right - rng.nextFloat() * 1f, baseY)
            // 右侧边缘
            postPath.lineTo(right + rng.nextFloat() * 1.5f, topY + postH * 0.85f)
            postPath.lineTo(right - rng.nextFloat() * 1f, topY + postH * 0.70f)
            postPath.lineTo(right + rng.nextFloat() * 1.5f, topY + postH * 0.50f)
            postPath.lineTo(right - rng.nextFloat() * 1.5f, topY + postH * 0.30f)
            postPath.lineTo(right + rng.nextFloat() * 2f, topY + postH * 0.15f)
            postPath.lineTo(right - rng.nextFloat() * 1f, topY)
            // 顶部（略微倾斜）
            postPath.lineTo(left + rng.nextFloat() * 1.5f, topY)
            postPath.close()
            canvas.drawPath(postPath, postPaint)

            // 左侧受光面（浅色，模拟光照）
            val lightPath = Path()
            lightPath.moveTo(left, topY + 3f)
            lightPath.lineTo(left + postW * 0.2f, topY + postH * 0.2f)
            lightPath.lineTo(left + postW * 0.25f, topY + postH * 0.5f)
            lightPath.lineTo(left + postW * 0.15f, topY + postH * 0.8f)
            lightPath.lineTo(left, baseY - 3f)
            lightPath.lineTo(left + 2f, baseY - 3f)
            lightPath.lineTo(left + 2f, topY + 3f)
            lightPath.close()
            canvas.drawPath(lightPath, postLight)

            // 右侧阴影面（深色）
            if (postW > 3f) {
                val darkPath = Path()
                darkPath.moveTo(right - 2f, topY + 3f)
                darkPath.lineTo(right - postW * 0.15f, topY + postH * 0.3f)
                darkPath.lineTo(right - postW * 0.2f, topY + postH * 0.6f)
                darkPath.lineTo(right - postW * 0.1f, topY + postH * 0.85f)
                darkPath.lineTo(right - 2f, baseY - 3f)
                darkPath.lineTo(right, baseY - 3f)
                darkPath.lineTo(right, topY + 3f)
                darkPath.close()
                canvas.drawPath(darkPath, postDark)
            }

            // 树木年轮/木纹（纵向弯曲线条，模拟真实木材纹理）
            val grainOffset = postW * 0.3f
            for (g in 0 until 4) {
                val gx = cx - grainOffset + g * grainOffset * 0.6f
                val grainPath = Path()
                grainPath.moveTo(gx + rng.nextFloat() * 2f - 1f, topY + 5f)
                grainPath.quadTo(
                    gx + rng.nextFloat() * 3f - 1.5f, topY + postH * 0.4f,
                    gx + rng.nextFloat() * 2f - 1f, topY + postH * 0.7f
                )
                grainPath.lineTo(gx + rng.nextFloat() * 2f - 1f, baseY - 3f)
                canvas.drawPath(grainPath, grainPaint)
            }

            // 木节（深色椭园，一上一下）
            val knotR1 = postW * 0.25f
            canvas.drawOval(RectF(cx - knotR1, topY + postH * 0.22f, cx + knotR1, topY + postH * 0.35f), postDark)
            canvas.drawOval(RectF(cx - knotR1 * 0.7f, topY + postH * 0.58f, cx + knotR1 * 0.7f, topY + postH * 0.68f), postDark)

            // 顶部砍削面（浅色不规则椭圆）
            val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#B09870"); alpha = 200 }
            val capPath = Path()
            capPath.moveTo(cx - postW * 0.35f, topY)
            capPath.quadTo(cx - postW * 0.2f, topY - postW * 0.3f, cx, topY - postW * 0.15f)
            capPath.quadTo(cx + postW * 0.2f, topY - postW * 0.3f, cx + postW * 0.35f, topY)
            capPath.close()
            canvas.drawPath(capPath, capPaint)

            // 顶部年轮线
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.5f; color = Color.parseColor("#8B7355"); alpha = 80 }
            canvas.drawOval(RectF(cx - postW * 0.15f, topY - postW * 0.05f, cx + postW * 0.15f, topY + postW * 0.08f), ringPaint)
            canvas.drawOval(RectF(cx - postW * 0.08f, topY - postW * 0.02f, cx + postW * 0.08f, topY + postW * 0.04f), ringPaint)

            // 裂缝（部分木桩上有竖直裂缝）
            if (i % 3 == 0) {
                val crackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f; color = Color.parseColor("#5A4A32"); alpha = 100 }
                val crackPath = Path()
                crackPath.moveTo(cx + rng.nextFloat() * 2f, topY + 4f)
                crackPath.lineTo(cx + rng.nextFloat() * 3f, topY + postH * 0.25f)
                crackPath.lineTo(cx + rng.nextFloat() * 1f, topY + postH * 0.4f)
                canvas.drawPath(crackPath, crackPaint)
            }

            // 钉子（4个，钉在横梁交叉处，深色大圆）
            val nailR = 2.0f
            val nailPositions = arrayOf(
                Pair(-postW * 0.3f, beam1Y[i]),
                Pair(postW * 0.3f, beam1Y[i]),
                Pair(-postW * 0.3f, beam2Y[i]),
                Pair(postW * 0.3f, beam2Y[i])
            )
            for ((nx, ny) in nailPositions) {
                canvas.drawCircle(cx + nx, ny, nailR, nailPaint)
                canvas.drawCircle(cx + nx - 0.5f, ny - 0.5f, nailR * 0.3f, nailHighlight)
            }
        }
    }

    /**
     * 可爱园圃土块格子：圆角土块 + 四季配色 + 立体阴影高光
     */
    private fun drawGardenPlots(canvas: Canvas, width: Int, height: Int, season: Season) {
        val w = width.toFloat()
        val h = height.toFloat()
        val useCols = gridCols
        val useRows = gridRows
        val layout = computePlotLayout(w, useCols, h, useRows)
        val plotW = layout.plotW
        val plotH = layout.plotH
        val startY = h * PLOT_TOP_OFFSET + 6f
        val gap = PLOT_GAP

        // 四季园圃配色
        val (fillColor, borderColor, innerColor) = when (season) {
            Season.SPRING -> Triple("#E8D4C0", "#D0BCA8", "#F0E4D4")
            Season.SUMMER -> Triple("#C8CCB8", "#B0B49E", "#D8DCC8")
            Season.AUTUMN -> Triple("#D4C4A0", "#BEAE84", "#E4D8B8")
            Season.WINTER -> Triple("#D0CCC8", "#B8B4B0", "#E4E0DC")
        }

        val soilFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor(fillColor)
        }
        val soilBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor(borderColor)
        }
        val soilInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = Color.parseColor(innerColor)
        }

        for (row in 0 until useRows) {
            for (col in 0 until useCols) {
                val left = layout.startX + col * (plotW + gap)
                val top = startY + row * (plotH + gap)
                val r = 8f

                // 阴影层（右下偏移）
                soilBorder.alpha = 80
                canvas.drawRoundRect(RectF(left + 2f, top + 2f, left + plotW + 2f, top + plotH + 2f), r, r, soilBorder)
                // 主体填充
                soilBorder.alpha = 200
                canvas.drawRoundRect(RectF(left, top, left + plotW, top + plotH), r, r, soilFill)
                // 内部高光（左上角亮色，制造立体感）
                soilInner.alpha = 100
                canvas.drawRoundRect(RectF(left + 3f, top + 3f, left + plotW * 0.6f, top + plotH * 0.4f), r * 0.5f, r * 0.5f, soilInner)
                // 边框（深色轮廓）
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; color = Color.parseColor("#A09078"); strokeWidth = 1.5f; strokeCap = Paint.Cap.ROUND
                }
                canvas.drawRoundRect(RectF(left, top, left + plotW, top + plotH), r, r, border)
            }
        }
    }

    /**
     * 可爱地面线：略带厚度的地面
     */
    private fun drawGroundLine(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val y = h * 0.82f

        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#8B7D6B")
            alpha = 160
        }
        canvas.drawRect(RectF(0f, y, w, y + 8f), groundPaint)

        // 地面线
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#57534E")
            strokeWidth = 1.5f
            alpha = 200
        }
        canvas.drawLine(0f, y, w, y, linePaint)
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

    /** 草坪已在场景层完整绘制；植物在独立前景层按景深绘制。 */
    fun drawPlants(
        canvas: Canvas,
        plants: List<PlantRenderInfo>,
        canvasWidth: Int,
        canvasHeight: Int,
        season: Season,
        ambientPhase: Float = 0f
    ) {
        plants.sortedWith(compareBy<PlantRenderInfo> { it.y }.thenBy { it.x }).forEach { plant ->
            val phaseOffset = (plant.plantId % 7).toFloat() * 0.72f
            val floatOffset = sin(ambientPhase + phaseOffset) *
                minOf(2.5f, plant.scale * 0.24f)
            val drawY = plant.y + floatOffset
            val bitmap = plant.bitmap
                ?.takeUnless { it.isRecycled }
                ?: plantPngCache[plant.plantName]?.takeUnless { it.isRecycled }
            if (bitmap == null) {
                // 规范第11节：图片加载失败时显示稳定的植物轮廓占位，不改变卡片尺寸
                drawPlantPlaceholder(canvas, plant, drawY)
                return@forEach
            }
            drawPlantBitmap(canvas, bitmap, plant.x, drawY, plant.scale, alpha = witherAlpha(plant.witherStage))
            if (plant.witherStage >= 3) drawSpiderWeb(canvas, plant, drawY)
        }
    }

    /** 休眠(规范第7节)：按阶段返回整体不透明度，只影响视觉、不触动任何数值 */
    private fun witherAlpha(stage: Int): Int = when (stage) {
        0 -> 255
        1 -> 204 // 4~6 天：0.8
        2 -> 153 // 7~14 天：0.6
        else -> 102 // 15 天以上：0.4（维持，不再继续恶化）
    }

    /** 植物 PNG 缺失时的程序化占位：柔和圆底 + 植物名首字，尺寸与正常植物一致 */
    private fun drawPlantPlaceholder(canvas: Canvas, plant: PlantRenderInfo, y: Float) {
        val size = 10f * plant.scale
        val cx = plant.x
        val cy = y - size * 0.40f
        val r = size * 0.30f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(38, 47, 107, 88)
        }
        canvas.drawCircle(cx, cy, r, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(120, 115, 138, 122)
            strokeWidth = 1.5f
        }
        canvas.drawCircle(cx, cy, r, border)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 90, 110, 96)
            textSize = size * 0.26f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val label = if (plant.plantName.length > 2) plant.plantName.substring(0, 2) else plant.plantName
        canvas.drawText(label, cx, cy + text.textSize * 0.34f, text)
    }

    /** 长期不阅读(15 天+)的轻微蛛网装饰，仍是纯视觉，不暗示惩罚 */
    private fun drawSpiderWeb(canvas: Canvas, plant: PlantRenderInfo, y: Float) {
        val size = 10f * plant.scale
        val cx = plant.x
        val cy = y - size * 0.40f
        val r = size * 0.34f
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(55, 200, 200, 200)
            strokeWidth = 1f
        }
        for (i in 0 until 3) {
            val a = i * 2.094f + 0.4f
            canvas.drawLine(cx, cy, cx + cos(a) * r, cy + sin(a) * r, line)
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(40, 200, 200, 200)
            strokeWidth = 1f
        }
        canvas.drawCircle(cx, cy, r * 0.6f, ring)
        canvas.drawCircle(cx, cy, r, ring)
    }
}
