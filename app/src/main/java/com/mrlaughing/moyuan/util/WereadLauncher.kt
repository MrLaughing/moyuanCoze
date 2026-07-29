package com.mrlaughing.moyuan.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 微信读书 App 跳转工具
 *
 * 官方 URL Scheme（来自微信读书官方 skill 文档）：
 * - 打开书籍并回到上次阅读进度：weread://reading?bId={bookId}
 * - 书籍详情页：weread://reading?bId={bookId}&type=1
 *
 * 回退：未安装时打开网页版阅读页。
 */
object WereadLauncher {

    private const val WEB_BASE = "https://weread.qq.com/web/reader/"

    /** 跳转到微信读书阅读界面（接续上次进度）；未安装时打开网页版 */
    fun openBook(context: Context, bookId: String) {
        if (bookId.isBlank()) return
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("weread://reading?bId=$bookId")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(appIntent)
            return
        } catch (e: ActivityNotFoundException) {
            // 未安装微信读书 → 回退网页版
        }
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_BASE + bookId)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未检测到可用的浏览器或微信读书", Toast.LENGTH_SHORT).show()
        }
    }
}
