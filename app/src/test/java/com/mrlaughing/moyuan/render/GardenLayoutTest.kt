package com.mrlaughing.moyuan.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenLayoutTest {
    @Test
    fun `supported square layouts contain every lawn`() {
        for (side in 3..7) {
            val cells = GardenLayout.calculate(side, side, 1080, 2200)
            assertEquals(side * side, cells.size)
            assertEquals((0 until side * side).toSet(), cells.map { it.fillRank }.toSet())
        }
    }

    @Test
    fun `three by three begins at visual center`() {
        val cells = GardenLayout.calculate(3, 3, 1080, 2200)
        val first = cells.single { it.fillRank == 0 }

        assertEquals(1, first.row)
        assertEquals(1, first.column)
        assertEquals(540f, first.centerX, 0.01f)
    }

    @Test
    fun `front cells are lower than back cells`() {
        val cells = GardenLayout.calculate(7, 7, 1080, 2200)
        val back = cells.single { it.row == 0 && it.column == 0 }
        val front = cells.single { it.row == 6 && it.column == 6 }

        assertTrue(front.centerY > back.centerY)
        assertTrue(front.depth > back.depth)
    }

    @Test
    fun `even four by four starts from central two by two`() {
        val cells = GardenLayout.calculate(4, 4, 1080, 2200)
        val first = cells.single { it.fillRank == 0 }
        // 规范第3节：偶数方阵从中央 2x2 起填，首格必落在中央两行两列内
        assertTrue(first.row in 1..2)
        assertTrue(first.column in 1..2)
    }

    @Test
    fun `depth grows monotonically from back to front`() {
        val cells = GardenLayout.calculate(5, 5, 1080, 2200)
        val byDepth = cells.sortedBy { it.depth }
        for (i in 0 until byDepth.size - 1) {
            assertTrue(byDepth[i].depth <= byDepth[i + 1].depth)
            assertTrue(byDepth[i].centerY <= byDepth[i + 1].centerY + 1f)
        }
    }
}
