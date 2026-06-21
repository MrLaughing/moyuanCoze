package com.mrlaughing.moyuan.util

/**
 * 墨园全局常量
 *
 * 数值来源：墨园-植物图鉴设计 + 墨园-核心引擎详细设计
 */
object Constants {

    // ── 等级阈值（分钟） ──────────────────────
    /** 各等级所需累计有效灌溉分钟数，索引=等级-1 */
    val LEVEL_THRESHOLDS = intArrayOf(
        0,       // Lv1: 0 分钟
        120,     // Lv2: 120 分钟
        480,     // Lv3: 480 分钟
        1200,    // Lv4: 1200 分钟
        2400     // Lv5: 2400 分钟
    )

    const val MAX_LEVEL = 5

    // ── 枯萎阈值（天） ──────────────────────
    /** 连续未阅读天数 → 枯萎阶段 */
    const val WITHER_FADE_DAYS = 2       // 2天 → 初淡
    const val WITHER_WITHER_DAYS = 4     // 4天 → 渐枯
    const val WITHER_SEVERE_DAYS = 7     // 7天 → 将枯
    const val WITHER_DEAD_DAYS = 14      // 14天 → 枯寂

    // ── 恢复保留比例 ──────────────────────────
    const val RETENTION_FADE = 0.90f     // 初淡恢复保留90%
    const val RETENTION_WITHER = 0.75f   // 渐枯恢复保留75%
    const val RETENTION_SEVERE = 0.50f   // 将枯恢复保留50%
    const val RETENTION_DEAD = 0.30f     // 枯寂恢复保留30%

    // ── 复活加成 ──────────────────────────────
    const val REVIVE_FIRST_DAY_BONUS = 1.5f  // 复活首日灌溉×1.5

    // ── 路径匹配加成 ──────────────────────────
    const val PATH_MATCH_MULTIPLIER = 1.2f   // 路径匹配×1.2

    // ── 路径（图鉴筛选Tab） ──────────────────
    const val PATH_ALL = 0
    const val PATH_JIMO = 1       // 积墨
    const val PATH_BINGZHU = 2    // 秉烛
    const val PATH_SUIHAN = 3     // 岁寒
    const val PATH_XUNFANG = 4    // 寻芳
    const val PATH_HIDDEN = 5     // 隐藏

    val PATH_NAMES = arrayOf("全部", "积墨", "秉烛", "岁寒", "寻芳", "隐藏")

    // ── 天气概率 ──────────────────────────────
    const val WEATHER_CLEAR_PROB = 0.60f       // 晴 60%
    const val WEATHER_OVERCAST_PROB = 0.15f    // 阴 15%
    const val WEATHER_FOGGY_PROB = 0.10f       // 雾 10%
    const val WEATHER_SPRING_RAIN_PROB = 0.08f // 春雨 8%
    const val WEATHER_MOONLIT_PROB = 0.05f     // 月夜 5%
    const val WEATHER_FIRST_SNOW_PROB = 0.02f  // 初雪 2%

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
    const val RARITY_COMMON = 1       // 常见
    const val RARITY_RARE = 2         // 稀有
    const val RARITY_LEGENDARY = 3    // 传说
    const val RARITY_HIDDEN = 4       // 隐藏
}
