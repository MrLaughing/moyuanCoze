package com.mrlaughing.moyuan.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mrlaughing.moyuan.R

/**
 * 墨园底部导航栏 - 传统文人风
 * - 宣纸色背景 + 顶部细分割线
 * - 毛笔笔触风格图标
 * - 选中态：浓墨色 + 墨点指示器
 * - 未选中态：中灰色
 * - 导航标签用系统无衬线字体
 */
class EinkBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val dividerPaint = Paint().apply {
        color = Color.parseColor("#D4C9B8")  // border色
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = false
    }

    private val dotPaint = Paint().apply {
        color = Color.parseColor("#2C2416")  // ink_dark
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var selectedIndex = 0
    private var itemCount = 0

    init {
        // 图标颜色
        itemIconTintList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.parseColor("#2C2416"),  // ink_dark
                Color.parseColor("#57534E")   // ink_medium
            )
        )

        // 文字颜色
        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.parseColor("#2C2416"),  // ink_dark 选中
                Color.parseColor("#57534E")   // ink_medium 未选中
            )
        )
        itemTextColor = colorStateList

        // 禁用 ripple 效果
        itemRippleColor = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)

        // 显示所有标签
        labelVisibilityMode = LABEL_VISIBILITY_LABELED

        // 宣纸色背景
        setBackgroundColor(Color.parseColor("#F4F1EA"))

        // 去掉 elevation 阴影
        elevation = 0f

        // 获取item数量
        itemCount = menu.size()

        // 监听选中变化
        setOnItemSelectedListener { item ->
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
        val cy = 8f
        val radius = 2.5f

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
