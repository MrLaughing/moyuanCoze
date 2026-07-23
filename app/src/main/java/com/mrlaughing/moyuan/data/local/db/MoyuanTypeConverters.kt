package com.mrlaughing.moyuan.data.local.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Room TypeConverter
 * 处理 LocalDate ↔ String 等类型的互转
 */
class MoyuanTypeConverters {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * LocalDate → String (yyyy-MM-dd)
     */
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(formatter)
    }

    /**
     * String (yyyy-MM-dd) → LocalDate
     */
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, formatter) }
    }

    /**
     * List<String> → String (逗号分隔)
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    /**
     * String (逗号分隔) → List<String>
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
    }
}
