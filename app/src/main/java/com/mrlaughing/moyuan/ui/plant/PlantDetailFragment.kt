package com.mrlaughing.moyuan.ui.plant

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.model.PlantDefinitions
import com.mrlaughing.moyuan.render.PlantImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 植物详情 Fragment
 * v2.0：展示大图、文化小传（描述 + 诗文引用）、解锁条件
 */
@AndroidEntryPoint
class PlantDetailFragment : Fragment() {

    private val viewModel: PlantDetailViewModel by viewModels()
    private val args: PlantDetailFragmentArgs by navArgs()

    private lateinit var plantImage: ImageView
    private lateinit var textTitle: TextView
    private lateinit var plantDescription: TextView
    private lateinit var plantLore: TextView
    private lateinit var unlockCondition: TextView
    private lateinit var gardenToggleButton: TextView
    private lateinit var backButton: View
    private lateinit var discoveryText: TextView
    private lateinit var wereadText: TextView
    private lateinit var appDaysText: TextView
    private lateinit var readNoteText: TextView
    private lateinit var readingBody: LinearLayout
    private lateinit var durationValue: TextView
    private lateinit var highlightValue: TextView
    private lateinit var categoriesLayout: LinearLayout
    private lateinit var booksLayout: LinearLayout
    private lateinit var excerptsLayout: LinearLayout
    private lateinit var excerptsLabel: TextView
    private lateinit var unauthorizedText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("PlantDetail", "onCreateView: plantId=${args.plantId}")
        return inflater.inflate(R.layout.fragment_plant_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PlantDetail", "onViewCreated: plantId=${args.plantId}")

        try {
            plantImage = view.findViewById(R.id.image_plant_detail)
            textTitle = view.findViewById(R.id.text_title)
            plantDescription = view.findViewById(R.id.text_plant_description)
            plantLore = view.findViewById(R.id.text_plant_lore)
            unlockCondition = view.findViewById(R.id.text_unlock_condition)
            gardenToggleButton = view.findViewById(R.id.button_garden_toggle)
            backButton = view.findViewById(R.id.button_back)
            discoveryText = view.findViewById(R.id.text_plant_discovery)
            wereadText = view.findViewById(R.id.text_plant_weread)
            appDaysText = view.findViewById(R.id.text_plant_app_days)
            readNoteText = view.findViewById(R.id.text_plant_readnote)
            readingBody = view.findViewById(R.id.layout_reading_body)
            durationValue = view.findViewById(R.id.text_reading_duration_value)
            highlightValue = view.findViewById(R.id.text_reading_highlight_value)
            categoriesLayout = view.findViewById(R.id.layout_reading_categories)
            booksLayout = view.findViewById(R.id.layout_reading_books)
            excerptsLayout = view.findViewById(R.id.layout_reading_excerpts)
            excerptsLabel = view.findViewById(R.id.text_excerpts_label)
            unauthorizedText = view.findViewById(R.id.text_weread_unauthorized)
            Log.d("PlantDetail", "视图初始化完成")
        } catch (e: Exception) {
            Log.e("PlantDetail", "视图初始化失败!!!", e)
            Toast.makeText(requireContext(), "视图初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        backButton.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e("PlantDetail", "返回导航失败", e)
            }
        }

        // 解析植物 String ID
        val plantIndex = (args.plantId - 1L).toInt().coerceIn(0, PlantDefinitions.all.lastIndex)
        val plantStringId = PlantDefinitions.all.getOrNull(plantIndex)?.id

        if (plantStringId.isNullOrBlank()) {
            Log.e("PlantDetail", "无法解析植物ID: plantId=${args.plantId}, index=$plantIndex")
            textTitle.text = "植物不存在"
            return
        }

        try {
            viewModel.loadPlant(plantStringId)
            Log.d("PlantDetail", "viewModel.loadPlant($plantStringId) 调用成功")
        } catch (e: Exception) {
            Log.e("PlantDetail", "加载植物详情失败", e)
            textTitle.text = "加载失败"
            return
        }

        // 观察 UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        try {
                            renderState(state)
                        } catch (e: Exception) {
                            Log.e("PlantDetail", "渲染植物详情失败", e)
                        }
                    }
                }
                launch {
                    viewModel.messages.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderState(state: PlantDetailUiState) {
        try {
            Log.d("PlantDetail", "renderState: name=${state.name}, plantIdStr=${state.plantIdStr}")

            // 顶栏标题：植物名
            textTitle?.text = state.name

            if (state.isUnlocked) {
                plantDescription.text = state.description
                plantLore.text = state.lore
                unlockCondition.text = state.unlockDate?.let {
                    getString(R.string.plant_discovered_on, it)
                } ?: getString(R.string.plant_discovered)
                discoveryText.text = state.discoveryLine
                wereadText.text = state.readingWindowLabel
                appDaysText.text = state.appDaysLine
                renderReadingTime(state)
                readNoteText.text = state.readNoteLine
                gardenToggleButton?.visibility = View.VISIBLE
                gardenToggleButton?.text = if (state.isInGarden) {
                    getString(R.string.label_remove_from_garden)
                } else {
                    getString(R.string.label_put_in_garden)
                }
                gardenToggleButton?.setOnClickListener {
                    try { viewModel.toggleGardenStatus() } catch (e: Exception) {
                        Log.e("PlantDetail", "切换花园状态失败", e)
                    }
                }
            } else {
                plantDescription.text = getString(R.string.plant_undiscovered_description)
                plantLore.text = getString(R.string.plant_undiscovered_lore)
                unlockCondition.text = getString(R.string.msg_random_discovery)
                gardenToggleButton?.visibility = View.GONE
            }

            // 加载植物大图
            loadPlantImage(state)
        } catch (e: Exception) {
            Log.e("PlantDetail", "renderState异常", e)
        }
    }

    /**
     * 渲染「阅读时光」窗口化区块：时长 / 划线 / 偏爱类型 / 书目。
     * 三态：加载中(…) / 未连接(提示) / 已加载(真实数据)。
     */
    private fun renderReadingTime(state: PlantDetailUiState) {
        if (!state.wereadLoaded) {
            readingBody.visibility = View.VISIBLE
            unauthorizedText.visibility = View.GONE
            durationValue.text = "…"
            highlightValue.text = "…"
            categoriesLayout.removeAllViews()
            booksLayout.removeAllViews()
            return
        }
        if (!state.wereadAuthorized) {
            readingBody.visibility = View.GONE
            unauthorizedText.visibility = View.VISIBLE
            return
        }
        readingBody.visibility = View.VISIBLE
        unauthorizedText.visibility = View.GONE
        durationValue.text = state.readingDurationText.ifBlank { "暂未记录" }
        highlightValue.text = state.readingHighlightText.ifBlank { "0 条划线" }
        renderCategories(state.readingCategories)
        renderBooks(state.readingBookTitles)
        renderExcerpts(state.readingExcerpts)
    }

    /**
     * 偏爱类型：每个类型是一枚可点击的浅绿胶囊，点击展开该类在窗口内的书目列表。
     */
    private fun renderCategories(cats: List<CategoryDetail>) {
        categoriesLayout.removeAllViews()
        val ctx = context ?: return
        if (cats.isEmpty()) {
            categoriesLayout.addView(makeLine(ctx, "· 这段时间还没留下分类偏好"))
            return
        }
        cats.forEach { cat ->
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }
            val pill = TextView(ctx).apply {
                text = "${cat.name} · ${cat.count} 本"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.ink_medium))
                setPadding(dpip(10), dpip(4), dpip(10), dpip(4))
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_chip)
                isClickable = true
                isFocusable = true
                applyFont(this, R.font.jinghua_laosong)
            }
            val detail = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dpip(6), dpip(6), 0, dpip(4))
            }
            if (cat.books.isNotEmpty()) {
                cat.books.forEach { t -> detail.addView(makeLine(ctx, "· 《$t》", dpip(3))) }
            } else {
                detail.addView(makeLine(ctx, "（全量阅读偏好，未限定窗口）"))
            }
            pill.setOnClickListener {
                detail.visibility = if (detail.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpip(6)
            pill.layoutParams = lp
            container.addView(pill)
            container.addView(detail)
            categoriesLayout.addView(container)
        }
    }

    private fun renderBooks(titles: List<String>) {
        booksLayout.removeAllViews()
        val ctx = context ?: return
        if (titles.isEmpty()) {
            booksLayout.addView(makeLine(ctx, "· 这段时间还没翻过书"))
            return
        }
        titles.forEach { title ->
            booksLayout.addView(makeLine(ctx, "· 《$title》", dpip(4)))
        }
    }

    /**
     * 书页拾光：窗口内真实划线，以浅绿引文胶囊呈现，文凯字体。
     */
    private fun renderExcerpts(items: List<ExcerptItem>) {
        if (items.isEmpty()) {
            excerptsLabel.visibility = View.GONE
            excerptsLayout.visibility = View.GONE
            excerptsLayout.removeAllViews()
            return
        }
        excerptsLabel.visibility = View.VISIBLE
        excerptsLayout.visibility = View.VISIBLE
        excerptsLayout.removeAllViews()
        val ctx = context ?: return
        items.forEach { ex ->
            val box = TextView(ctx).apply {
                text = "「${ex.text}」\n—— 《${ex.source}》"
                textSize = 13f
                setLineSpacing(dpip(4).toFloat(), 1f)
                setTextColor(ContextCompat.getColor(ctx, R.color.ink_medium))
                setPadding(dpip(10), dpip(8), dpip(10), dpip(8))
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_chip)
                applyFont(this, R.font.lxgw_wenkai)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpip(8)
            box.layoutParams = lp
            excerptsLayout.addView(box)
        }
    }

    private fun makeLine(ctx: android.content.Context, text: String, bottomMargin: Int = 0): TextView {
        return TextView(ctx).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.ink_medium))
            applyFont(this, R.font.lxgw_wenkai)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = bottomMargin
            layoutParams = lp
        }
    }

    /** 给动态创建的 TextView 应用 res/font 下的字体（XML 里用 fontFamily，代码里需手动 setTypeface） */
    private fun applyFont(tv: TextView, fontRes: Int) {
        try {
            tv.typeface = ResourcesCompat.getFont(requireContext(), fontRes)
        } catch (_: Exception) {
            // 字体缺失时退化为系统默认，不影响功能
        }
    }

    private fun dpip(dp: Int): Int {
        return (dp * (resources?.displayMetrics?.density ?: 1f)).toInt()
    }

    private fun loadPlantImage(state: PlantDetailUiState) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    if (state.isUnlocked) {
                        PlantImageLoader.loadByStringId(ctx, state.plantIdStr)
                    } else {
                        PlantImageLoader.loadSilhouetteByStringId(ctx, state.plantIdStr)
                    }
                } catch (e: Exception) {
                    Log.e("PlantDetail", "加载植物图片失败: ${state.plantIdStr}", e)
                    null
                }
            }
            if (view != null && isAdded) {
                bitmap?.let {
                    plantImage.setImageBitmap(it)
                }
            }
        }
    }
}
