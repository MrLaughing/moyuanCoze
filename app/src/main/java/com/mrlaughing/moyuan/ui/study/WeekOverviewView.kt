package com.mrlaughing.moyuan.ui.study

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.time.LocalDate

/**
 * 周概览方格图：7个方格，每格代表一天，有阅读则填满
 */
class WeekOverviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private var records: List<DailyRecord> = emptyList()

    fun setRecords(records: List<DailyRecord>) {
        this.records = records
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0 || records.isEmpty()) return

        val padding = 4f * resources.displayMetrics.density
        val cellWidth = (w - padding * 8) / 7  // 7格 + 间距
        val cellHeight = h - 40f * resources.displayMetrics.density  // 留出底部文字空间

        for (i in records.indices) {
            val record = records[i]
            val left = padding + i * (cellWidth + padding)
            val top = padding
            val right = left + cellWidth
            val bottom = top + cellHeight

            val rect = RectF(left, top, right, bottom)
            val paint = if (record.hasRead && record.readMinutes > 0) filledPaint else emptyPaint
            val cornerRadius = 4f * resources.displayMetrics.density
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            // 底部显示星期几
            val dayOfWeek = record.date.dayOfWeek.value  // 1=Mon ... 7=Sun
            val dayLabel = when (dayOfWeek) {
                1 -> "一"
                2 -> "二"
                3 -> "三"
                4 -> "四"
                5 -> "五"
                6 -> "六"
                7 -> "日"
                else -> ""
            }
            val textX = left + cellWidth / 2
            val textY = bottom + 24f * resources.displayMetrics.density
            canvas.drawText(dayLabel, textX, textY, textPaint)
        }
    }
}
