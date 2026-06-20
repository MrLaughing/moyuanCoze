package com.mrlaughing.moyuan.data.model

data class Plant(
    val id: String,
    val name: String,
    val path: PlantPath,
    val rarity: PlantRarity,
    val unlockThreshold: Int,
    val description: String,
    val lore: String
)
