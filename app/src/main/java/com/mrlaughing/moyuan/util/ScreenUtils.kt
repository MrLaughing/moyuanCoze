package com.mrlaughing.moyuan.util

import android.content.Context
import android.content.res.Resources

/**
 * 屏幕尺寸和布局工具类
 * 支持全尺寸自适应布局适配（5寸手机到13.3寸电纸书）
 */
object ScreenUtils {

    /**
     * 获取最小屏幕宽度（dp）
     * 这是Android推荐的用于确定布局配置的基准值
     */
    fun getSmallestScreenWidthDp(context: Context): Int {
        return context.resources.configuration.smallestScreenWidthDp
    }

    /**
     * 获取屏幕宽度（dp）
     */
    fun getScreenWidthDp(context: Context): Int {
        return context.resources.configuration.screenWidthDp
    }

    /**
     * 获取屏幕高度（dp）
     */
    fun getScreenHeightDp(context: Context): Int {
        return context.resources.configuration.screenHeightDp
    }

    /**
     * 计算网格列数
     * @param context 上下文
     * @param minCardWidthDp 最小卡片宽度（dp）
     * @param sidePaddingDp 两侧边距总和（dp）
     * @param cardSpacingDp 卡片间距总和（dp）
     * @return 推荐列数
     */
    fun calculateGridColumns(
        context: Context,
        minCardWidthDp: Int,
        sidePaddingDp: Int,
        cardSpacingDp: Int
    ): Int {
        val screenWidthDp = getScreenWidthDp(context)
        val availableWidth = screenWidthDp - sidePaddingDp
        val columns = (availableWidth + cardSpacingDp) / (minCardWidthDp + cardSpacingDp)
        return columns.coerceAtLeast(2)
    }

    /**
     * 获取图鉴推荐网格列数
     * 
     * 档位划分：
     * - sw < 360dp: 2列 (小屏手机)
     * - sw 360-479dp: 3列 (标准6寸)
     * - sw 480-599dp: 3列 (8寸)
     * - sw 600-719dp: 4列 (10.3寸)
     * - sw >= 720dp: 5列 (13.3寸)
     */
    fun getRecommendedGridColumns(context: Context): Int {
        val smallestWidth = getSmallestScreenWidthDp(context)
        return when {
            smallestWidth < 360 -> 2  // 小屏手机
            smallestWidth < 600 -> 3 // 标准6寸/7寸/8寸
            smallestWidth < 720 -> 4 // 10.3寸电纸书/小平板
            else -> 5                // 13.3寸电纸书/大平板
        }
    }

    /**
     * 获取成就推荐网格列数
     * 成就列数比图鉴多1列
     */
    fun getAchievementGridColumns(context: Context): Int {
        val smallestWidth = getSmallestScreenWidthDp(context)
        return if (smallestWidth < 360) 4 else 5
    }

    /**
     * 判断是否为电纸书设备（大屏、高分辨率）
     */
    fun isEinkDevice(context: Context): Boolean {
        val smallestWidth = getSmallestScreenWidthDp(context)
        return smallestWidth >= 600
    }

    /**
     * 判断是否为平板设备
     */
    fun isTablet(context: Context): Boolean {
        val smallestWidth = getSmallestScreenWidthDp(context)
        return smallestWidth >= 600
    }

    /**
     * 判断是否为小屏设备（5寸以下）
     */
    fun isSmallScreen(context: Context): Boolean {
        val smallestWidth = getSmallestScreenWidthDp(context)
        return smallestWidth < 360
    }

    /**
     * 将dp值转换为px值
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * 将px值转换为dp值
     */
    fun pxToDp(context: Context, px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }
}

