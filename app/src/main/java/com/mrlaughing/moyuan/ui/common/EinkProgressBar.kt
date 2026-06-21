package com.mrlaughing.moyuan.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.mrlaughing.moyuan.R

/**
 * 墨水屏友好的进度条：无动画、纯墨色、8dp 高
 */
class EinkProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8E0D0")  // 暖色轨道
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2416")  // 浓墨进度
        style = Paint.Style.FILL
    }

    private val rect = RectF()

    /** 进度值 0.0 ~ 1.0 */
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /** 轨道颜色 */
    var trackColor: Int = Color.parseColor("#E8E0D0")
        set(value) {
            field = value
            trackPaint.color = value
            invalidate()
        }

    /** 进度颜色 */
    var progressColor: Int = Color.parseColor("#2C2416")
        set(value) {
            field = value
            progressPaint.color = value
            invalidate()
        }

    init {
        // 从 XML 属性读取
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.EinkProgressBar)
            progress = typedArray.getFloat(R.styleable.EinkProgressBar_progress, 0f)
            trackColor = typedArray.getColor(
                R.styleable.EinkProgressBar_trackColor,
                Color.parseColor("#E8E0D0")
            )
            progressColor = typedArray.getColor(
                R.styleable.EinkProgressBar_progressColor,
                Color.parseColor("#2C2416")
            )
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h / 2f

        // 绘制轨道（圆角矩形）
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        // 绘制进度（圆角矩形）
        if (progress > 0f) {
            val progressWidth = w * progress
            rect.set(0f, 0f, progressWidth, h)
            canvas.drawRoundRect(rect, radius, radius, progressPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (8 * resources.displayMetrics.density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}
