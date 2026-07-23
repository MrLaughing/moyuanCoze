package com.mrlaughing.moyuan.render

import kotlin.math.abs

data class GardenCell(
    val row: Int,
    val column: Int,
    val centerX: Float,
    val centerY: Float,
    val tileSize: Float,
    val fillRank: Int,
    val depth: Int
)

/** Pure isometric layout shared by rendering, hit testing, and unit tests. */
object GardenLayout {
    private const val HORIZONTAL_SPREAD = 0.47f
    private const val VERTICAL_SPREAD = 0.23f
    private const val WIDTH_USAGE = 0.90f
    private const val HEIGHT_USAGE = 0.58f
    private const val VERTICAL_CENTER = 0.46f

    fun calculate(columns: Int, rows: Int, width: Int, height: Int): List<GardenCell> {
        if (columns <= 0 || rows <= 0 || width <= 0 || height <= 0) return emptyList()

        val horizontalSpan = 1f + (columns + rows - 2) * HORIZONTAL_SPREAD
        val verticalSpan = 1f + (columns + rows - 2) * VERTICAL_SPREAD
        val tileSize = minOf(
            width * WIDTH_USAGE / horizontalSpan,
            height * HEIGHT_USAGE / verticalSpan
        )
        val centerX = width * 0.5f
        val centerY = height * VERTICAL_CENTER
        val rowCenter = (rows - 1) / 2f
        val columnCenter = (columns - 1) / 2f

        val rankedCoordinates = buildList {
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val rowDelta = row - rowCenter
                    val columnDelta = column - columnCenter
                    add(
                        RankedCoordinate(
                            row = row,
                            column = column,
                            radiusSquared = rowDelta * rowDelta + columnDelta * columnDelta,
                            verticalDistance = abs(rowDelta),
                            columnDelta = columnDelta,
                            rowDelta = rowDelta
                        )
                    )
                }
            }
        }.sortedWith(
            compareBy<RankedCoordinate> { it.radiusSquared }
                .thenBy { it.verticalDistance }
                .thenBy { it.columnDelta }
                .thenBy { it.rowDelta }
        )
        val ranks = rankedCoordinates.mapIndexed { rank, coordinate ->
            (coordinate.row to coordinate.column) to rank
        }.toMap()
        val depthCenter = (columns + rows - 2) / 2f

        return buildList(columns * rows) {
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    add(
                        GardenCell(
                            row = row,
                            column = column,
                            centerX = centerX + (column - row) * tileSize * HORIZONTAL_SPREAD,
                            centerY = centerY + (column + row - depthCenter) * tileSize * VERTICAL_SPREAD,
                            tileSize = tileSize,
                            fillRank = ranks.getValue(row to column),
                            depth = row + column
                        )
                    )
                }
            }
        }
    }

    private data class RankedCoordinate(
        val row: Int,
        val column: Int,
        val radiusSquared: Float,
        val verticalDistance: Float,
        val columnDelta: Float,
        val rowDelta: Float
    )
}
