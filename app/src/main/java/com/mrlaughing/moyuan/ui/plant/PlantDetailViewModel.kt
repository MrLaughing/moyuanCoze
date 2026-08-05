package com.mrlaughing.moyuan.ui.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.data.remote.dto.ApiPreferCategory
import com.mrlaughing.moyuan.data.remote.dto.ShelfBook
import com.mrlaughing.moyuan.data.repository.PlantRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.repository.GardenPlacementResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId

/**
 * 植物详情 ViewModel
 *
 * v2.0：展示植物名、大图、描述、诗文引用（lore）、解锁条件。
 * 阅读时光：结合「上一株解锁」锚点，从微信读书拉取时间窗内的
 * 阅读时长 / 书目 / 偏爱类型 / 划线数 / 真实书摘，形成「用阅读养花园」的轻养成档案。
 */
@HiltViewModel
@OptIn(kotlinx.coroutines.FlowPreview::class)
class PlantDetailViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val wereadRepository: WereadRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun loadPlant(plantStringId: String) {
        viewModelScope.launch {
            try {
                val plantDef = PlantDefinitions.getById(plantStringId)
                if (plantDef == null) {
                    Log.e("PlantDetailVM", "未找到植物定义: $plantStringId")
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = "未知植物"
                    )
                    return@launch
                }

                Log.d("PlantDetailVM", "加载植物: $plantStringId (${plantDef.name})")

                // 上一株解锁锚点（只与 plantId 相关，collect 前算一次即可）
                val prevDate = runCatching { plantRepository.getPreviousUnlockDate(plantStringId) }
                    .getOrNull()

                plantRepository.observePlant(plantStringId)
                    .distinctUntilChanged()
                    .debounce(150)
                    .collect { entity ->  // entity: PlantStateEntity?
                        try {
                            val isUnlocked = entity != null && !entity.unlockDate.isNullOrEmpty()

                            // 发现印记
                            val discoveryDate = entity?.unlockDate
                            val discoveryLine = if (discoveryDate != null) {
                                "你于 $discoveryDate 发现这株「${plantDef.name}」"
                            } else {
                                "这株植物尚未与你相遇"
                            }
                            val appDaysLine = buildAppDaysLine()

                            // 先给基础状态（不依赖微信读书网络），保证画面即时可见
                            _uiState.value = PlantDetailUiState(
                                plantIdStr = plantStringId,
                                name = plantDef.name,
                                description = plantDef.description,
                                lore = plantDef.lore,
                                isUnlocked = isUnlocked,
                                unlockThreshold = plantDef.unlockThreshold,
                                isInGarden = entity?.isInGarden ?: false,
                                unlockDate = entity?.unlockDate,
                                discoveryLine = discoveryLine,
                                appDaysLine = appDaysLine,
                                readNoteLine = "每一次翻开书页，都为这座花园添了一缕墨香"
                            )

                            // 解锁后才去拉阅读时光（可能稍慢，异步回填）
                            if (isUnlocked) {
                                viewModelScope.launch {
                                    try {
                                        val wr = buildWindowedReadingContext(prevDate)
                                        _uiState.value = _uiState.value.copy(
                                            wereadLoaded = true,
                                            wereadAuthorized = wr.authorized,
                                            readingWindowLabel = wr.windowLabel,
                                            readingDurationText = wr.durationText,
                                            readingHighlightText = wr.highlightText,
                                            readingCategories = wr.categories,
                                            readingBookTitles = wr.bookTitles,
                                            readingExcerpts = wr.excerpts,
                                            readNoteLine = wr.readNoteLine
                                        )
                                    } catch (e: Exception) {
                                        Log.e("PlantDetailVM", "阅读时光加载失败", e)
                                        _uiState.value = _uiState.value.copy(
                                            wereadLoaded = true,
                                            wereadAuthorized = false
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PlantDetailVM", "处理植物状态失败", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e("PlantDetailVM", "加载植物详情失败 plantStringId=$plantStringId", e)
                val plantDef = PlantDefinitions.getById(plantStringId)
                if (plantDef != null) {
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = plantDef.name,
                        description = plantDef.description,
                        lore = plantDef.lore,
                        isUnlocked = false,
                        unlockThreshold = plantDef.unlockThreshold
                    )
                } else {
                    _uiState.value = PlantDetailUiState(
                        plantIdStr = plantStringId,
                        name = "未知植物"
                    )
                }
            }
        }
    }

    /**
     * 切换植物是否放入花园（自定义摆放）
     */
    fun toggleGardenStatus() {
        val plantId = _uiState.value.plantIdStr
        if (plantId.isBlank()) return
        viewModelScope.launch {
            val newStatus = !(_uiState.value.isInGarden)
            try {
                when (plantRepository.updateGardenStatus(plantId, newStatus)) {
                    GardenPlacementResult.ADDED -> _messages.emit("已放入花园")
                    GardenPlacementResult.REMOVED -> _messages.emit("已移出花园")
                    GardenPlacementResult.FULL -> _messages.emit("满园最多陈列 49 株，请先移出一株植物")
                    GardenPlacementResult.LOCKED -> _messages.emit("这株植物尚未被发现")
                    GardenPlacementResult.NOT_FOUND -> _messages.emit("未找到植物状态")
                }
            } catch (e: Exception) {
                Log.e("PlantDetailVM", "切换花园状态失败: $plantId", e)
            }
        }
    }

    /**
     * 阅读时光 · 上一株解锁之后的窗口化阅读档案。
     *
     * 口径（混合方案）：
     * - 阅读时长：readTimes 按天粒度，可精确累加锚点日(含)之后的秒数。
     * - 读了哪些书：书架 readUpdateTime >= 锚点 的书。
     * - 划线数：窗口内书目在笔记本里的 bookmarkCount 之和。
     * - 偏爱类型：窗口内书的 category 汇总（最多查前 12 本），并保留该类下的书目列表供展开。
     * - 真实书摘：窗口内前若干本书的 bookmarklist，按 createTime 切窗取真实划线文本。
     */
    private suspend fun buildWindowedReadingContext(prevDate: String?): WindowedReading {
        val authorized = runCatching { wereadRepository.isAuthorized() }.getOrDefault(false)
        if (!authorized) {
            return WindowedReading(
                authorized = false,
                readNoteLine = "墨园静候，待你在书页间拾得第一枚落款"
            )
        }

        val anchorSec = prevDate?.let { parseDateToStartOfDayEpochSec(it) }

        val readResp = runCatching { wereadRepository.fetchReadDataOverall().getOrNull() }.getOrNull()
        val windowSec = readResp?.readTimes
            ?.filterKeys { anchorSec == null || (it.toLongOrNull() ?: 0L) >= anchorSec }
            ?.values
            ?.sum() ?: 0L

        val shelf = runCatching { wereadRepository.fetchShelf().getOrNull() }.getOrNull()
        val windowedBooks = shelf?.books
            ?.filter { anchorSec == null || it.readUpdateTime >= anchorSec }
            ?.sortedByDescending { it.readUpdateTime }
            ?.take(MAX_BOOKS_SHOWN)
            ?: emptyList()
        val bookTitles = windowedBooks.mapNotNull { it.title.takeIf { t -> t.isNotBlank() } }

        val notebooks = runCatching { wereadRepository.fetchNotebooks().getOrNull() }.getOrNull()
        val windowedIds = windowedBooks.map { it.bookId }.toSet()
        val highlightCount = notebooks?.books
            ?.filter { it.bookId in windowedIds }
            ?.sumOf { it.bookmarkCount } ?: 0

        val categories = deriveCategories(windowedBooks, readResp?.preferCategory ?: emptyList())
        val excerpts = buildExcerpts(windowedBooks, anchorSec)

        val durationText = formatDuration(windowSec)
        val highlightText = "${highlightCount} 条划线"
        val windowLabel = prevDate?.let { "自 ${formatMonthDay(it)} 种下上一株以来" }
            ?: "自你开启墨园阅读以来"
        val readNoteLine = buildReadNoteLine(readResp?.readDays ?: 0, windowSec)

        return WindowedReading(
            authorized = true,
            windowLabel = windowLabel,
            durationText = durationText,
            highlightText = highlightText,
            categories = categories,
            bookTitles = bookTitles,
            excerpts = excerpts,
            readNoteLine = readNoteLine
        )
    }

    /**
     * 偏爱类型：优先用窗口内书的 category 汇总，并保留该类下的书目（供点击展开）；
     * 窗口内无书时退化为全量 preferCategory（无书目列表）。
     */
    private suspend fun deriveCategories(
        books: List<ShelfBook>,
        fallback: List<ApiPreferCategory>
    ): List<CategoryDetail> {
        val fromBooks = if (books.isNotEmpty()) {
            val counts = mutableMapOf<String, Int>()
            val catBooks = mutableMapOf<String, MutableList<String>>()
            books.take(MAX_CATEGORY_LOOKUP).forEach { b ->
                val cat = runCatching {
                    wereadRepository.fetchBookInfo(b.bookId).getOrNull()?.category
                }.getOrNull()
                if (!cat.isNullOrBlank()) {
                    counts[cat] = counts.getOrDefault(cat, 0) + 1
                    b.title.takeIf { it.isNotBlank() }
                        ?.let { catBooks.getOrPut(cat) { mutableListOf() }.add(it) }
                }
            }
            counts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { CategoryDetail(it.key, it.value, catBooks[it.key] ?: emptyList()) }
        } else {
            emptyList()
        }
        if (fromBooks.isNotEmpty()) return fromBooks
        return fallback.take(3).map { CategoryDetail(it.categoryTitle, it.readingCount) }
    }

    /**
     * 真实书摘：窗口内前若干本书的 bookmarklist，按 createTime 切窗取真实划线文本。
     */
    private suspend fun buildExcerpts(
        books: List<ShelfBook>,
        anchorSec: Long?
    ): List<ExcerptItem> {
        val result = mutableListOf<ExcerptItem>()
        books.take(MAX_EXCERPT_BOOKS).forEach { b ->
            val bm = runCatching { wereadRepository.fetchBookmarks(b.bookId).getOrNull() }.getOrNull()
            bm?.updated
                ?.filter { anchorSec == null || it.createTime >= anchorSec }
                ?.take(MAX_EXCERPTS_PER_BOOK)
                ?.forEach { item ->
                    val text = item.markText.trim()
                    if (text.isNotBlank()) result.add(ExcerptItem(text, b.title))
                }
        }
        return result.take(MAX_EXCERPTS_TOTAL)
    }

    /**
     * 书摘拾遗：由阅读体量生成的轻养成文案，区别于「文化小传」
     */
    private fun buildReadNoteLine(readDays: Int, windowSec: Long): String {
        val hours = (windowSec / 3600)
        return when {
            readDays <= 0 -> "墨园静候，待你在书页间拾得第一枚落款"
            hours >= 100 -> "百小时的书香，已在这座花园里长成看不见的根须"
            hours >= 24 -> "廿四小时的阅读，足够让一株草木记住你的温度"
            readDays >= 30 -> "三十个读书的夜，是这座花园最绵长的春雨"
            else -> "每一次翻开书页，都为这座花园添了一缕墨香"
        }
    }

    private fun formatDuration(sec: Long): String {
        if (sec <= 0) return "暂未记录"
        val minutes = (sec / 60).toInt()
        val hours = minutes / 60
        val remMin = minutes % 60
        return if (hours > 0) "${hours} 小时 ${remMin} 分" else "${remMin} 分"
    }

    private fun formatMonthDay(dateStr: String): String {
        return try {
            val d = LocalDate.parse(dateStr)
            "${d.monthValue} 月 ${d.dayOfMonth} 日"
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseDateToStartOfDayEpochSec(dateStr: String): Long? {
        return try {
            LocalDate.parse(dateStr)
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 阅读时光印记 · 应用陪伴天数
     */
    private suspend fun buildAppDaysLine(): String {
        val installTime = try {
            userPrefs.firstLaunchTime.first()
        } catch (e: Exception) {
            0L
        }
        val days = if (installTime > 0) {
            ((System.currentTimeMillis() - installTime) / 86_400_000L).coerceAtLeast(0)
        } else {
            0L
        }
        return "墨园已陪你走过 $days 天"
    }

    companion object {
        /** 阅读时光卡中最多展示的书目数 */
        private const val MAX_BOOKS_SHOWN = 8

        /** 为汇总偏爱类型，最多回溯的书详情数（控制额外请求量） */
        private const val MAX_CATEGORY_LOOKUP = 12

        /** 为取真实书摘，最多回溯的书数（控制 bookmarklist 请求量） */
        private const val MAX_EXCERPT_BOOKS = 4

        /** 每本书最多取的真实划线条数 */
        private const val MAX_EXCERPTS_PER_BOOK = 3

        /** 书页拾光最多展示的真实划线条数 */
        private const val MAX_EXCERPTS_TOTAL = 6
    }
}

/** 偏爱类型统计（窗口内书的分类汇总，附带该类下的书目供展开） */
data class CategoryDetail(
    val name: String,
    val count: Int,
    val books: List<String> = emptyList()
)

/** 真实书摘（划线文本 + 来源书名） */
data class ExcerptItem(
    val text: String,
    val source: String
)

/** 阅读时光窗口化聚合结果（ViewModel 内部使用） */
private data class WindowedReading(
    val authorized: Boolean = false,
    val windowLabel: String = "",
    val durationText: String = "",
    val highlightText: String = "",
    val categories: List<CategoryDetail> = emptyList(),
    val bookTitles: List<String> = emptyList(),
    val excerpts: List<ExcerptItem> = emptyList(),
    val readNoteLine: String = ""
)

/**
 * 植物详情 UI 状态
 * v2.0：精简，移除等级、稀有度、枯萎等废弃字段；新增阅读时光窗口化字段。
 */
data class PlantDetailUiState(
    val plantIdStr: String = "",
    val name: String = "",
    val description: String = "",
    val lore: String = "",
    val isUnlocked: Boolean = false,
    val unlockThreshold: Int = 0,
    val isInGarden: Boolean = false,
    val unlockDate: String? = null,
    val discoveryLine: String = "",
    val appDaysLine: String = "",
    val readNoteLine: String = "",
    // —— 阅读时光（上一株解锁之后） ——
    val wereadLoaded: Boolean = false,
    val wereadAuthorized: Boolean = false,
    val readingWindowLabel: String = "",
    val readingDurationText: String = "",
    val readingHighlightText: String = "",
    val readingCategories: List<CategoryDetail> = emptyList(),
    val readingBookTitles: List<String> = emptyList(),
    val readingExcerpts: List<ExcerptItem> = emptyList()
)
