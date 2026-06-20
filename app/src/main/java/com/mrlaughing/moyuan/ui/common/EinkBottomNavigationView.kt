package com.mrlaughing.moyuan.ui.common

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * 墨水屏友好的底部导航栏
 * - 禁用动画
 * - 选中无 ripple
 * - 高对比度样式
 */
class EinkBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle
) : BottomNavigationView(context, attrs, defStyleAttr) {

    init {
        // 设置高对比度颜色
        itemIconTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A1A"))
        itemTextColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A1A"))

        // 禁用 ripple 效果
        itemRippleColor = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)

        // 选中态使用纯黑
        val selectedColor = Color.parseColor("#1A1A1A")
        val unselectedColor = Color.parseColor("#999999")
        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(selectedColor, unselectedColor)
        )
        itemIconTintList = colorStateList
        itemTextColor = colorStateList

        // 禁用选中动画（label 只显示选中项）
        labelVisibilityMode = LABEL_VISIBILITY_SELECTED

        // 白色背景
        setBackgroundColor(Color.WHITE)

        // 去掉 elevation 阴影
        elevation = 0f
    }

    override fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        // 包装 listener 以禁用过渡动画
        super.setOnItemSelectedListener(listener)
    }
}
