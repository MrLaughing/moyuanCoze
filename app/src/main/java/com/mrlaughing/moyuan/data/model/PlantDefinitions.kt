package com.mrlaughing.moyuan.data.model

/**
 * 墨园 · 二十七种植物全图鉴
 *
 * 积墨六种 · 秉烛六种 · 岁寒六种 · 寻芳六种 · 隐藏三种
 */
object PlantDefinitions {

    val all: List<Plant> by lazy {
        jimoPlants + bingzhuPlants + suihanPlants + xunfangPlants + hiddenPlants
    }

    fun getById(id: String): Plant? = all.find { it.id == id }

    fun getByPath(path: PlantPath): List<Plant> = all.filter { it.path == path }

    // ─── 积墨（累计阅读时长）───────────────────────────────────
    private val jimoPlants = listOf(
        Plant(
            id = "changpu",
            name = "菖蒲",
            path = PlantPath.JIMO,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 0,
            description = "水畔常见之草，叶似剑而气清冽，古人悬于门楣以驱邪。墨芽初绽时，其香最淡，若有似无。",
            lore = "屈子行吟泽畔，手揽菖蒲而叹：「举世皆浊，我独清。」后以菖蒲为君子之草，以其清气化墨，点染初心。"
        ),
        Plant(
            id = "wenzhu",
            name = "文竹",
            path = PlantPath.JIMO,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 120,
            description = "枝叶如云似雾，纤细而不折，案头清供之佳品。虽名竹而非竹，却得竹之文雅。",
            lore = "旧时书斋，案上必置一盆文竹。文竹伴墨香而生，阅尽千卷而不改其青。人云：文竹不语，却知书中三昧。"
        ),
        Plant(
            id = "orchid",
            name = "兰草",
            path = PlantPath.JIMO,
            rarity = PlantRarity.RARE,
            unlockThreshold = 480,
            description = "空谷幽兰，不求人赏而自芳。叶如碧带，花似素蝶，清而不寒，幽而不暗。",
            lore = "孔子见幽谷兰草，叹曰：「兰为王者香，今独茂与众草为伍。」然兰不以无人而不芳，恰如墨客不求闻达而自修。"
        ),
        Plant(
            id = "bajiao",
            name = "芭蕉",
            path = PlantPath.JIMO,
            rarity = PlantRarity.RARE,
            unlockThreshold = 1200,
            description = "叶大如盖，雨打芭蕉声声入诗。绿云舒展之间，藏纳风雅无数，听雨最宜。",
            lore = "蒋捷听雨半生，少年时红烛昏罗帐，壮年时江阔云低，暮年时僧庐下听雨打芭蕉。芭蕉叶上雨，便是人间半世书。"
        ),
        Plant(
            id = "shuixian",
            name = "水仙",
            path = PlantPath.JIMO,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 2400,
            description = "凌波仙子踏水来，金盏银台照月开。清水一盏即可生根，冰肌玉骨不染尘。",
            lore = "相传水仙为洛神之遗佩所化。洛水之畔，神女遗落玉佩，落入清泉化为水仙，岁岁凌波而开，以慰世间痴墨人。"
        ),
        Plant(
            id = "mozhu",
            name = "墨竹",
            path = PlantPath.JIMO,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 4800,
            description = "竹中之竹，墨中之墨。以墨为色，以竹为骨，风过不折，雪压不弯，乃积墨之极。",
            lore = "文与可画竹，胸有成竹。苏子瞻观之曰：「与其师人，不如师竹。」墨竹者，非竹也，乃竹之魂以墨凝之。阅尽万卷方成此竹，每一节皆是书山之路。"
        )
    )

