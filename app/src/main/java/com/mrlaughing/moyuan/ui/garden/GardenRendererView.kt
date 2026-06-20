package com.mrlaughing.moyuan.ui.garden

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mrlaughing.moyuan.render.PlantRenderInfo
import com.mrlaughing.moyuan.util.einkInvalidate

/**
 * 花园渲染自定义 View：Canvas 绘制植物
 * - 宣纸纹理背景 (#FFFFF5)
 * - 按 Y 坐标排序绘制植物 PNG
 * - 点击检测
 */
class GardenRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 宣纸背景色 */
    private val xuanPaperColor = Color.parseColor("#FFFFF5")

    /** 宣纸纹理画笔 */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = xuanPaperColor
        style = Paint.Style.FILL
    }

    /** 纹理噪点画笔 */
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F0E0")
        style = Paint.Style.FILL
        alpha = 30
    }

    /** 当前渲染的植物列表 */
    private var renderPlants: List<PlantRenderInfo> = emptyList()

    /** 点击监听 */
    private var onPlantClickListener: ((Long) -> Unit)? = null

    /** 纹理是否已生成 */
    private var textureGenerated = false

    /** 纹理点位置缓存 */
    private val texturePoints = mutableListOf<Float>()

    private val plantPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = false  // 墨水屏不需要抗锯齿
        isDither = false
    }

    /** 临时 Matrix 用于缩放和定位 */
    private val drawMatrix = Matrix()

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
        val rng = java.util.Random(42)  // 固定种子保证一致性
        val density = 0.002f  // 每1000像素2个点
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
        renderPlants = plants.sortedBy { it.y }  // 按 Y 坐标排序（远的先画）
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

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. 绘制宣纸背景
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. 绘制纹理噪点
        if (textureGenerated) {
            var i = 0
            while (i + 1 < texturePoints.size) {
                canvas.drawPoint(texturePoints[i], texturePoints[i + 1], texturePaint)
                i += 2
            }
        }

        // 3. 按 Y 坐标顺序绘制植物
        for (plant in renderPlants) {
            drawPlant(canvas, plant)
        }
    }

    /**
     * 绘制单株植物
     */
    private fun drawPlant(canvas: Canvas, plant: PlantRenderInfo) {
        val bitmap = plant.bitmap ?: return

        drawMatrix.reset()
        // 缩放
        drawMatrix.postScale(plant.scale, plant.scale)
        // 平移到指定位置（以植物底部中心为锚点）
        val scaledWidth = bitmap.width * plant.scale
        val scaledHeight = bitmap.height * plant.scale
        drawMatrix.postTranslate(
            plant.x - scaledWidth / 2f,
            plant.y - scaledHeight  // Y坐标是底部
        )

        canvas.drawBitmap(bitmap, drawMatrix, plantPaint)
    }

    /**
     * 点击检测：判断点击位置是否在某个植物的 bitmap 范围内
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return false

        val touchX = event.x
        val touchY = event.y

        // 从上层（近景）到下层（远景）遍历，优先命中近景
        for (plant in renderPlants.reversed()) {
            val bitmap = plant.bitmap ?: continue
            val scaledWidth = bitmap.width * plant.scale
            val scaledHeight = bitmap.height * plant.scale
            val left = plant.x - scaledWidth / 2f
            val top = plant.y - scaledHeight
            val right = plant.x + scaledWidth / 2f
            val bottom = plant.y

            if (touchX in left..right && touchY in top..bottom) {
                onPlantClickListener?.invoke(plant.plantId)
                return true
            }
        }
        return false
    }
}
