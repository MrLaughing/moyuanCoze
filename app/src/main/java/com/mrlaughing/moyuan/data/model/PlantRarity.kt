package com.mrlaughing.moyuan.data.model

enum class PlantRarity(val label: String, val stars: Int) {
    COMMON("常见", 1),
    RARE("稀有", 2),
    LEGENDARY("传说", 3),
    HIDDEN("隐藏", 4)
}