    // ─── 秉烛（夜间阅读天数）───────────────────────────────────
    private val bingzhuPlants = listOf(
        Plant(
            id = "yelaixiang",
            name = "夜来香",
            path = PlantPath.BINGZHU,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 3,
            description = "日暮方醒，夜深愈香。寻常花木日出而作，此花独于月下吐蕊，幽香暗送。",
            lore = "古有秉烛夜游之习，文人秉烛读书，夜来香应墨而生。月下展卷，花香伴墨香，此乃夜读者独有的清福。"
        ),
        Plant(
            id = "guihua",
            name = "桂花",
            path = PlantPath.BINGZHU,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 7,
            description = "金粟点点映月明，十里飘香入画屏。秋夜读书时，桂子随窗入，满室皆是秋意。",
            lore = "蟾宫折桂，古之读书人最高志向。月宫桂树常青不凋，恰如秉烛之人，夜夜不辍，终得金桂盈枝。"
        ),
        Plant(
            id = "yehehua",
            name = "夜荷花",
            path = PlantPath.BINGZHU,
            rarity = PlantRarity.RARE,
            unlockThreshold = 15,
            description = "昼伏夜开之莲，月下始展瓣，瓣瓣凝霜辉。花色淡紫似墨晕，暗香浮动。",
            lore = "佛经有云，优昙花三千年一开。夜荷非优昙，却同样珍稀——唯夜读者方知其美。人世间最柔的花，开在最深的夜。"
        ),
        Plant(
            id = "lamei",
            name = "蜡梅",
            path = PlantPath.BINGZHU,
            rarity = PlantRarity.RARE,
            unlockThreshold = 30,
            description = "蜡质花瓣凝如琥珀，寒夜中独燃一盏明黄。香幽而远，非雪夜不闻其真味。",
            lore = "王安石诗云「墙角数枝梅，凌寒独自开」。蜡梅非梅，却得梅之傲骨。秉烛人最知蜡梅之心——黑暗中守一盏灯，便如寒冬守一朵花。"
        ),
        Plant(
            id = "tanhua",
            name = "昙花",
            path = PlantPath.BINGZHU,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 60,
            description = "月下美人，一现即逝。花瓣洁白如雪，绽放时满室生辉，须臾间便归寂灭。",
            lore = "昙花一现，只为韦陀。传说昙花痴恋佛前护法韦陀，千年只为一面。此花最懂等候之苦——如夜读者等候黎明，虽一瞬亦值得。"
        ),
        Plant(
            id = "yuejiancao",
            name = "月见草",
            path = PlantPath.BINGZHU,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 100,
            description = "待月而开，月升则花放，月落则花收。瓣如薄纱染月华，蕊似星子落凡尘。",
            lore = "印第安古族视月见草为「夜之使节」，巫师以此通灵。东方则传月见草为嫦娥洒落的月光所化，唯长夜不眠者方能与之共鸣。秉烛百夜，方见月见花开。"
        )
    )

    // ─── 岁寒（连续阅读天数）───────────────────────────────────
    private val suihanPlants = listOf(
        Plant(
            id = "pine",
            name = "青松",
            path = PlantPath.SUIHAN,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 3,
            description = "四时常青，岁寒不凋。针叶如墨，树皮如铁，风霜愈烈其色愈浓。",
            lore = "「岁寒，然后知松柏之后凋也。」夫子之言，千载犹新。青松不择地而生，不因寒而败，正如日日不辍之人，恒心可见。"
        ),
        Plant(
            id = "cypress",
            name = "翠柏",
            path = PlantPath.SUIHAN,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 7,
            description = "苍翠森然，四季不改其色。柏香清苦如墨，最宜伴读，庙堂之木化为书房之伴。",
            lore = "古柏千年犹翠，人谓其得天地之正气。文天祥狱中作《正气歌》，以柏自喻。翠柏者，连日不辍之功也。"
        ),
        Plant(
            id = "plum",
            name = "寒梅",
            path = PlantPath.SUIHAN,
            rarity = PlantRarity.RARE,
            unlockThreshold = 15,
            description = "百花先觉，独占东风第一枝。疏影横斜，暗香浮动，雪中更显清绝。",
            lore = "林和靖梅妻鹤子，孤山二十年，梅即其妻。寒梅不争春，却先知春意——如日日坚持之人，看似独行，实则先驱。"
        ),
        Plant(
            id = "bamboo",
            name = "修竹",
            path = PlantPath.SUIHAN,
            rarity = PlantRarity.RARE,
            unlockThreshold = 30,
            description = "中空而直，有节不折。风来则鸣，雪压则弯而不断，竹之声即风雅之声。",
            lore = "苏子瞻云「宁可食无肉，不可居无竹」。郑板桥画竹一生，云「咬定青山不放松」。修竹之节，即读书人之节——日日不辍，节节高升。"
        ),
        Plant(
            id = "ginkgo",
            name = "银杏",
            path = PlantPath.SUIHAN,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 60,
            description = "活化石也，亿年不改其形。秋来金叶满地如铺墨，一树金黄便是千年岁月之书。",
            lore = "佛门以银杏为菩提之替，取其长青不凋之意。银杏历经冰川而不灭，其韧如铁——六十日连读不辍之人，方有银杏之志。"
        ),
        Plant(
            id = "guteng",
            name = "古藤",
            path = PlantPath.SUIHAN,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 100,
            description = "盘根错节，百年纠缠。春发新枝而老干犹劲，枯荣同株，生死共存。",
            lore = "李白诗云「枯藤老树昏鸦」，然古藤非枯，乃百岁之韧。藤虽柔而能绕石穿崖，百日后仍日日不辍者，便如古藤——看似柔弱，实则无人可断其志。"
        )
    )

