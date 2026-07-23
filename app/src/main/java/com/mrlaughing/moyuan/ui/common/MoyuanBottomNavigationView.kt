package com.mrlaughing.moyuan.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mrlaughing.moyuan.R

/** 墨园底部导航栏：白色表面、品牌绿选中态和轻量触摸反馈。 */
class MoyuanBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val dividerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.border)  // border色
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = false
    }

    private val dotPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.accent_green)  // ink_dark
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var itemCount = 0

    init {
        // 图标缩放 68%：24dp * 0.68 ≈ 16dp
        itemIconSize = resources.getDimensionPixelSize(R.dimen.nav_icon_size)

        // 图标颜色
        itemIconTintList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.accent_green),
                ContextCompat.getColor(context, R.color.text_secondary)   // ink_medium
            )
        )

        // 文字颜色
        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.accent_green),
                ContextCompat.getColor(context, R.color.text_secondary)   // ink_medium 未选中
            )
        )
        itemTextColor = colorStateList

        itemRippleColor = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.accent_green_soft)
        )

        // 显示所有标签
        labelVisibilityMode = LABEL_VISIBILITY_LABELED

        setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        elevation = 4f * resources.displayMetrics.density

        // 获取item数量
        itemCount = menu.size()

        // 监听选中变化
        setOnItemSelectedListener { _ ->
            invalidate()
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 顶部细分割线
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, dividerPaint)
        // 选中态墨点指示器
        drawInkDot(canvas)
    }

    private fun drawInkDot(canvas: Canvas) {
        if (width == 0 || height == 0 || itemCount == 0) return

        val itemWidth = width.toFloat() / itemCount
        val selectedItemIndex = getSelectedItemIndex()

        // 墨点位置：图标上方
        val cx = selectedItemIndex * itemWidth + itemWidth / 2
        val density = resources.displayMetrics.density
        val cy = 6f * density
        val radius = 2.5f * density

        canvas.drawCircle(cx, cy, radius, dotPaint)
    }

    private fun getSelectedItemIndex(): Int {
        val selectedItemId = selectedItemId
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == selectedItemId) {
                return i
            }
        }
        return 0
    }
}
