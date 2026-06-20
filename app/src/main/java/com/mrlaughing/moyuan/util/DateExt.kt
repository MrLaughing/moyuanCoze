package com.mrlaughing.moyuan.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * LocalDate / LocalDateTime 扩展函数
 */

/** 格式化为中文日期，如 "3月15日" */
fun LocalDate.formatCN(): String {
    return format(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_CN))
}

/** 格式化为 ISO 日期，如 "2025-03-15" */
fun LocalDate.formatISO(): String {
    return format(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_ISO))
}

/** 判断是否为夜间时段 */
fun LocalDateTime.isNight(): Boolean {
    val hour = hour
    return hour >= Constants.NIGHT_START_HOUR || hour < Constants.NIGHT_END_HOUR
}

/** 计算与另一个日期之间相差的天数（绝对值） */
fun LocalDate.daysBetween(other: LocalDate): Long {
    return Math.abs(ChronoUnit.DAYS.between(this, other))
}

/** 判断是否为今天 */
fun LocalDate.isToday(): Boolean = this == LocalDate.now()

/** 判断是否为昨天 */
fun LocalDate.isYesterday(): Boolean = this == LocalDate.now().minusDays(1)

/** 获取本周一的日期 */
fun LocalDate.weekMonday(): LocalDate {
    return minusDays(dayOfWeek.value.toLong() - 1)
}

/** 获取过去 N 天的日期列表（含今天） */
fun LocalDate.pastDays(n: Int): List<LocalDate> {
    return (0 until n).map { minusDays(it.toLong()) }.reversed()
}

/** 格式化分钟数为 "X小时Y分钟" */
fun Int.formatMinutes(): String {
    val h = this / 60
    val m = this % 60
    return if (h > 0) "${h}小时${m}分钟" else "${m}分钟"
}

/** 格式化分钟数为简洁形式 "Xh Ym" */
fun Int.formatMinutesShort(): String {
    val h = this / 60
    val m = this % 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

/** 格式化 LocalTime 为 "H:mm" */
fun LocalTime.formatCN(): String {
    return format(DateTimeFormatter.ofPattern(Constants.TIME_FORMAT_CN))
}
