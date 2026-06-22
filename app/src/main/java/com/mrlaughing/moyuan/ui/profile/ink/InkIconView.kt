package com.mrlaughing.moyuan.ui.profile.ink

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.mrlaughing.moyuan.R

/**
 * 成就水墨线条图标 View
 *
 * 12种图标类型，用简单几何线条绘制水墨风格图案：
 * SPROUT(嫩芽), BOOK(书卷), MOUNTAIN(山峦), BRUSH(毛笔), SEAL(印章),
 * MOON(月牙), STAR(五角星), FOUR_LEAVES(四叶), PATH_LINE(连续路径),
 * FLOWER(花朵), HOUSE(房屋), WAVE(波浪)
 */
class InkIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val SPROUT = 0
        const val BOOK = 1
        const val MOUNTAIN = 2
        const val BRUSH = 3
        const val SEAL = 4
        const val MOON = 5
        const val STAR = 6
        const val FOUR_LEAVES = 7
        const val PATH_LINE = 8
        const val FLOWER = 9
        const val HOUSE = 10
        const val WAVE = 11

        fun iconTypeForAchievement(achievementId: String): Int {
            return when (achievementId) {
                "first_sync" -> SPROUT
                "read_10_books" -> BOOK
                "read_50_books" -> BOOK
                "night_read_30" -> MOON
                "read_100_hours" -> MOUNTAIN
                "read_500_hours" -> WAVE
                "first_sprout" -> SEAL
                "unlock_10" -> FLOWER
                "unlock_all" -> HOUSE
                "reach_lv5" -> FOUR_LEAVES
                "streak_7" -> PATH_LINE
                "streak_30" -> STAR
                else -> SPROUT
            }
        }
    }

    var iconType: Int = SPROUT
        set(value) { field = value; invalidate() }

    var isUnlocked: Boolean = true
        set(value) { field = value; invalidate() }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val color = if (isUnlocked) context.getColor(R.color.ink_dark) else Color.parseColor("#CCCCCC")
        strokePaint.color = color
        fillPaint.color = color
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val size = minOf(w, h) * 0.7f
        when (iconType) {
            SPROUT -> drawSprout(canvas, cx, cy, size)
            BOOK -> drawBook(canvas, cx, cy, size)
            MOUNTAIN -> drawMountain(canvas, cx, cy, size)
            BRUSH -> drawBrush(canvas, cx, cy, size)
            SEAL -> drawSeal(canvas, cx, cy, size)
            MOON -> drawMoon(canvas, cx, cy, size)
            STAR -> drawStar(canvas, cx, cy, size)
            FOUR_LEAVES -> drawFourLeaves(canvas, cx, cy, size)
            PATH_LINE -> drawPathLine(canvas, cx, cy, size)
            FLOWER -> drawFlower(canvas, cx, cy, size)
            HOUSE -> drawHouse(canvas, cx, cy, size)
            WAVE -> drawWave(canvas, cx, cy, size)
        }
    }

    private fun drawSprout(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        path.reset(); path.moveTo(cx, cy + s * 0.8f); path.lineTo(cx, cy - s * 0.2f)
        canvas.drawPath(path, strokePaint)
        path.reset(); path.moveTo(cx, cy - s * 0.1f); path.quadTo(cx - s * 0.6f, cy - s * 0.5f, cx - s * 0.15f, cy - s * 0.8f)
        canvas.drawPath(path, strokePaint)
        path.reset(); path.moveTo(cx, cy + s * 0.1f); path.quadTo(cx + s * 0.6f, cy - s * 0.3f, cx + s * 0.15f, cy - s * 0.7f)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawBook(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        val left = cx - s * 0.7f; val right = cx + s * 0.7f
        val top = cy - s * 0.5f; val bottom = cy + s * 0.5f
        path.reset(); path.moveTo(cx, top); path.lineTo(left, top + s * 0.15f); path.lineTo(left, bottom); path.lineTo(cx, bottom - s * 0.15f); path.close()
        canvas.drawPath(path, strokePaint)
        path.reset(); path.moveTo(cx, top); path.lineTo(right, top + s * 0.15f); path.lineTo(right, bottom); path.lineTo(cx, bottom - s * 0.15f); path.close()
        canvas.drawPath(path, strokePaint)
        canvas.drawLine(cx, top, cx, bottom - s * 0.15f, strokePaint)
    }

    private fun drawMountain(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        path.reset(); path.moveTo(cx - s * 0.8f, cy + s * 0.6f); path.lineTo(cx - s * 0.3f, cy - s * 0.4f); path.lineTo(cx, cy + s * 0.1f); path.lineTo(cx + s * 0.4f, cy - s * 0.7f); path.lineTo(cx + s * 0.8f, cy + s * 0.6f)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawBrush(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        path.reset(); path.moveTo(cx - s * 0.5f, cy - s * 0.7f); path.lineTo(cx + s * 0.2f, cy + s * 0.3f)
        canvas.drawPath(path, strokePaint)
        path.reset(); path.moveTo(cx + s * 0.1f, cy + s * 0.2f); path.quadTo(cx + s * 0.5f, cy + s * 0.5f, cx + s * 0.15f, cy + s * 0.7f); path.quadTo(cx - s * 0.1f, cy + s * 0.5f, cx + s * 0.1f, cy + s * 0.2f)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawSeal(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f; val half = s * 0.55f
        path.reset(); path.addRect(cx - half, cy - half, cx + half, cy + half, Path.Direction.CW)
        canvas.drawPath(path, strokePaint)
        canvas.drawLine(cx - half * 0.6f, cy - half * 0.2f, cx + half * 0.6f, cy - half * 0.2f, strokePaint)
        canvas.drawLine(cx - half * 0.6f, cy + half * 0.3f, cx + half * 0.6f, cy + half * 0.3f, strokePaint)
    }

    private fun drawMoon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        path.reset()
        path.addArc(cx - s * 0.6f, cy - s * 0.6f, cx + s * 0.6f, cy + s * 0.6f, 50f, 260f)
        path.addArc(cx - s * 0.15f, cy - s * 0.5f, cx + s * 0.65f, cy + s * 0.5f, 240f, -200f)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f; val outerR = s * 0.7f; val innerR = s * 0.28f
        path.reset()
        for (i in 0 until 5) {
            val outerAngle = Math.toRadians((-90.0 + i * 72.0))
            val innerAngle = Math.toRadians((-90.0 + (i + 0.5) * 72.0))
            val ox = cx + (outerR * Math.cos(outerAngle)).toFloat()
            val oy = cy + (outerR * Math.sin(outerAngle)).toFloat()
            val ix = cx + (innerR * Math.cos(innerAngle)).toFloat()
            val iy = cy + (innerR * Math.sin(innerAngle)).toFloat()
            if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
            path.lineTo(ix, iy)
        }
        path.close(); canvas.drawPath(path, strokePaint)
    }

    private fun drawFourLeaves(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f; val leafLen = s * 0.45f
        for (angle in listOf(0f, 90f, 180f, 270f)) {
            val rad = Math.toRadians(angle.toDouble())
            val endX = cx + (leafLen * Math.cos(rad)).toFloat()
            val endY = cy + (leafLen * Math.sin(rad)).toFloat()
            val ctrlR = leafLen * 0.7f
            path.reset(); path.moveTo(cx, cy)
            path.quadTo(cx + (ctrlR * Math.cos(Math.toRadians((angle + 40.0)))).toFloat(), cy + (ctrlR * Math.sin(Math.toRadians((angle + 40.0)))).toFloat(), endX, endY)
            path.quadTo(cx + (ctrlR * Math.cos(Math.toRadians((angle - 40.0)))).toFloat(), cy + (ctrlR * Math.sin(Math.toRadians((angle - 40.0)))).toFloat(), cx, cy)
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun drawPathLine(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        path.reset(); path.moveTo(cx - s * 0.7f, cy + s * 0.3f); path.lineTo(cx - s * 0.3f, cy - s * 0.5f); path.lineTo(cx + s * 0.1f, cy + s * 0.3f); path.lineTo(cx + s * 0.5f, cy - s * 0.6f); path.lineTo(cx + s * 0.7f, cy + s * 0.1f)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawFlower(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f; val petalR = s * 0.3f
        for (i in 0 until 5) {
            val angle = Math.toRadians((-90.0 + i * 72.0))
            val px = cx + (s * 0.4f * Math.cos(angle)).toFloat()
            val py = cy + (s * 0.4f * Math.sin(angle)).toFloat()
            path.reset(); path.addCircle(px, py, petalR, Path.Direction.CW)
            canvas.drawPath(path, strokePaint)
        }
        canvas.drawCircle(cx, cy, s * 0.12f, fillPaint)
    }

    private fun drawHouse(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        path.reset(); path.moveTo(cx, cy - s * 0.7f); path.lineTo(cx - s * 0.6f, cy - s * 0.1f); path.lineTo(cx + s * 0.6f, cy - s * 0.1f); path.close()
        canvas.drawPath(path, strokePaint)
        path.reset(); path.addRect(cx - s * 0.5f, cy - s * 0.1f, cx + s * 0.5f, cy + s * 0.6f, Path.Direction.CW)
        canvas.drawPath(path, strokePaint)
        path.reset(); path.addRect(cx - s * 0.12f, cy + s * 0.15f, cx + s * 0.12f, cy + s * 0.6f, Path.Direction.CW)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawWave(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size / 2f
        for (offset in listOf(-s * 0.3f, 0f, s * 0.3f)) {
            path.reset(); path.moveTo(cx - s * 0.7f, cy + offset)
            path.quadTo(cx - s * 0.35f, cy + offset - s * 0.3f, cx, cy + offset)
            path.quadTo(cx + s * 0.35f, cy + offset + s * 0.3f, cx + s * 0.7f, cy + offset)
            canvas.drawPath(path, strokePaint)
        }
    }
}
