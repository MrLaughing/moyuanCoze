package com.mrlaughing.moyuan.ui.study

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 每周阅读柱状图：7根柱子代表周一到周日
 * 柱子高度按当日阅读分钟数等比缩放
 * 支持显示指定周的日期范围
 */
class WeekOverviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 墨色配色
    private val inkDark = 0xFF2C2416.toInt()      // 有阅读的柱子
    private val inkLight = 0xFFD4C9B8.toInt()      // 无阅读的柱子
    private val textPrimary = 0xFF2C2416.toInt()   // 文字颜色
    private val textSecondary = 0xFF78716C.toInt() // 次要文字

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textSecondary
        textSize = 11f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary
        textSize = 10f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    // 星期标签
    private val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    
    private var records: List<DailyRecord> = emptyList()
    private var maxMinutes: Int = 60 // 默认最大值，避免除零
    
    // 日期范围格式化
    private val dateFormatter = DateTimeFormatter.ofPattern("M.d")

    fun setRecords(records: List<DailyRecord>) {
        this.records = records
        // 计算最大阅读分钟数作为基准
        maxMinutes = records.maxOfOrNull { it.readMinutes }?.coerceAtLeast(30) ?: 60
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        val paddingH = 16f * density  // 水平内边距
        val paddingTop = 8f * density   // 顶部间距（给数值留空间）
        val paddingBottom = 20f * density // 底部间距（给标签留空间）
        
        val chartHeight = h - paddingTop - paddingBottom
        val chartWidth = w - paddingH * 2
        
        // 计算柱宽和间距
        val barCount = 7
        val spacing = 8f * density  // 柱子间距
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (chartWidth - totalSpacing) / barCount

        // 绘制柱子
        for (i in 0 until barCount) {
            val record = records.getOrNull(i)
            val readMinutes = record?.readMinutes ?: 0
            val hasRead = readMinutes > 0
            val recordDate = record?.date

            // 计算柱子位置
            val left = paddingH + i * (barWidth + spacing)
            val right = left + barWidth

            // 计算柱子高度（按比例）
            val barHeightRatio = if (maxMinutes > 0) {
                readMinutes.toFloat() / maxMinutes
            } else {
                0f
            }
            val actualBarHeight = (chartHeight * barHeightRatio).coerceIn(4f * density, chartHeight)
            
            // 底部对齐
            val top = h - paddingBottom - actualBarHeight
            val bottom = h - paddingBottom

            // 柱子圆角矩形
            barPaint.color = if (hasRead) inkDark else inkLight
            val cornerRadius = 3f * density
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)

            // 柱子顶部显示阅读分钟数（>0时）
            if (hasRead && readMinutes > 0) {
                val valueText = "${readMinutes}m"
                val valueY = top - 4f * density
                canvas.drawText(valueText, left + barWidth / 2, valueY, valuePaint)
            }

            // 底部显示日期数字（替代星期标签，更直观）
            val displayText = if (recordDate != null) {
                recordDate.format(dateFormatter).substringAfter(".")
            } else {
                dayLabels[i]
            }
            val labelY = h - 4f * density
            canvas.drawText(displayText, left + barWidth / 2, labelY, labelPaint)
        }
    }
}
