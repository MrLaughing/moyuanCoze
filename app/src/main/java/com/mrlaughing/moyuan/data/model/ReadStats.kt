package com.mrlaughing.moyuan.data.model

data class ReadStats(
    val todayMinutes: Int,
    val streakDays: Int,
    val nightReadDays: Int,
    val accumulatedMinutes: Int,
    val booksRead: Int,
    val hasNightReadToday: Boolean
)
