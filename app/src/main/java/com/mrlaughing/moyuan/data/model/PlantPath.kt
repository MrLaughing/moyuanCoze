package com.mrlaughing.moyuan.data.model

enum class PlantPath(val label: String) {
    JIMO("积墨"),      // 累计阅读时长
    BINGZHU("秉烛"),   // 夜间阅读天数
    SUIHAN("岁寒"),    // 连续阅读天数
    XUNFANG("寻芳"),   // 已读不同书目数
    HIDDEN("隐藏")     // 特殊条件
}
