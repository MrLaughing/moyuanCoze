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
}