    // ─── 寻芳（已读不同书目数）─────────────────────────────────
    private val xunfangPlants = listOf(
        Plant(
            id = "chrysanthemum",
            name = "墨菊",
            path = PlantPath.XUNFANG,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 3,
            description = "菊中异品，花瓣如墨染，深紫近黑。秋深始绽，不与群芳争春色。",
            lore = "陶渊明采菊东篱下，悠然见南山。墨菊尤为隐士之菊——不取金黄之耀，独守墨色之沉。读三书方识墨菊之美，不在花色，在花骨。"
        ),
        Plant(
            id = "ziteng",
            name = "紫藤",
            path = PlantPath.XUNFANG,
            rarity = PlantRarity.COMMON,
            unlockThreshold = 7,
            description = "紫云垂瀑，花穗如瀑似帘。春风中摇曳成诗，一架紫藤便是半部花间集。",
            lore = "李白诗云「紫藤挂云木，花蔓宜阳春」。紫藤攀援而上，如读书人拾级而行。七卷在胸，紫藤方成一架花瀑。"
        ),
        Plant(
            id = "lotus",
            name = "青莲",
            path = PlantPath.XUNFANG,
            rarity = PlantRarity.RARE,
            unlockThreshold = 15,
            description = "出淤泥而不染，濯清涟而不妖。花大如碗，瓣如凝脂，清香远逸。",
            lore = "周敦颐独爱莲，谓其花之君子。青莲非青色之莲，乃清净之莲。十五卷不同之书入目，方见莲叶田田——博闻方能不染。"
        ),
        Plant(
            id = "haitang",
            name = "海棠",
            path = PlantPath.XUNFANG,
            rarity = PlantRarity.RARE,
            unlockThreshold = 30,
            description = "花姿娇而不媚，色如胭脂点墨。苏子称为花中神仙，一树海棠便是半卷丹青。",
            lore = "东坡谪黄州，唯海棠一树相伴，夜半燃烛照花，作「只恐夜深花睡去」。三十卷阅尽世间百态，方知海棠之姿——非娇，乃阅尽后的从容。"
        ),
        Plant(
            id = "peony",
            name = "墨牡丹",
            path = PlantPath.XUNFANG,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 50,
            description = "花中之王，墨色为衣。花瓣层叠如墨晕渲染，华贵中见清雅，雍容中含傲骨。",
            lore = "洛阳牡丹甲天下，墨牡丹却不在洛阳。传为武则天怒贬百花时，唯墨牡丹不朝，遂化为墨色。五十卷书入胸中，方有墨牡丹之大气——博览而能自持，方为王。"
        ),
        Plant(
            id = "bodhi",
            name = "菩提",
            path = PlantPath.XUNFANG,
            rarity = PlantRarity.LEGENDARY,
            unlockThreshold = 100,
            description = "心形之叶，叶脉如经文细密。风过叶鸣，似梵音低吟，树下悟道，一叶可知秋。",
            lore = "佛祖于菩提树下悟道，叹曰「一切众生皆具如来智慧德相」。百卷藏书之人，叶叶皆通——菩提本无树，明镜亦非台，唯百卷之功，方照见本来面目。"
        )
    )

    // ─── 隐藏（特殊条件）───────────────────────────────────────
    private val hiddenPlants = listOf(
        Plant(
            id = "lingzhi",
            name = "灵芝",
            path = PlantPath.HIDDEN,
            rarity = PlantRarity.HIDDEN,
            unlockThreshold = 0,
            description = "仙草之首，祥瑞之兆。形如云盖，色如丹霞，千年结实，万年成精。",
            lore = "四径通达，方见灵芝。积墨、秉烛、岁寒、寻芳——每一条路上都走过三重境，方能遇见这株灵芝。它不在任何一条路的尽头，而在四路交汇的灵山之上。"
        ),
        Plant(
            id = "bianhua",
            name = "彼岸花",
            path = PlantPath.HIDDEN,
            rarity = PlantRarity.HIDDEN,
            unlockThreshold = 0,
            description = "花叶两不相见，生生相错。赤红如血似墨染，开于幽冥之畔，却照见来路。",
            lore = "传说彼岸花开于忘川之畔，花香能唤起前生记忆。枯寂之中忽见花开——那不是死亡，而是重生。当枯寂的植物重新苏醒，彼岸花便悄然绽放，告诉你：没有白走的路，也没有白枯的花。"
        ),
        Plant(
            id = "bingtilian",
            name = "并蒂莲",
            path = PlantPath.HIDDEN,
            rarity = PlantRarity.HIDDEN,
            unlockThreshold = 0,
            description = "一茎双花，同根并蒂。两朵莲花并肩而开，似知己相对，如故人重逢。",
            lore = "世间并蒂莲千万中无一。五株鲜活墨韵之花同在一园，并蒂莲便不请自来——此非人力所求，乃心诚则灵。一个人走过五条路、养出五朵满花，这园中便有了并蒂之缘。"
        )
    )
}
