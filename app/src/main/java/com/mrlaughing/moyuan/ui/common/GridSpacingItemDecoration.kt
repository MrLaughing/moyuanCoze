package com.mrlaughing.moyuan.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 网格间距装饰器
 * 为GridLayoutManager的item添加均匀间距
 *
 * @param spanCount 列数
 * @param spacingDp 间距（dp）
 * @param includeEdge 是否在左右边缘也加间距（RecyclerView自身有padding时设为false）
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacingPx: Int,
    private val includeEdge: Boolean = false
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacingPx - column * spacingPx / spanCount
            outRect.right = (column + 1) * spacingPx / spanCount
        } else {
            outRect.left = column * spacingPx / spanCount
            outRect.right = spacingPx - (column + 1) * spacingPx / spanCount
        }

        if (position >= spanCount) {
            outRect.top = spacingPx
        }
    }
}
