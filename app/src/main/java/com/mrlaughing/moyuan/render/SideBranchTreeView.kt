package com.mrlaughing.moyuan.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 垂枝梅花/樱花效果 View
 *
 * 参考：多根细长枝条从左侧某点呈扇形展开，花朵密集点缀
 * 主干不明显，主要是细长分枝自然交错延伸
 */
class SideBranchTreeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Twig(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val width: Float
    )
    data class Blossom(
        val x: Float, val y: Float,
        val size: Float,
        val colorIdx: Int
    )

    // 枝条画笔 — 极细的淡褐色
    private val twigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#B8A090")
        alpha = 160
    }

    // 花朵颜色 — 粉红色系
    private val blossomPaints = listOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.FILL 
            color = Color.parseColor("#FFB7C5") // 樱花粉
            alpha = 200 
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.FILL 
            color = Color.parseColor("#FF9EAA") // 深粉
            alpha = 190 
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.FILL 
            color = Color.parseColor("#FFC0CB") // 浅粉
            alpha = 180 
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.FILL 
            color = Color.parseColor("#FF69B4") // 亮粉
            alpha = 170 
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.FILL 
            color = Color.parseColor("#F4A4A4") // 桃粉
            alpha = 185 
        },
    )

    // 花蕊
    private val stamenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFE4E1")
        alpha = 220
    }

    private val twigs = mutableListOf<Twig>()
    private val blossoms = mutableListOf<Blossom>()
    private val pendingTasks = mutableListOf<() -> Unit>()
    private var isAnimating = false
    private val handler = Handler(Looper.getMainLooper())
    private var treeStarted = false
    private var rng = Random(System.nanoTime())

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        // 父容器 wrap_content 时 (UNSPECIFIED)，返回 0 不撑大布局
        val h = if (heightMode == MeasureSpec.UNSPECIFIED) 0 else heightSize
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (!treeStarted) {
            treeStarted = true
            // 延迟到布局完成后启动，避免首次绘制时尺寸未定导致方向错乱
            post {
                if (width > 0 && height > 0) {
                    rng = Random(System.nanoTime())
                    startGrowth(width.toFloat(), height.toFloat())
                }
            }
        }

        // 绘制所有细枝
        for (t in twigs) {
            twigPaint.strokeWidth = t.width
            canvas.drawLine(t.x1, t.y1, t.x2, t.y2, twigPaint)
        }

        // 绘制所有花朵
        for (b in blossoms) {
            val paint = blossomPaints[b.colorIdx % blossomPaints.size]
            // 五瓣简化绘制
            val r = b.size * 0.4f
            for (i in 0 until 5) {
                val angle = Math.PI * 2 / 5 * i - Math.PI / 2
                val px = b.x + cos(angle) * r * 0.6
                val py = b.y + sin(angle) * r * 0.6
                canvas.drawCircle(px.toFloat(), py.toFloat(), r * 0.5f, paint)
            }
            // 花蕊
            canvas.drawCircle(b.x, b.y, b.size * 0.2f, stamenPaint)
        }
    }

    private fun startGrowth(w: Float, h: Float) {
        pendingTasks.clear()
        
        // 主枝从左侧头像高度中间伸出（固定像素值，不依赖父容器高度）
        val startX = -2f
        val startY = 80f // 固定 80px，约等于顶部 padding 16dp + 头像中心 28dp
        val angle = 0.20 // 约 11° 向下
        val length = w * 0.12f
        val brWidth = 8.0f
        
        pendingTasks.add {
            growBranch(startX, startY, angle, length, brWidth, 0, w)
        }
        
        isAnimating = true
        scheduleNextFrame()
    }

    private fun growBranch(
        startX: Float, startY: Float,
        angle: Double, length: Float, brWidth: Float,
        depth: Int, cardW: Float
    ) {
        val endX = (startX + cos(angle) * length).toFloat()
        val endY = (startY + sin(angle) * length).toFloat()

        twigs.add(Twig(startX, startY, endX, endY, brWidth))

        // 沿枝条密集分布花朵，两侧随机摆动
        val flowerCount = (length / 10f).toInt().coerceIn(4, 25)
        for (fi in 0 until flowerCount) {
            val t = 0.1f + fi * (0.9f / flowerCount)
            val perpAngle = angle + Math.PI / 2
            val offset = (rng.nextFloat() - 0.5f) * 18f
            val fx = (startX + (endX - startX) * t + cos(perpAngle) * offset).toFloat()
            val fy = (startY + (endY - startY) * t + sin(perpAngle) * offset).toFloat()
            val fsize = 5.0f + rng.nextFloat() * 5.0f
            val colorIdx = rng.nextInt(blossomPaints.size)
            blossoms.add(Blossom(fx, fy, fsize, colorIdx))
        }

        // 主枝上抽侧枝
        if (depth == 0) {
            // 1-2 条侧枝，角度 ±10°~30°
            val subCount = 1 + rng.nextInt(2)
            for (si in 0 until subCount) {
                val t = 0.3f + si * (0.7f / (subCount - 1).coerceAtLeast(1))
                val sx = startX + (endX - startX) * t
                val sy = startY + (endY - startY) * t
                val upOrDown = if (rng.nextFloat() < 0.5f) -1f else 1f
                val angleOffset = upOrDown * (0.17f + rng.nextFloat() * 0.35f) // ±10°~30°
                val subAngle = angle + angleOffset
                val subLength = cardW * (0.16f + rng.nextFloat() * 0.10f) // w*0.16~0.26
                val subWidth = brWidth * 0.55f
                pendingTasks.add {
                    growBranch(sx, sy, subAngle, subLength, subWidth.coerceAtLeast(5.0f), depth + 1, cardW)
                }
            }
        }
        
        // 递归分叉（共 7 层：主枝→6层子枝）
        if (depth >= 1 && depth < 6 && length > 15f) {
            val subCount = 1 + rng.nextInt(2) // 1-2 条
            for (si in 0 until subCount) {
                val t = 0.3f + si * (0.7f / (subCount - 1).coerceAtLeast(1))
                val sx = startX + (endX - startX) * t
                val sy = startY + (endY - startY) * t
                // 角度 ±10°~30°
                val absOffset = 0.17f + rng.nextFloat() * 0.35f
                val angleOffset = if (rng.nextFloat() < 0.5f) -absOffset else absOffset
                val subAngle = angle + angleOffset
                // 逐层缩短
                val shrink = 1f - depth * 0.08f
                val subLength = cardW * (0.18f + rng.nextFloat() * 0.17f) * shrink.coerceAtLeast(0.5f)
                val subWidth = brWidth * (0.55f - depth * 0.05f).coerceAtLeast(1.0f)
                pendingTasks.add {
                    growBranch(sx, sy, subAngle, subLength, subWidth.coerceAtLeast(1.0f), depth + 1, cardW)
                }
            }
        }
    }

    private fun scheduleNextFrame() {
        if (!isAnimating) return
        handler.postDelayed({
            performFrame()
            if (pendingTasks.isNotEmpty()) {
                scheduleNextFrame()
            } else {
                isAnimating = false
            }
        }, 30L)
    }

    private fun performFrame() {
        val tasks = pendingTasks.toList()
        pendingTasks.clear()
        tasks.forEach { it() }
        invalidate()
    }

    fun regrow() {
        treeStarted = false
        isAnimating = false
        twigs.clear()
        blossoms.clear()
        pendingTasks.clear()
        handler.removeCallbacksAndMessages(null)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAnimating = false
        handler.removeCallbacksAndMessages(null)
    }
}
