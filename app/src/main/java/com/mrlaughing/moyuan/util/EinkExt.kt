package com.mrlaughing.moyuan.util

import android.view.View
import com.mrlaughing.moyuan.render.EinkHelper

/**
 * View 墨水屏扩展函数
 */

/** 墨水屏友好的 invalidate：不触发动画，直接刷新 */
fun View.einkInvalidate() {
    EinkHelper.refresh(this, EinkHelper.MODE_PARTIAL)
}

/** 墨水屏全刷 */
fun View.einkFullRefresh() {
    EinkHelper.refresh(this, EinkHelper.MODE_FULL)
}
