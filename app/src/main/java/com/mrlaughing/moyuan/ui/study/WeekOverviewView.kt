package com.mrlaughing.moyuan.ui.study

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.util.formatMinutesShort
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 每周阅读柱状图（星期柱状图）：7 根胶囊柱代表周一至周日。
 * - 柱子高度按当日阅读分钟数等比缩放，空日显示浅色短桩，整周结构清晰。
 * - 当天列以「金色圆点 + 金色加粗星期标签」双重标记。
 * - 数据按 weekday 对齐（用 record.date.dayOfWeek），避免「只有部分天有记录」
 *   时柱子按列表下标错位到错误的星期。
 * 占据空间由外层 week_chart_height 决定，本视图不放大任何尺寸。
 */
class WeekOverviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val accentGreen = ContextCompat.getColor(context, R.color.accent_green)
    private val borderLight = ContextCompat.getColor(context, R.color.border_light)
    private val textSecondary = ContextCompat.getColor(context, R.color.text_secondary)
    private val quietGold = ContextCompat.getColor(context, R.color.quiet_gold)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textSecondary
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textSecondary
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val barRect = RectF()

    // 星期标签（周一..周日）
    private val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    // 对齐到周一到周日的 7 槽：[分钟数, 是否今天]
    private var slots: List<Slot> = List(7) { Slot(0, false) }
    private var maxMinutes: Int = 60

    private data class Slot(val minutes: Int, val isToday: Boolean)

    fun setRecords(records: List<DailyRecord>) {
        val today = LocalDate.now()
        val arr = Array(7) { Slot(0, false) }
        for (r in records) {
            // 周一=1..周日=7 → 下标 0..6，确保柱子落在正确星期
            val idx = r.date.dayOfWeek.value - 1
            if (idx in 0..6) {
                arr[idx] = Slot(r.readMinutes, r.date == today)
            }
        }
        slots = arr.toList()
        maxMinutes = records.maxOfOrNull { it.readMinutes }?.coerceAtLeast(30) ?: 60
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        val paddingH = 16f * density
        val paddingTop = 16f * density
        val paddingBottom = 18f * density
        val chartHeight = h - paddingTop - paddingBottom
        val chartWidth = w - paddingH * 2
        if (chartHeight <= 0 || chartWidth <= 0) return

        val barCount = 7
        val spacing = 8f * density
        val barWidth = (chartWidth - spacing * (barCount - 1)) / barCount
        val radius = barWidth * 0.45f
        val chartBottom = h - paddingBottom

        labelPaint.textSize = 11f * density
        valuePaint.textSize = 9f * density

        for (i in 0 until barCount) {
            val slot = slots[i]
            val cx = paddingH + i * (barWidth + spacing) + barWidth / 2f
            val left = cx - barWidth / 2f
            val right = cx + barWidth / 2f

            // 计算柱子高度（阅读日按占比；空日给浅色短桩）
            val ratio = if (maxMinutes > 0) slot.minutes.toFloat() / maxMinutes else 0f
            val barHeight = if (slot.minutes > 0) {
                (chartHeight * ratio).coerceIn(8f * density, chartHeight)
            } else {
                5f * density
            }
            val top = chartBottom - barHeight

            // 胶囊柱
            barPaint.color = if (slot.minutes > 0) accentGreen else borderLight
            barRect.set(left, top, right, chartBottom)
            canvas.drawRoundRect(barRect, radius, radius, barPaint)

            // 数值（仅阅读日，简洁形式）
            if (slot.minutes > 0) {
                canvas.drawText(slot.minutes.formatMinutesShort(), cx, top - 3f * density, valuePaint)
            }

            // 当天标记：柱下、标签上的金色圆点
            if (slot.isToday) {
                dotPaint.color = quietGold
                canvas.drawCircle(cx, chartBottom + 6f * density, 2.5f * density, dotPaint)
            }

            // 星期标签（今天为金色加粗）
            labelPaint.color = if (slot.isToday) quietGold else textSecondary
            labelPaint.isFakeBoldText = slot.isToday
            canvas.drawText(dayLabels[i], cx, h - 4f * density, labelPaint)
            labelPaint.isFakeBoldText = false
        }
    }
}
