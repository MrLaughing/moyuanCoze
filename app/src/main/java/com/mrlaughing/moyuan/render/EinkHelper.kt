package com.mrlaughing.moyuan.render

import android.app.Activity
import android.view.View
import android.view.WindowManager

/**
 * 墨水屏刷新辅助工具
 */
object EinkHelper {

    const val MODE_PARTIAL = 0   // 局部刷新（无闪烁）
    const val MODE_FULL = 1      // 全刷（消除残影）

    /**
     * 刷新指定 View
     */
    fun refresh(view: View, mode: Int) {
        when (mode) {
            MODE_FULL -> {
                view.invalidate()
                // 墨水屏设备通过全局刷新请求来触发全刷
                try {
                    val activity = view.context as? Activity
                    activity?.window?.let { window ->
                        // 对于文石等设备，通过 WindowManager 标记全刷
                        // 通用方案：直接 invalidate 并设置稍长的绘制延迟
                        view.postInvalidateDelayed(16)
                    }
                } catch (_: Exception) {
                    // 非 Activity Context 时忽略
                }
            }
            MODE_PARTIAL -> {
                view.invalidate()
            }
        }
    }

    /**
     * 在 Activity 级别禁用所有动画（墨水屏优化）
     */
    fun disableAnimations(activity: Activity) {
        // 禁用窗口动画
        activity.window.setWindowAnimations(0)

        // 禁用过渡动画
        activity.overridePendingTransition(0, 0)

        // 设置硬件加速关闭（墨水屏更适合软件渲染）
        // 注意：不全局关闭，只针对特定 View
    }

    /**
     * 全局禁用动画（Application 级别）
     */
    fun disableAnimations(context: android.content.Context) {
        // 通过系统设置全局禁用动画
        // 注意：修改系统设置需要 WRITE_SECURE_SETTINGS 权限，普通应用无法获取
        // 此处仅做占位，实际需系统签名或 adb 授权
    }

    /**
     * 强制全刷整个 Activity
     * 某些墨水屏设备支持通过特定广播触发全刷
     */
    fun forceFullRefresh(activity: Activity) {
        try {
            // 方式1：文石设备专用广播
            val intent = android.content.Intent("com.onyx.action.FULL_REFRESH")
            activity.sendBroadcast(intent)
        } catch (_: Exception) {
            // 不支持则使用通用方案
        }

        // 通用方案：通过快速黑白切换模拟全刷
        val decorView = activity.window.decorView
        decorView.setBackgroundColor(0xFF000000.toInt())
        decorView.postDelayed({
            decorView.setBackgroundColor(0xFFFFFFFF.toInt())
            decorView.postDelayed({
                decorView.setBackgroundColor(0xFFF5F5F5.toInt())
            }, 16)
        }, 16)
    }
}
