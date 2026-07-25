package com.mrlaughing.moyuan.render

import com.mrlaughing.moyuan.data.model.HeightTier
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.model.PlantHeightTiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenArrangerTest {

    private data class FakePlant(val name: String, val height: Int)

    @Test
    fun `taller plants are assigned to smaller depth cells`() {
        val cells = GardenLayout.calculate(5, 5, 1080, 2200)
        val plants = listOf(
            FakePlant("低1", 0), FakePlant("高1", 2), FakePlant("中1", 1),
            FakePlant("高2", 2), FakePlant("低2", 0)
        )

        val result = GardenArranger.arrangeByHeight(plants, cells) { it.height }
        assertEquals(plants.size, result.size)

        val depthOf = result.associate { (p, c) -> p.name to c.depth }
        // 任一高植物的 depth 不大于任一低植物的 depth
        for (tall in listOf("高1", "高2")) {
            for (low in listOf("低1", "低2")) {
                assertTrue(
                    "$tall(depth=${depthOf[tall]}) 应不晚于 $low(depth=${depthOf[low]})",
                    depthOf.getValue(tall) <= depthOf.getValue(low)
                )
            }
        }
    }

    @Test
    fun `arrangement keeps center-first cell selection`() {
        val cells = GardenLayout.calculate(7, 7, 1080, 2200)
        val plants = (1..9).map { FakePlant("p$it", it % 3) }

        val result = GardenArranger.arrangeByHeight(plants, cells) { it.height }

        val expectedCells = cells.sortedBy { it.fillRank }.take(9)
            .map { it.row to it.column }.toSet()
        val usedCells = result.map { (_, c) -> c.row to c.column }.toSet()
        assertEquals("使用的格子必须仍是 fillRank 前 9 格", expectedCells, usedCells)
    }

    @Test
    fun `result preserves original plant order`() {
        val cells = GardenLayout.calculate(3, 3, 1080, 2200)
        val plants = listOf(
            FakePlant("a", 2), FakePlant("b", 0), FakePlant("c", 1)
        )
        val result = GardenArranger.arrangeByHeight(plants, cells) { it.height }
        assertEquals(listOf("a", "b", "c"), result.map { it.first.name })
    }

    @Test
    fun `plants beyond capacity are dropped`() {
        val cells = GardenLayout.calculate(3, 3, 1080, 2200)
        val plants = (1..12).map { FakePlant("p$it", 1) }
        val result = GardenArranger.arrangeByHeight(plants, cells) { it.height }
        assertEquals(9, result.size)
    }

    @Test
    fun `height tiers only reference existing plant ids`() {
        val knownIds = PlantDefinitions.all.map { it.id }.toSet()
        // 抽查代表性条目 + 默认层级
        assertEquals(HeightTier.TALL, PlantHeightTiers.tierFor("bamboo"))
        assertEquals(HeightTier.LOW, PlantHeightTiers.tierFor("lavender"))
        assertEquals(HeightTier.MEDIUM, PlantHeightTiers.tierFor("rose"))
        assertEquals(HeightTier.MEDIUM, PlantHeightTiers.tierFor("nonexistent_id"))
        // 所有定义的植物都能取到合法层级
        PlantDefinitions.all.forEach { plant ->
            assertTrue(plant.id in knownIds)
            PlantHeightTiers.tierFor(plant) // 不应抛异常
        }
    }
}
