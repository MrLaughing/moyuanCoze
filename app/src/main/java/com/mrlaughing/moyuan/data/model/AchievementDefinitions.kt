package com.mrlaughing.moyuan.data.model

/**
 * 成就定义 - 12种成就对应12个水墨线条图标
 */
object AchievementDefinitions {
    const val CATEGORY_READING = "READING"
    const val CATEGORY_GROWTH = "GROWTH"
    const val CATEGORY_MILESTONE = "MILESTONE"
    const val CATEGORY_ALL = "ALL"
    
    data class AchievementDef(
        val id: String,
        val category: String,
        val name: String,
        val description: String,
        val condition: String,
        val targetValue: Int
    )
    
    val ALL_ACHIEVEMENTS = listOf(
        AchievementDef("first_sync", CATEGORY_READING, "初心", "墨香初识，书卷初开", "首次同步", 1),
        AchievementDef("read_10_books", CATEGORY_READING, "破万卷", "读书破万卷，下笔如有神", "阅读10本书", 10),
        AchievementDef("night_read_30", CATEGORY_READING, "夜读", "夜深灯火共书窗", "夜读30天", 30),
        AchievementDef("read_100_hours", CATEGORY_READING, "登高", "会当凌绝顶，一览众山小", "阅读100小时", 100),
        AchievementDef("read_50_books", CATEGORY_READING, "执笔", "笔落惊风雨，诗成泣鬼神", "阅读50本书", 50),
        AchievementDef("read_500_hours", CATEGORY_READING, "墨海", "墨海无涯，孜孜不倦", "阅读500小时", 500),
        AchievementDef("first_sprout", CATEGORY_GROWTH, "留印", "墨园初开，留印为证", "解锁首株植物", 1),
        AchievementDef("unlock_10", CATEGORY_GROWTH, "寻芳", "寻芳不觉醉流霞", "解锁10株植物", 10),
        AchievementDef("unlock_all", CATEGORY_GROWTH, "归园", "归园田居，心远地自偏", "解锁全部27株植物", 27),
        AchievementDef("reach_lv5", CATEGORY_GROWTH, "四季", "四时之景不同，而乐亦无穷", "植物达到Lv5", 1),
        AchievementDef("streak_7", CATEGORY_MILESTONE, "不辍", "七日持之以恒，笔耕不辍", "连续阅读7天", 7),
        AchievementDef("streak_30", CATEGORY_MILESTONE, "星辰", "三十日仰望星空，星辰指路", "连续阅读30天", 30)
    )
    
    val CATEGORY_LABELS = mapOf(
        CATEGORY_ALL to "全部",
        CATEGORY_READING to "阅读",
        CATEGORY_GROWTH to "养成",
        CATEGORY_MILESTONE to "里程碑"
    )
}
