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
 * 分形树生长动画 View（参考掘金仿树木生长开花效果）
 *
 * 存储所有枝干数据 → 每次onDraw全部重绘 → 逐帧新增枝干实现动画
 */
class FractalTreeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BranchLine(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val width: Float
    )
    data class Decoration(
        val x: Float, val y: Float,
        val size: Float,
        val isFlower: Boolean
    )

    private val branchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; color = Color.parseColor("#8B7355"); alpha = 200
    }
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#8B7D6B"); alpha = 140
    }
    private val flowerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#B09890"); alpha = 160
    }
    private val flowerCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#C8B8A8"); alpha = 180
    }

    private val branches = mutableListOf<BranchLine>()
    private val decorations = mutableListOf<Decoration>()
    private val pendingTasks = mutableListOf<() -> Unit>()
    private var isAnimating = false
    private val handler = Handler(Looper.getMainLooper())
    private var treeStarted = false
    private var rng = Random(System.nanoTime())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (!treeStarted) {
            treeStarted = true
            rng = Random(System.nanoTime())
            startGrowth(w, h)
        }
        // 重绘所有枝干
        for (b in branches) {
            branchPaint.strokeWidth = b.width
            canvas.drawLine(b.x1, b.y1, b.x2, b.y2, branchPaint)
        }
        // 重绘所有装饰
        for (d in decorations) {
            if (d.isFlower) {
                canvas.drawCircle(d.x, d.y, d.size, flowerPaint)
                canvas.drawCircle(d.x, d.y, d.size * 0.4f, flowerCorePaint)
            } else {
                canvas.drawCircle(d.x, d.y, d.size, leafPaint)
            }
        }
    }

    private fun startGrowth(w: Float, h: Float) {
        pendingTasks.clear()
        val trunkLen = (h * 0.30f).coerceAtMost(w * 0.25f)
        val trunkWidth = (h * 0.06f).coerceAtMost(3f)
        val startBranch = Branch(
            startX = w * 0.5f, startY = h * 0.92f,
            angle = -Math.PI / 2,
            length = trunkLen,
            lineWidth = trunkWidth
        )
        isAnimating = true
        step(startBranch, 0)
        scheduleNextFrame()
    }

    private fun step(b: Branch, depth: Int) {
        val endX = (b.startX + cos(b.angle) * b.length).toFloat()
        val endY = (b.startY + sin(b.angle) * b.length).toFloat()

        pendingTasks.add {
            branches.add(BranchLine(b.startX, b.startY, endX, endY, b.lineWidth))
        }

        // 末端装饰
        val isSmall = depth >= 5 || b.length < 4f
        if (isSmall && rng.nextFloat() < 0.45f) {
            pendingTasks.add {
                decorations.add(Decoration(
                    endX + (rng.nextFloat() - 0.5f) * 3f,
                    endY + (rng.nextFloat() - 0.5f) * 3f,
                    1f + rng.nextFloat() * 2f,
                    rng.nextFloat() < 0.5f
                ))
            }
        }

        // 递归分支
        if (depth < 6 && b.length > 2f) {
            val shrink = 0.7f + rng.nextFloat() * 0.1f
            val newLen = b.length * shrink
            val newWidth = (b.lineWidth * 0.72f).coerceAtLeast(0.5f)

            // 左分支（大角度展开）
            pendingTasks.add {
                val la = b.angle - 0.35f - rng.nextFloat() * 0.25f
                step(Branch(endX, endY, la, newLen, newWidth), depth + 1)
            }
            // 右分支（大角度展开）
            pendingTasks.add {
                val ra = b.angle + 0.35f + rng.nextFloat() * 0.25f
                step(Branch(endX, endY, ra, newLen, newWidth), depth + 1)
            }
            // 中间分支 - 30%概率
            if (depth >= 2 && rng.nextFloat() < 0.3f) {
                pendingTasks.add {
                    val ma = b.angle + (rng.nextFloat() - 0.5f) * 0.3f
                    step(Branch(endX, endY, ma, newLen * 0.8f, newWidth * 0.8f), depth + 2)
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
        }, 40L)
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
        branches.clear(); decorations.clear(); pendingTasks.clear()
        handler.removeCallbacksAndMessages(null)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAnimating = false
        handler.removeCallbacksAndMessages(null)
    }

    data class Branch(
        val startX: Float, val startY: Float,
        val angle: Double,
        val length: Float,
        val lineWidth: Float
    )
}
