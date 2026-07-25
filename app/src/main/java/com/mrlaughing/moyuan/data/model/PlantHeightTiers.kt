package com.mrlaughing.moyuan.data.model

/**
 * 植物视觉身高分层（用于花园自动排列「高大后排、低矮前排」）。
 *
 * 分层依据：植物 2.5D 立绘的视觉高度。
 * - TALL：乔木、竹、大型直立植物（立绘显著高于其他）
 * - MEDIUM：灌木、中等花茎（默认）
 * - LOW：地被、低矮草本、球根（立绘贴近地面）
 *
 * 仅按 id 标注需要特殊处理的植物，未标注的一律按 MEDIUM 处理，
 * 避免一次性改动 50 条植物定义带来的风险。
 */
enum class HeightTier(val rank: Int) {
    LOW(0),
    MEDIUM(1),
    TALL(2)
}

object PlantHeightTiers {

    private val TIER_BY_ID: Map<String, HeightTier> = mapOf(
        // ── 高（乔木 / 竹 / 大型直立）──
        "bajiao" to HeightTier.TALL,        // 芭蕉：宽大叶片舒展如扇
        "cypress" to HeightTier.TALL,       // 柏：苍劲挺拔
        "arborvitae" to HeightTier.TALL,    // 侧柏：树姿古朴端庄
        "boxwood" to HeightTier.TALL,       // 黄杨：耐修剪，盆景良材
        "plum" to HeightTier.TALL,          // 梅：疏影横斜
        "peony" to HeightTier.TALL,         // 牡丹：花大色艳
        "bodhi" to HeightTier.TALL,         // 菩提：心形叶片摇曳
        "chinesepeony" to HeightTier.TALL,  // 芍药：花大色艳
        "yucca" to HeightTier.TALL,         // 丝兰：剑形叶片挺立，花穗高耸
        "pine" to HeightTier.TALL,          // 松：挺拔苍翠
        "sunflower" to HeightTier.TALL,     // 向日葵：金黄圆盘
        "ginkgo" to HeightTier.TALL,        // 银杏：扇形叶片
        "hosta" to HeightTier.TALL,         // 玉簪：碧绿叶片
        "tulip" to HeightTier.TALL,         // 郁金香：杯形花冠挺拔
        "paniclehydrangea" to HeightTier.TALL, // 圆锥绣球：圆锥花序饱满
        "wisteria" to HeightTier.TALL,      // 紫藤：花序如瀑布垂下
        "bamboo" to HeightTier.TALL,        // 竹子：挺拔修长

        // ── 低（地被 / 低矮草本 / 球根）──
        "spiderlily" to HeightTier.LOW,     // 彼岸花：贴近地面
        "calamus" to HeightTier.LOW,        // 菖蒲：水边细叶如剑（低）
        "hydrangea_big" to HeightTier.LOW,  // 大花绣球：球形花序
        "orchid" to HeightTier.LOW,         // 兰花：素雅高洁（低）
        "switchgrass" to HeightTier.LOW,    // 柳枝稷：细长花序如烟
        "catmint" to HeightTier.LOW,        // 猫薄荷：灰绿低矮
        "morningglory" to HeightTier.LOW,   // 牵牛花：清晨绽放的紫色小喇叭
        "pansy" to HeightTier.LOW,          // 三色堇：花瓣如蝴蝶
        "sweetalyssum" to HeightTier.LOW,   // 香雪球：白色小花簇拥
        "littlebluestem" to HeightTier.LOW, // 小须芒草：草丛
        "lavender" to HeightTier.LOW,       // 薰衣草：花穗
        "tuberose" to HeightTier.LOW,       // 夜来香：低矮
        "eveningprimrose" to HeightTier.LOW,// 月见草：低矮花盘
        "echinacea" to HeightTier.LOW       // 紫锥花：花瓣如松果挺立（低）
    )

    /** 取得植物身高分层，未标注默认 MEDIUM */
    fun tierFor(plantId: String): HeightTier = TIER_BY_ID[plantId] ?: HeightTier.MEDIUM

    /** 取得植物身高分层（按 Plant 对象），未标注默认 MEDIUM */
    fun tierFor(plant: Plant): HeightTier = tierFor(plant.id)
}
