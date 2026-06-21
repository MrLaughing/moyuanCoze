package com.mrlaughing.moyuan.ui.garden

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.render.GardenRenderer
import com.mrlaughing.moyuan.render.PlantRenderInfo
import com.mrlaughing.moyuan.util.einkInvalidate

/**
 * 花园渲染自定义 View：水墨风格 Canvas 绘制
 * - 宣纸暖色背景
 * - 山水背景层 + 四季变化
 * - 天气特效
 * - Canvas 绘制水墨风植物
 * - 点击检测
 */
class GardenRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 当前季节 */
    private var currentSeason: Season = Season.SPRING

    /** 当前天气 */
    private var currentWeather: Weather = Weather.CLEAR

    /** 当前渲染的植物列表 */
    private var renderPlants: List<PlantRenderInfo> = emptyList()

    /** 点击监听 */
    private var onPlantClickListener: ((Long) -> Unit)? = null

    /** 纹理是否已生成 */
    private var textureGenerated = false

    /** 纹理点位置缓存 */
    private val texturePoints = mutableListOf<Float>()

    /** 宣纸纹理画笔 */
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F0E0")
        style = Paint.Style.FILL
        alpha = 20
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textureGenerated = false
        generateTexture(w, h)
    }

    /**
     * 生成宣纸纹理噪点
     */
    private fun generateTexture(width: Int, height: Int) {
        if (textureGenerated || width <= 0 || height <= 0) return
        texturePoints.clear()
        val rng = java.util.Random(42)
        val density = 0.001f
        val count = (width * height * density).toInt()
        for (i in 0 until count) {
            val x = rng.nextFloat() * width
            val y = rng.nextFloat() * height
            texturePoints.add(x)
            texturePoints.add(y)
        }
        textureGenerated = true
    }

    /**
     * 更新植物数据并重绘
     */
    fun updatePlants(plants: List<PlantRenderInfo>) {
        renderPlants = plants.sortedBy { it.y }
        einkInvalidate()
    }

    /**
     * 设置季节和天气
     */
    fun setSeasonAndWeather(season: Season, weather: Weather) {
        currentSeason = season
        currentWeather = weather
        einkInvalidate()
    }

    /**
     * 设置植物点击监听
     */
    fun setOnPlantClickListener(listener: (Long) -> Unit) {
        onPlantClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        // 1. 绘制花园完整画面（背景 + 天气 + 植物）
        GardenRenderer.drawGarden(
            canvas,
            w, h,
            currentSeason,
            currentWeather,
            renderPlants
        )

        // 2. 绘制宣纸纹理（叠加在植物上方）
        if (textureGenerated) {
            var i = 0
            while (i + 1 < texturePoints.size) {
                canvas.drawPoint(texturePoints[i], texturePoints[i + 1], texturePaint)
                i += 2
            }
        }
    }

    /**
     * 点击检测：判断点击位置是否在某个植物的范围内
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false

        val touchX = event.x
        val touchY = event.y

        // 从上层（近景）到下层（远景）遍历，优先命中近景
        for (plant in renderPlants.reversed()) {
            val hitRadius = 30f * plant.scale  // 扩大命中区域，远景植物也能点到

            // 植物位置是 y 坐标，在其上方一定范围内
            val topY = plant.y - 60f * plant.scale
            val bottomY = plant.y

            if (touchX >= plant.x - hitRadius && touchX <= plant.x + hitRadius &&
                touchY >= topY && touchY <= bottomY) {
                onPlantClickListener?.invoke(plant.plantId)
                return true
            }
        }
        return false
    }
}
