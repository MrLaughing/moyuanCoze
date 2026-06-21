package com.mrlaughing.moyuan.data.model

/**
 * 成就定义
 * 包含所有成就的静态定义信息
 */
object AchievementDefinitions {
    
    // 成就分类
    const val CATEGORY_READING = "READING"
    const val CATEGORY_GROWTH = "GROWTH"
    const val CATEGORY_MILESTONE = "MILESTONE"
    const val CATEGORY_ALL = "ALL"
    
    /**
     * 成就定义数据类
     */
    data class AchievementDef(
        val id: String,
        val category: String,
        val name: String,
        val description: String,
        val condition: String,
        val targetValue: Int
    )
    
    /**
     * 所有成就定义列表
     */
    val ALL_ACHIEVEMENTS = listOf(
        // 阅读成就
        AchievementDef(
            id = "first_sync",
            category = CATEGORY_READING,
            name = "初芽",
            description = "墨香初识，书卷初开",
            condition = "首次同步",
            targetValue = 1
        ),
        AchievementDef(
            id = "bookworm",
            category = CATEGORY_READING,
            name = "书虫",
            description = "腹有诗书气自华",
            condition = "阅读10本",
            targetValue = 10
        ),
        AchievementDef(
            id = "book_collector",
            category = CATEGORY_READING,
            name = "藏书家",
            description = "藏书万卷，胸中自足",
            condition = "阅读50本",
            targetValue = 50
        ),
        AchievementDef(
            id = "library_master",
            category = CATEGORY_READING,
            name = "书阁主人",
            description = "坐拥书阁，墨韵悠长",
            condition = "阅读100本",
            targetValue = 100
        ),
        AchievementDef(
            id = "night_reader",
            category = CATEGORY_READING,
            name = "夜读者",
            description = "夜深灯火共书窗",
            condition = "夜读30天",
            targetValue = 30
        ),
        AchievementDef(
            id = "morning_reader",
            category = CATEGORY_READING,
            name = "晨读客",
            description = "晨光熹微，书声朗朗",
            condition = "晨读30天",
            targetValue = 30
        ),
        AchievementDef(
            id = "ink_hours_100",
            category = CATEGORY_READING,
            name = "墨时累积",
            description = "积墨成塔，跬步千里",
            condition = "阅读100小时",
            targetValue = 100
        ),
        AchievementDef(
            id = "ink_hours_500",
            category = CATEGORY_READING,
            name = "墨时深厚",
            description = "墨海无涯，孜孜不倦",
            condition = "阅读500小时",
            targetValue = 500
        ),
        
        // 养成成就
        AchievementDef(
            id = "first_sprout",
            category = CATEGORY_GROWTH,
            name = "萌芽破土",
            description = "一株新绿，破土而出",
            condition = "解锁首株",
            targetValue = 1
        ),
        AchievementDef(
            id = "ink_forest",
            category = CATEGORY_GROWTH,
            name = "墨林初成",
            description = "墨林初成，郁郁葱葱",
            condition = "解锁10株",
            targetValue = 10
        ),
        AchievementDef(
            id = "full_bloom",
            category = CATEGORY_GROWTH,
            name = "墨园丰收",
            description = "墨园繁盛，万物生长",
            condition = "解锁全部27株",
            targetValue = 27
        ),
        AchievementDef(
            id = "mohua",
            category = CATEGORY_GROWTH,
            name = "墨华绽放",
            description = "墨华绽放，锦绣天成",
            condition = "植物达LV5",
            targetValue = 1
        ),
        AchievementDef(
            id = "revival",
            category = CATEGORY_GROWTH,
            name = "枯木逢春",
            description = "枯木逢春犹再发",
            condition = "救活1株",
            targetValue = 1
        ),
        
        // 里程碑成就
        AchievementDef(
            id = "week_streak",
            category = CATEGORY_MILESTONE,
            name = "一周墨途",
            description = "七日持之以恒",
            condition = "连续7天",
            targetValue = 7
        ),
        AchievementDef(
            id = "month_streak",
            category = CATEGORY_MILESTONE,
            name = "一月墨耕",
            description = "一月勤耕，墨香盈门",
            condition = "连续30天",
            targetValue = 30
        ),
        AchievementDef(
            id = "hundred_streak",
            category = CATEGORY_MILESTONE,
            name = "百日墨缘",
            description = "百日之约，墨缘深厚",
            condition = "连续100天",
            targetValue = 100
        )
    )
    
    /**
     * 分类显示名称
     */
    val CATEGORY_LABELS = mapOf(
        CATEGORY_ALL to "全部",
        CATEGORY_READING to "阅读",
        CATEGORY_GROWTH to "养成",
        CATEGORY_MILESTONE to "里程碑"
    )
}
