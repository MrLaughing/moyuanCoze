package com.mrlaughing.moyuan.util

/**
 * 墨园全局常量（简化版）
 *
 * 已废弃：枯萎、路径、天气概率、灌溉加成等旧版游戏数值
 */
object Constants {

    // ── 等级阈值（分钟） ──────────────────────
    /** 各等级所需累计分钟数，索引=等级-1 */
    val LEVEL_THRESHOLDS = intArrayOf(
        0,       // Lv1: 0 分钟
        120,     // Lv2: 120 分钟
        480,     // Lv3: 480 分钟
        1200,    // Lv4: 1200 分钟
        2400     // Lv5: 2400 分钟
    )

    const val MAX_LEVEL = 5

    // ── 路径 ──────────────────
    const val PATH_ALL = 0
    const val PATH_JIMO = 1
    const val PATH_BINGZHU = 2
    const val PATH_SUIHAN = 3
    const val PATH_XUNFANG = 4
    const val PATH_HIDDEN = 5

    // ── 季节月份 ──────────────────────────────
    val SPRING_MONTHS = intArrayOf(3, 4)
    val SUMMER_MONTHS = intArrayOf(5, 6, 7)
    val AUTUMN_MONTHS = intArrayOf(8, 9)
    val WINTER_MONTHS = intArrayOf(10, 11, 12, 1, 2)

    // ── 同步 ──────────────────────────────────
    const val SYNC_DEFAULT_HOUR = 8
    const val SYNC_DEFAULT_MINUTE = 0
    const val SYNC_ACTION = "com.mrlaughing.moyuan.ACTION_DAILY_SYNC"
    const val SYNC_REQUEST_CODE = 10001

    // ── 渲染 ──────────────────────────────────
    const val GARDEN_FOREGROUND_COUNT = 6
    const val GARDEN_BACKGROUND_COUNT = 6
    const val PLANT_ASSET_PREFIX = "plants/"
    const val PLANT_ASSET_SUFFIX = ".png"

    // ── 日期格式 ──────────────────────────────
    const val DATE_FORMAT_CN = "M月d日"
    const val DATE_FORMAT_ISO = "yyyy-MM-dd"
    const val TIME_FORMAT_CN = "H:mm"

    // ── 夜间时段 ──────────────────────────────
    const val NIGHT_START_HOUR = 22
    const val NIGHT_END_HOUR = 6

    // ── 周概览方格数 ──────────────────────────
    const val WEEK_OVERVIEW_DAYS = 7

    // ── 稀有度 ────────────────────────────────
    const val RARITY_COMMON = 1
    const val RARITY_RARE = 2
    const val RARITY_LEGENDARY = 3
}
