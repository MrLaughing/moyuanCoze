package com.mrlaughing.moyuan.ui.study

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 月历热力图：按当日阅读分钟数着色
 * 热力等级：
 * - 无数据/0分钟：浅灰 #E8E0D0
 * - 1-30分钟：淡色 #C4B9A8
 * - 31-60分钟：中色 #8B7E6A  
 * - 61-120分钟：深色 #5C5242
 * - 120+分钟：最深 #2C2416
 */
class MonthHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 热力配色
    private val heatLevel0 = 0xFFE8E0D0.toInt()  // 无数据/0分钟
    private val heatLevel1 = 0xFFC4B9A8.toInt()   // 1-30分钟
    private val heatLevel2 = 0xFF8B7E6A.toInt()   // 31-60分钟
    private val heatLevel3 = 0xFF5C5242.toInt()   // 61-120分钟
    private val heatLevel4 = 0xFF2C2416.toInt()   // 120+分钟
    private val todayBorder = 0xFF2C2416.toInt()  // 今天边框

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    private val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val monthTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val weekDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        color = 0xFF78716C.toInt()
    }

    // 周标题
    private val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    private var currentYearMonth: YearMonth = YearMonth.now()
    private var records: Map<LocalDate, Int> = emptyMap() // 日期 -> 阅读分钟数
    private var today: LocalDate = LocalDate.now()

    // 点击回调
    var onMonthChangeListener: ((YearMonth) -> Unit)? = null

    fun setYearMonth(yearMonth: YearMonth) {
        currentYearMonth = yearMonth
        invalidate()
    }

    fun setRecords(records: Map<String, Int>) {
        // 转换：String日期 -> LocalDate -> 映射
        this.records = records.mapNotNull { (dateStr, minutes) ->
            try {
                LocalDate.parse(dateStr) to minutes
            } catch (_: Exception) {
                null
            }
        }.toMap()
        invalidate()
    }

    fun refresh() {
        today = LocalDate.now()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        val paddingH = 12f * density
        val paddingTop = 36f * density  // 月份标题
        val paddingBottom = 4f * density

        // 绘制月份标题
        val monthTitle = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy年M月"))
        monthTitlePaint.color = 0xFF2C2416.toInt()
        canvas.drawText(monthTitle, w / 2, 20f * density, monthTitlePaint)

        // 左右切换箭头（简单用文字）
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f * resources.displayMetrics.density
            color = 0xFF78716C.toInt()
        }
        canvas.drawText("◀", paddingH, 20f * density, arrowPaint)
        canvas.drawText("▶", w - paddingH, 20f * density, arrowPaint)

        // 计算日历网格
        val chartHeight = h - paddingTop - paddingBottom
        val chartWidth = w - paddingH * 2

        val cols = 7 // 周一至周日
        val rows = 6 // 最多6周
        val spacing = 2f * density
        val cellWidth = (chartWidth - spacing * (cols - 1)) / cols
        val cellHeight = (chartHeight - spacing * (rows - 1)) / rows

        // 计算本月第一天是周几（周一=0）
        val firstDayOfMonth = currentYearMonth.atDay(1)
        val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1) // 0=周一

        // 绘制星期标题
        for (i in 0 until 7) {
            val x = paddingH + i * (cellWidth + spacing) + cellWidth / 2
            val y = paddingTop - 8f * density
            canvas.drawText(weekDayLabels[i], x, y, weekDayPaint)
        }

        // 计算本月天数
        val daysInMonth = currentYearMonth.lengthOfMonth()

        // 绘制日期格子
        for (day in 1..daysInMonth) {
            val position = startDayOfWeek + day - 1
            val col = position % 7
            val row = position / 7

            val x = paddingH + col * (cellWidth + spacing)
            val y = paddingTop + row * (cellHeight + spacing)

            // 获取该日期的阅读分钟数
            val date = currentYearMonth.atDay(day)
            val minutes = records[date] ?: 0

            // 根据分钟数选择热力颜色
            val heatColor = getHeatColor(minutes)
            fillPaint.color = heatColor

            // 绘制格子
            val rect = RectF(x, y, x + cellWidth, y + cellHeight)
            canvas.drawRect(rect, fillPaint)

            // 如果是今天，绘制边框
            if (date == today) {
                borderPaint.color = todayBorder
                borderPaint.strokeWidth = 2f * density
                canvas.drawRect(rect, borderPaint)
            }

            // 绘制日期数字
            dateTextPaint.color = getTextColor(heatColor)
            val textX = x + cellWidth / 2
            val textY = y + cellHeight / 2 + 3f * density
            canvas.drawText(day.toString(), textX, textY, dateTextPaint)
        }
    }

    private fun getHeatColor(minutes: Int): Int {
        return when {
            minutes <= 0 -> heatLevel0
            minutes <= 30 -> heatLevel1
            minutes <= 60 -> heatLevel2
            minutes <= 120 -> heatLevel3
            else -> heatLevel4
        }
    }

    private fun getTextColor(bgColor: Int): Int {
        // 根据背景色决定文字颜色（深色背景用浅色文字）
        val r = (bgColor shr 16) and 0xFF
        val g = (bgColor shr 8) and 0xFF
        val b = bgColor and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        
        return if (luminance < 0.5) {
            0xFFF4F1EA.toInt() // 浅色文字
        } else {
            0xFF2C2416.toInt() // 深色文字
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
