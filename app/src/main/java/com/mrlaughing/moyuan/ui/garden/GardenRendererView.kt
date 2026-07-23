package com.mrlaughing.moyuan.ui.garden

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.HapticFeedbackConstants
import com.mrlaughing.moyuan.data.model.Season
import com.mrlaughing.moyuan.data.model.Weather
import com.mrlaughing.moyuan.render.GardenRenderer
import com.mrlaughing.moyuan.render.GardenLayout
import com.mrlaughing.moyuan.render.PlantRenderInfo

/** Renders the interactive 2.5D garden scene. */
class GardenRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val AMBIENT_DURATION_MS = 6200L
        private const val AMBIENT_FRAME_DELAY_MS = 83L
        private const val FULL_CIRCLE_RADIANS = (Math.PI * 2).toFloat()
        private const val EDIT_LONG_PRESS_MS = 420L
    }

    private var renderPlants: List<PlantRenderInfo> = emptyList()
    private var currentSeason: Season = Season.SPRING
    private var currentWeather: Weather = Weather.CLEAR
    private var currentGridCols: Int = 3
    private var currentGridRows: Int = 3
    private var onPlantClickListener: ((Long) -> Unit)? = null
    private var onEmptyPlotClickListener: (() -> Unit)? = null
    private var onPlantMoveListener: ((Long, Int) -> Unit)? = null
    private var editingEnabled = false
    private var dragCandidate: PlantRenderInfo? = null
    private var dragOriginalPlants: List<PlantRenderInfo>? = null
    private var isDragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private val beginDrag = Runnable {
        val candidate = dragCandidate ?: return@Runnable
        isDragging = true
        dragOriginalPlants = renderPlants
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        parent?.requestDisallowInterceptTouchEvent(true)
        contentDescription = "正在移动${candidate.plantName}"
    }
    private var ambientPhase = 0f
    private var sceneBitmap: Bitmap? = null
    private var sceneDirty = true
    private var wateringProgress = 0f
    private var wateringAnimator: ValueAnimator? = null
    private val wateringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ambientFrame = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow || windowVisibility != VISIBLE) return

            val progress = (SystemClock.uptimeMillis() % AMBIENT_DURATION_MS).toFloat() /
                AMBIENT_DURATION_MS
            ambientPhase = progress * FULL_CIRCLE_RADIANS
            invalidate()
            postDelayed(this, AMBIENT_FRAME_DELAY_MS)
        }
    }

    init {
        isClickable = true
    }

    fun updatePlants(plants: List<PlantRenderInfo>) {
        renderPlants = plants
        postInvalidateOnAnimation()
    }

    fun setWeatherSeason(season: Season, weather: Weather) {
        if (currentSeason != season || currentWeather != weather) {
            currentSeason = season
            currentWeather = weather
            sceneDirty = true
            postInvalidateOnAnimation()
        }
    }

    fun setGridLayout(cols: Int, rows: Int) {
        if (currentGridCols == cols && currentGridRows == rows) return
        currentGridCols = cols
        currentGridRows = rows
        sceneDirty = true
        postInvalidateOnAnimation()
    }

    fun notifySceneAssetsChanged() {
        sceneDirty = true
        postInvalidateOnAnimation()
    }

    fun setOnPlantClickListener(listener: (Long) -> Unit) {
        onPlantClickListener = listener
    }

    fun setOnEmptyPlotClickListener(listener: () -> Unit) {
        onEmptyPlotClickListener = listener
    }

    fun setOnPlantMoveListener(listener: (Long, Int) -> Unit) {
        onPlantMoveListener = listener
    }

    fun setEditingEnabled(enabled: Boolean) {
        editingEnabled = enabled
        if (!enabled) cancelDrag()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        GardenRenderer.gridCols = currentGridCols
        GardenRenderer.gridRows = currentGridRows

        val background = getOrCreateSceneBitmap()
        canvas.drawBitmap(background, 0f, 0f, null)
        GardenRenderer.drawPlants(
            canvas = canvas,
            plants = renderPlants,
            canvasWidth = width,
            canvasHeight = height,
            season = currentSeason,
            ambientPhase = ambientPhase
        )
        drawWateringFeedback(canvas)
    }

    fun playWateringAnimation() {
        wateringAnimator?.cancel()
        wateringAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 760L
            addUpdateListener {
                wateringProgress = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    private fun drawWateringFeedback(canvas: Canvas) {
        val progress = wateringProgress
        if (progress <= 0f || progress >= 1f) return
        val cells = GardenLayout.calculate(currentGridCols, currentGridRows, width, height)
        if (cells.isEmpty()) return
        val tile = cells.first().tileSize
        val fade = if (progress < 0.65f) 1f else (1f - progress) / 0.35f

        wateringPaint.color = Color.argb((42 * fade).toInt(), 47, 107, 88)
        wateringPaint.style = Paint.Style.FILL
        cells.forEach { cell ->
            canvas.drawCircle(cell.centerX, cell.centerY, tile * 0.20f * fade, wateringPaint)
        }

        wateringPaint.color = Color.argb((155 * fade).toInt(), 111, 170, 193)
        wateringPaint.strokeWidth = (tile * 0.025f).coerceAtLeast(2f)
        wateringPaint.strokeCap = Paint.Cap.ROUND
        repeat(9) { index ->
            val lane = (index - 4) / 4f
            val x = width * 0.5f + lane * tile * 1.4f
            val startY = height * 0.25f + (index % 3) * tile * 0.09f
            val y = startY + progress * tile * 1.25f
            canvas.drawLine(x, y, x - tile * 0.035f, y + tile * 0.10f, wateringPaint)
        }
    }

    private fun getOrCreateSceneBitmap(): Bitmap {
        val existing = sceneBitmap
        if (!sceneDirty && existing != null && !existing.isRecycled) return existing

        existing?.recycle()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            GardenRenderer.drawScene(
                canvas = Canvas(bitmap),
                width = width,
                height = height,
                season = currentSeason,
                weather = currentWeather
            )
            sceneBitmap = bitmap
            sceneDirty = false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sceneDirty = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragCandidate = findPlantAt(event.x, event.y)
                dragCandidate?.let {
                    dragOffsetX = event.x - it.x
                    dragOffsetY = event.y - it.y
                    if (editingEnabled) postDelayed(beginDrag, EDIT_LONG_PRESS_MS)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val movingId = dragCandidate?.plantId ?: return true
                    val newX = event.x - dragOffsetX
                    val newY = event.y - dragOffsetY
                    renderPlants = renderPlants.map {
                        if (it.plantId == movingId) it.copy(x = newX, y = newY) else it
                    }
                    postInvalidateOnAnimation()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(beginDrag)
                if (isDragging) {
                    val movingId = dragCandidate?.plantId
                    val target = findLawnAt(event.x, event.y)
                    dragOriginalPlants?.let { renderPlants = it }
                    cancelDrag(clearOriginal = true)
                    if (movingId != null && target != null) {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onPlantMoveListener?.invoke(movingId, target.fillRank)
                    }
                    postInvalidateOnAnimation()
                    return true
                }

                val plant = dragCandidate ?: findPlantAt(event.x, event.y)
                dragCandidate = null
                if (plant != null) {
                    onPlantClickListener?.invoke(plant.plantId)
                } else if (findLawnAt(event.x, event.y) != null) {
                    onEmptyPlotClickListener?.invoke()
                }
                return performClick()
            }
        }
        return true
    }

    private fun findPlantAt(x: Float, y: Float): PlantRenderInfo? = renderPlants
        .asReversed()
        .firstOrNull {
            val imageSize = it.scale * 10f
            val horizontalRadius = imageSize * 0.48f
            x in (it.x - horizontalRadius)..(it.x + horizontalRadius) &&
                y in (it.y - imageSize * 0.82f)..(it.y + imageSize * 0.20f)
        }

    private fun findLawnAt(x: Float, y: Float) =
        GardenLayout.calculate(currentGridCols, currentGridRows, width, height)
            .minByOrNull { cell ->
                val dx = kotlin.math.abs(x - cell.centerX) / (cell.tileSize * 0.48f)
                val dy = kotlin.math.abs(y - cell.centerY) / (cell.tileSize * 0.24f)
                dx + dy
            }
            ?.takeIf { cell ->
                val dx = kotlin.math.abs(x - cell.centerX) / (cell.tileSize * 0.48f)
                val dy = kotlin.math.abs(y - cell.centerY) / (cell.tileSize * 0.24f)
                dx + dy <= 1.15f
            }

    private fun cancelDrag(clearOriginal: Boolean = false) {
        removeCallbacks(beginDrag)
        if (!clearOriginal) dragOriginalPlants?.let { renderPlants = it }
        dragCandidate = null
        dragOriginalPlants = null
        isDragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
        contentDescription = context.getString(com.mrlaughing.moyuan.R.string.desc_garden_scene)
        postInvalidateOnAnimation()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAmbientAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startAmbientAnimation()
        } else {
            stopAmbientAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        stopAmbientAnimation()
        wateringAnimator?.cancel()
        wateringAnimator = null
        sceneBitmap?.recycle()
        sceneBitmap = null
        sceneDirty = true
        super.onDetachedFromWindow()
    }

    private fun startAmbientAnimation() {
        stopAmbientAnimation()
        if (isAttachedToWindow && windowVisibility == VISIBLE) {
            ambientFrame.run()
        }
    }

    private fun stopAmbientAnimation() {
        removeCallbacks(ambientFrame)
    }
}
