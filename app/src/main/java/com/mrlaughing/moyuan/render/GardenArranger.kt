package com.mrlaughing.moyuan.render

/**
 * 自动排列器（纯函数，可单测）。
 *
 * 规范（moyuan-product-design.md 第 3 节）：
 * 自动排列在「从中心向外环形填充」的基础上，尽量做到
 * **高大植物靠后排、低矮植物靠前排**，避免前排遮挡后排。
 *
 * 实现：
 * 1. 候选格 = 按 fillRank 取前 N 格（保持中心聚拢，不破坏环形观感）；
 * 2. 候选格内部按 depth 升序（depth 小 = 视觉靠后）排序；
 * 3. 植物按身高等级降序（稳定排序，等高保持原有顺序）排序；
 * 4. 两列表按序配对：最高的植物 → 最靠后的格子。
 */
object GardenArranger {

    /**
     * 将植物按身高配对到格子。
     *
     * @param plants     要摆放的植物（顺序即默认展示顺序）
     * @param cells      完整方阵格子（未筛选）
     * @param heightRank 植物身高等级取值函数，数值越大越高
     * @return 配对结果，顺序与 [plants] 一致；格子不足时多余植物被丢弃
     */
    fun <T> arrangeByHeight(
        plants: List<T>,
        cells: List<GardenCell>,
        heightRank: (T) -> Int
    ): List<Pair<T, GardenCell>> {
        if (plants.isEmpty() || cells.isEmpty()) return emptyList()

        val selected = cells
            .sortedBy { it.fillRank }
            .take(plants.size.coerceAtMost(cells.size))

        // 视觉靠后（depth 小）优先；同 depth 保持 fillRank 稳定
        val backToFront = selected.sortedWith(
            compareBy({ it.depth }, { it.fillRank })
        )

        // 高个优先；稳定排序保持等高植物的原顺序
        val tallFirst = plants.sortedByDescending { heightRank(it) }

        val cellByPlant = tallFirst.zip(backToFront).toMap()

        // 按原始植物顺序返回，方便调用方 mapIndexed
        return plants.mapNotNull { p -> cellByPlant[p]?.let { p to it } }
    }
}
