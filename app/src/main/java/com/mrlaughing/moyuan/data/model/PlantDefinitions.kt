package com.mrlaughing.moyuan.data.model

/**
 * 墨园 · 五十种2.5D植物图鉴
 *
 * 所有植物通过累计阅读时长解锁，使用 assets/plants/ 中的 PNG 图片渲染
 */
object PlantDefinitions {

    val all: List<Plant> = listOf(
        // ─── 初始植物（threshold=0，首次进入花园即解锁）────────────────
        Plant("bajiao", "芭蕉", PlantPath.JIMO, PlantRarity.COMMON, 0,
            "宽大的叶片舒展如扇，为园中增添热带风情。", "芭蕉叶大如席，古时常作信纸。窗前谁种芭蕉树，阴满中庭。"),
        Plant("cypress", "柏", PlantPath.JIMO, PlantRarity.COMMON, 0,
            "苍劲挺拔，四季常青，是庭院中的君子树。", "柏树苍劲，陵墓常植，象征着英魂不灭、文脉永续。"),
        Plant("spiderlily", "彼岸花", PlantPath.JIMO, PlantRarity.COMMON, 0,
            "红艳似火的花瓣卷曲如丝，花开不见叶，叶生不见花。", "花开彼岸，叶落黄泉，生生相错。又称曼珠沙华，是轮回之书中的点睛之花。"),

        // ─── 200~2000 分钟解锁 ────────────────────────────────────
        Plant("twinlotus", "并蒂莲", PlantPath.JIMO, PlantRarity.COMMON, 200,
            "一茎双花，相伴而生，寓意同心同德。", "一茎双花，同心同德。传说并蒂莲开处，必有一段传世佳话正在书写。"),
        Plant("arborvitae", "侧柏", PlantPath.JIMO, PlantRarity.COMMON, 500,
            "枝叶扁平如扇，树姿古朴端庄。", "侧柏枝条平展如扇，古时常作书简，其木纹细密如字字珠玑。"),
        Plant("calamus", "菖蒲", PlantPath.JIMO, PlantRarity.RARE, 800,
            "水边细叶如剑，清雅幽香，有文人风骨。", "菖蒲乃文房清供，与兰菊水仙并称花草四雅，最宜伴读。"),
        Plant("hydrangea_big", "大花绣球", PlantPath.JIMO, PlantRarity.COMMON, 1100,
            "球形花序饱满圆润，花色随土壤酸碱变幻。", "绣球花团锦簇，花色可随土壤酸碱而变，如同读书时的心情百转千回。"),
        Plant("africanlily", "非洲百合", PlantPath.JIMO, PlantRarity.RARE, 1400,
            "伞形花序如蓝色烟花，花茎挺拔优雅。", "非洲百合花茎挺拔如笔杆，蓝色花朵如墨水泼洒，是热带书桌上的精灵。"),
        Plant("osmanthus", "桂花", PlantPath.JIMO, PlantRarity.RARE, 1700,
            "金黄小花藏于叶间，秋日飘香数十里。", "桂花开时满城飘香，古时科举折桂，寓意读书人金榜题名。"),
        Plant("crabapple", "海棠", PlantPath.JIMO, PlantRarity.COMMON, 2000,
            "胭脂色的花朵缀满枝头，明媚动人。", "海棠花姿明媚，苏东坡诗云：只恐夜深花睡去，故烧高烛照红妆。"),

        // ─── 3200~9400 分钟解锁 ───────────────────────────────────
        Plant("pittosporum", "海桐", PlantPath.JIMO, PlantRarity.RARE, 3200,
            "圆润的绿叶四季不凋，白花清香怡人。", "海桐四季常青，白花如雪，是书斋窗外最安静的伴读之树。"),
        Plant("blackeyedsusan", "黑心金光菊", PlantPath.JIMO, PlantRarity.RARE, 3900,
            "金黄花瓣围簇深褐色花心，如灿烂小太阳。", "金黄花瓣围着深色花心，如一只只会发光的眼睛，注视着翻动的书页。"),
        Plant("boxwood", "黄杨", PlantPath.JIMO, PlantRarity.COMMON, 4600,
            "叶片细小光亮，耐修剪，是盆景良材。", "黄杨木质地细腻，是制作印章和书签的上等材料，每一片叶子都是一枚书签。"),
        Plant("goldenprivet", "金叶女贞", PlantPath.JIMO, PlantRarity.RARE, 5200,
            "金黄色的叶片在阳光下熠熠生辉。", "金黄色的叶片在阳光下如金箔般闪耀，像是书页边缘烫金的装饰。"),
        Plant("chrysanthemum", "菊", PlantPath.JIMO, PlantRarity.RARE, 5900,
            "秋日百花杀尽时，此花独自绽放。", "陶渊明采菊东篱下，悠然见南山。菊花是隐士的花，也是书卷气的象征。"),
        Plant("wintersweet", "腊梅", PlantPath.JIMO, PlantRarity.RARE, 6600,
            "寒冬腊月，金黄的花朵凌霜绽放，幽香袭人。", "腊梅傲雪而开，遥知不是雪，为有暗香来。寒冬夜里最宜伴读。"),
        Plant("orchid", "兰花", PlantPath.JIMO, PlantRarity.RARE, 7300,
            "素雅高洁，幽香清远，花中君子。", "兰为王者香，生于幽谷而芳华自赏。与善人居，如入芝兰之室。"),
        Plant("forsythia", "连翘", PlantPath.JIMO, PlantRarity.RARE, 8000,
            "早春满枝金黄，是最早报春的使者。", "连翘花开最早，满枝金黄如碎金，是春日书案上的第一抹亮色。"),
        Plant("lotus", "莲", PlantPath.JIMO, PlantRarity.RARE, 8700,
            "出淤泥而不染，濯清涟而不妖。", "出淤泥而不染，濯清涟而不妖。花之君子者也。"),
        Plant("reishi", "灵芝", PlantPath.JIMO, PlantRarity.RARE, 9400,
            "菌盖如祥云，被视为吉祥如意之物。", "灵芝乃祥瑞之物，形如如意，读书人案头摆灵芝，寓意笔下生花。"),

        // ─── 10100~16200 分钟解锁 ─────────────────────────────────
        Plant("switchgrass", "柳枝稷", PlantPath.JIMO, PlantRarity.RARE, 10100,
            "细长花序如烟似雾，随风摇曳生姿。", "柳枝稷的花序如烟如雾，在风中摇曳的姿态，像极了吟诵古诗时的摇头晃脑。"),
        Plant("catmint", "猫薄荷", PlantPath.JIMO, PlantRarity.RARE, 10800,
            "灰绿叶片的香气令猫咪为之疯狂。", "猫薄荷的香气让猫咪沉醉，正如好书让读者沉迷，都是无法抗拒的诱惑。"),
        Plant("rose", "玫瑰", PlantPath.JIMO, PlantRarity.RARE, 11400,
            "层层叠叠的花瓣包裹着浓郁芬芳。", "玫瑰花瓣层层叠叠，如同翻开一本厚重的小说，每一层都有新的惊喜。"),
        Plant("plum", "梅", PlantPath.JIMO, PlantRarity.RARE, 12100,
            "疏影横斜水清浅，暗香浮动月黄昏。", "墙角数枝梅，凌寒独自开。梅花是读书人坚韧精神的写照。"),
        Plant("peony", "牡丹", PlantPath.JIMO, PlantRarity.RARE, 12800,
            "花大色艳，雍容华贵，花中之王。", "牡丹雍容华贵，被称为花王。一株盛开的牡丹，像是读完一部恢弘的史诗。"),
        Plant("bodhi", "菩提", PlantPath.JIMO, PlantRarity.RARE, 13500,
            "心形叶片摇曳生姿，树下悟道之树。", "释迦牟尼在菩提树下悟道。菩提叶呈心形，每片叶子都像一颗觉悟的心。"),
        Plant("morningglory", "牵牛花", PlantPath.JIMO, PlantRarity.RARE, 14200,
            "清晨绽放的紫色小喇叭，迎着朝阳盛开。", "牵牛花朝开夕落，只在清晨绽放，如同记在晨读时的灵光一现。"),
        Plant("pansy", "三色堇", PlantPath.JIMO, PlantRarity.RARE, 14900,
            "花瓣如蝴蝶展翅，色彩斑斓多姿。", "三色堇花瓣如蝴蝶，色彩斑斓，就像书架上五彩缤纷的书脊。"),
        Plant("camellia", "山茶花", PlantPath.JIMO, PlantRarity.RARE, 15600,
            "端庄的花朵开在深冬，红白相映成趣。", "山茶花在寒冬绽放，花瓣圆满如书卷，层层展开，香气清幽。"),
        Plant("chinesepeony", "芍药", PlantPath.JIMO, PlantRarity.RARE, 16200,
            "花大色艳，绰约多姿，五月花神。", "芍药花开时如锦绣铺陈，恰似一部辞藻华丽的辞赋。"),

        // ─── 16900~30000 分钟解锁（终极植物）──────────────────────
        Plant("sage", "鼠尾草", PlantPath.JIMO, PlantRarity.LEGENDARY, 16900,
            "紫色的花序亭亭玉立，香气清幽。", "鼠尾草紫色的花序挺拔如笔，香气清冽，最适合写在回忆录里的味道。"),
        Plant("narcissus", "水仙", PlantPath.JIMO, PlantRarity.RARE, 17600,
            "凌波仙子，金盏银台，清香满室。", "水仙凌波而立，金盏银台，是寒冬腊月里文房中最清雅的一抹绿意。"),
        Plant("yucca", "丝兰", PlantPath.JIMO, PlantRarity.LEGENDARY, 18300,
            "剑形叶片挺立，白色花穗高耸入云。", "丝兰叶片如剑，花穗高耸，像是挺立在书页边缘的惊叹号。"),
        Plant("pine", "松", PlantPath.JIMO, PlantRarity.RARE, 19000,
            "岁寒知松柏之后凋，挺拔苍翠。", "岁寒，然后知松柏之后凋也。松树是坚韧不拔的读书精神。"),
        Plant("epiphyllum", "昙花", PlantPath.JIMO, PlantRarity.LEGENDARY, 19700,
            "月下美人，夜半绽放，惊艳一瞬。", "昙花一现，只为韦陀。深夜绽放的昙花，比任何夜读灯火都要惊艳。"),
        Plant("asparagusfern", "文竹", PlantPath.JIMO, PlantRarity.RARE, 20400,
            "纤细分枝如云片般轻柔，书桌清供。", "文竹纤细分枝如云，是书斋中最常见的盆景，增添一份文人雅趣。"),
        Plant("sweetalyssum", "香雪球", PlantPath.JIMO, PlantRarity.LEGENDARY, 21100,
            "白色小花簇拥如雪球，甜美芬芳。", "香雪球小花簇拥如雪球，在春风中轻轻摇曳，甜香如刚翻开的书页的气味。"),
        Plant("sunflower", "向日葵", PlantPath.JIMO, PlantRarity.RARE, 21800,
            "金黄圆盘永远追随太阳的方向。", "向日葵永远朝着太阳，正如读书人永远朝着光明和真理。"),
        Plant("littlebluestem", "小须芒草", PlantPath.JIMO, PlantRarity.LEGENDARY, 22400,
            "秋季转为红铜色，为花园增添野趣。", "小须芒草秋日转为红铜色，像是书页边缘泛黄的时光印记。"),
        Plant("lavender", "薰衣草", PlantPath.JIMO, PlantRarity.RARE, 23100,
            "紫色的花穗随风起伏，空气中弥漫清香。", "薰衣草的紫色花海与淡淡香气，最适合当作书签，夹在某个午后的回忆里。"),
        Plant("magnolia_coco", "夜合花", PlantPath.JIMO, PlantRarity.LEGENDARY, 23800,
            "夜晚闭合的花朵，白天舒展绽放。", "夜合花日开夜合，花朵闭合时像一本合上的书，清晨再次舒展翻开。"),
        Plant("tuberose", "夜来香", PlantPath.JIMO, PlantRarity.LEGENDARY, 24500,
            "夜色中芬芳最为浓郁，月下美人。", "夜来香在月色中绽放最浓的芬芳，是夜读时分最忠实的香气伴侣。"),
        Plant("ginkgo", "银杏", PlantPath.JIMO, PlantRarity.LEGENDARY, 25200,
            "扇形的叶片秋日金黄，活化石。", "银杏是活化石，扇形的叶片如一把把小小的书页，秋日金黄如古籍的色泽。"),
        Plant("hosta", "玉簪", PlantPath.JIMO, PlantRarity.LEGENDARY, 25900,
            "碧绿的叶片如碧玉簪，白花清雅芬芳。", "玉簪叶片如碧玉，白花如簪，是江南园林中最典雅的伴读花草。"),
        Plant("tulip", "郁金香", PlantPath.JIMO, PlantRarity.LEGENDARY, 26600,
            "杯形的花冠优雅挺拔，春日使者。", "郁金香原产西域，沿着丝绸之路传入中土，每一朵都是东西方文明的对话。"),
        Plant("paniclehydrangea", "圆锥绣球", PlantPath.JIMO, PlantRarity.LEGENDARY, 27200,
            "圆锥状花序饱满，秋日转为红褐色。", "圆锥绣球的花序像金字塔般层层叠加，秋日泛红，如读完整本书后的满足感。"),
        Plant("eveningprimrose", "月见草", PlantPath.JIMO, PlantRarity.LEGENDARY, 27900,
            "傍晚绽放的明黄花盘，为夜色添金。", "月见草在黄昏时绽放明黄色的花盘，为挑灯夜读的人点亮一盏小灯。"),
        Plant("bamboo", "竹子", PlantPath.JIMO, PlantRarity.LEGENDARY, 28600,
            "中空有节，挺拔修长，君子之姿。", "竹中空外直，有节有度。宁可食无肉，不可居无竹。"),
        Plant("wisteria", "紫藤", PlantPath.JIMO, PlantRarity.LEGENDARY, 29300,
            "藤蔓缠绕，紫色花序如瀑布般垂下。", "紫藤花如紫色瀑布垂下，若是坐在紫藤花架下读书，便是人间至乐。"),
        Plant("echinacea", "紫锥花", PlantPath.JIMO, PlantRarity.LEGENDARY, 30000,
            "紫色花瓣如松果般挺立，生机勃勃。", "紫锥花花瓣如松果般挺立，从花心向外舒展，充满蓬勃的生命力。")
    )

    fun getById(id: String): Plant? = all.find { it.id == id }

    fun getByLongIndex(index: Long): Plant? {
        val i = (index - 1).toInt()
        return all.getOrNull(i)
    }

    /** 英文ID → 中文文件名映射（用于 assets/plants/ 中的扁平PNG加载） */
    fun getAssetFileName(plantStringId: String): String {
        return idToAssetName[plantStringId] ?: plantStringId
    }

    private val idToAssetName: Map<String, String> by lazy {
        all.associate { it.id to it.name }
    }
}
