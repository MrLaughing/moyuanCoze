package com.mrlaughing.moyuan.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * 墨水屏友好的底部导航栏
 * - 纯文字显示，无图标
 * - 选中态下划线样式
 * - 高对比度
 */
class EinkBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val underlinePaint = Paint().apply {
        color = Color.parseColor("#1A1A1A")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var selectedIndex = 0
    private var itemCount = 0

    init {
        // 纯文字模式
        itemIconTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        
        // 文字颜色：选中纯黑，未选中浅灰
        val selectedColor = Color.parseColor("#1A1A1A")
        val unselectedColor = Color.parseColor("#999999")
        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(selectedColor, unselectedColor)
        )
        itemTextColor = colorStateList

        // 禁用 ripple 效果
        itemRippleColor = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)

        // 显示所有文字标签
        labelVisibilityMode = LABEL_VISIBILITY_LABELED

        // 浅灰背景
        setBackgroundColor(Color.parseColor("#CCCCCC"))

        // 去掉 elevation 阴影
        elevation = 0f
        
        // 获取item数量
        itemCount = menu.size()

        // 监听选中变化
        setOnItemSelectedListener { item ->
            selectedIndex = item.itemId
            invalidate() // 重绘下划线
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawUnderline(canvas)
    }

    private fun drawUnderline(canvas: Canvas) {
        val width = width
        val height = height
        if (width == 0 || height == 0 || itemCount == 0) return

        val itemWidth = width.toFloat() / itemCount
        val selectedItemIndex = getSelectedItemIndex()
        
        // 计算选中项下划线的位置
        val startX = selectedItemIndex * itemWidth + itemWidth / 2 - 20f
        val endX = selectedItemIndex * itemWidth + itemWidth / 2 + 20f
        val y = height - 8f

        canvas.drawLine(startX, y, endX, y, underlinePaint)
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

    override fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        super.setOnItemSelectedListener(listener)
    }
}
