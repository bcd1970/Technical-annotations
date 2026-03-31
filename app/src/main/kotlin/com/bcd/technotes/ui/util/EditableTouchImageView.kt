package com.bcd.technotes.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.ortiz.touchview.TouchImageView
import kotlin.math.abs

class EditableTouchImageView(context: Context) : TouchImageView(context) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    var editMode = false
        set(value) { if (field != value) { field = value; invalidate() } }

    var activePhotoIndex = -1
        set(value) { if (!bridgeActive && field != value) { field = value; invalidate() } }

    var photoBounds: List<RectF> = emptyList()

    var isHorizontalLayout = true

    var onReorderComplete: ((newOrder: List<Int>) -> Unit)? = null

    // --- Crop mode ---
    var cropMode = false
        set(value) {
            field = value
            if (!value) {
                activeCropHandle = -1
                cropRect.setEmpty()
            }
            invalidate()
        }

    var cropRect = RectF()

    private var activeCropHandle = -1
    private var cropMoveOffsetX = 0f
    private var cropMoveOffsetY = 0f
    private val handleRadiusPx by lazy { 10f * resources.displayMetrics.density }
    private val handleHitRadiusPx by lazy { 24f * resources.displayMetrics.density }
    private val minCropSizePx = 50f

    fun initCropRect() {
        if (activePhotoIndex in photoBounds.indices) {
            cropRect = RectF(photoBounds[activePhotoIndex])
        }
    }

    // --- Integrated pan for grid layouts ---
    var panEnabled = false
    var photoVisibleRects: List<RectF> = emptyList()
    var onPanComplete: ((panX: Float, panY: Float) -> Unit)? = null
    var getTransformedBitmap: ((index: Int) -> Bitmap?)? = null

    private var isPanDragging = false
    private var panBridgeActive = false
    private var panDragOffsetX = 0f
    private var panDragOffsetY = 0f
    private var panTouchDownX = 0f
    private var panTouchDownY = 0f
    private var panBitmap: Bitmap? = null

    private fun isPhotoCropped(index: Int): Boolean {
        if (index !in photoVisibleRects.indices) return false
        val vis = photoVisibleRects[index]
        return vis.left > 0.001f || vis.top > 0.001f || vis.right < 0.999f || vis.bottom < 0.999f
    }

    private fun clearPanState() {
        panBitmap = null
        panBridgeActive = false
        isPanDragging = false
        panDragOffsetX = 0f
        panDragOffsetY = 0f
    }

    private val blackPaint = Paint().apply { color = android.graphics.Color.BLACK }

    private val photoOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        alpha = 120
        strokeWidth = 3f
    }

    // --- Paints ---
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        alpha = 180
        strokeWidth = 4f
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF4FC3F7.toInt()
        strokeWidth = 8f
    }

    private val dimPaint = Paint()

    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val cropDimPaint = Paint().apply {
        color = android.graphics.Color.argb(160, 0, 0, 0)
    }

    private val cropBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        strokeWidth = 3f
    }

    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }

    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.DKGRAY
        strokeWidth = 2f
    }

    // --- Reorder drag state ---
    private var isReorderDragging = false
    private var isDragCandidate = false
    private var dragCancelled = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var dragOffsetPx = 0f
    private var dragOffsetY = 0f
    private var photoCrops: List<Bitmap> = emptyList()
    private var currentOrder: MutableList<Int> = mutableListOf()
    private var activeDisplayIndex = -1
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var slotCenters: List<Float> = emptyList()
    private var slotCenters2D: List<PointF> = emptyList()

    // --- Animation state ---
    private var dimFactor = 1f
    private var activeScale = 1f
    private var isSettling = false
    private val slotAnimOffsets = mutableMapOf<Int, Float>()
    private val slotAnimOffsetsY = mutableMapOf<Int, Float>()
    private var staleBitmapRef: Bitmap? = null
    private var bridgeActive = false
    private var bridgeFullBitmap: Bitmap? = null
    private var bridgeTargetCellIdx = -1

    override fun setImageBitmap(bm: Bitmap?) {
        if (panBridgeActive) {
            clearPanState()
        }
        if (staleBitmapRef != null) {
            if (bm === staleBitmapRef) return
            staleBitmapRef = null
            bridgeActive = false
            bridgeFullBitmap = null
            bridgeTargetCellIdx = -1
            isReorderDragging = false
            isSettling = false
            photoCrops = emptyList()
            currentOrder.clear()
            activeDisplayIndex = -1
            slotCenters = emptyList()
            slotCenters2D = emptyList()
            dragOffsetPx = 0f
            dragOffsetY = 0f
        }
        super.setImageBitmap(bm)
    }

    fun photoIndexAt(bitmapX: Float, bitmapY: Float): Int =
        photoBounds.indexOfFirst {
            bitmapX >= it.left && bitmapX <= it.right &&
            bitmapY >= it.top && bitmapY <= it.bottom
        }

    // --- Crop handle helpers ---

    private fun getCropHandlePositions(): List<PointF> {
        val r = cropRect
        return listOf(
            PointF(r.left, r.top),          // 0: TL
            PointF(r.centerX(), r.top),      // 1: TC
            PointF(r.right, r.top),          // 2: TR
            PointF(r.left, r.centerY()),     // 3: ML
            PointF(r.right, r.centerY()),    // 4: MR
            PointF(r.left, r.bottom),        // 5: BL
            PointF(r.centerX(), r.bottom),   // 6: BC
            PointF(r.right, r.bottom)        // 7: BR
        )
    }

    private fun hitTestCropHandle(screenX: Float, screenY: Float): Int {
        val handles = getCropHandlePositions()
        for (i in handles.indices) {
            val screen = transformCoordBitmapToTouch(handles[i].x, handles[i].y)
            val dx = screenX - screen.x
            val dy = screenY - screen.y
            if (dx * dx + dy * dy <= handleHitRadiusPx * handleHitRadiusPx) return i
        }
        val tl = transformCoordBitmapToTouch(cropRect.left, cropRect.top)
        val br = transformCoordBitmapToTouch(cropRect.right, cropRect.bottom)
        if (screenX in tl.x..br.x && screenY in tl.y..br.y) return 8
        return -1
    }

    private fun handleCropTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val handle = hitTestCropHandle(event.x, event.y)
                if (handle >= 0) {
                    activeCropHandle = handle
                    if (handle == 8) {
                        val bp = transformCoordTouchToBitmap(event.x, event.y, true)
                        cropMoveOffsetX = bp.x - cropRect.left
                        cropMoveOffsetY = bp.y - cropRect.top
                    }
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeCropHandle >= 0) {
                    updateCropDrag(event.x, event.y)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeCropHandle >= 0) {
                    activeCropHandle = -1
                    return true
                }
                return false
            }
        }
        return false
    }

    private fun updateCropDrag(screenX: Float, screenY: Float) {
        val bp = transformCoordTouchToBitmap(screenX, screenY, true)
        val pb = photoBounds.getOrNull(activePhotoIndex) ?: return

        when (activeCropHandle) {
            0 -> {
                cropRect.left = bp.x.coerceIn(pb.left, cropRect.right - minCropSizePx)
                cropRect.top = bp.y.coerceIn(pb.top, cropRect.bottom - minCropSizePx)
            }
            1 -> cropRect.top = bp.y.coerceIn(pb.top, cropRect.bottom - minCropSizePx)
            2 -> {
                cropRect.right = bp.x.coerceIn(cropRect.left + minCropSizePx, pb.right)
                cropRect.top = bp.y.coerceIn(pb.top, cropRect.bottom - minCropSizePx)
            }
            3 -> cropRect.left = bp.x.coerceIn(pb.left, cropRect.right - minCropSizePx)
            4 -> cropRect.right = bp.x.coerceIn(cropRect.left + minCropSizePx, pb.right)
            5 -> {
                cropRect.left = bp.x.coerceIn(pb.left, cropRect.right - minCropSizePx)
                cropRect.bottom = bp.y.coerceIn(cropRect.top + minCropSizePx, pb.bottom)
            }
            6 -> cropRect.bottom = bp.y.coerceIn(cropRect.top + minCropSizePx, pb.bottom)
            7 -> {
                cropRect.right = bp.x.coerceIn(cropRect.left + minCropSizePx, pb.right)
                cropRect.bottom = bp.y.coerceIn(cropRect.top + minCropSizePx, pb.bottom)
            }
            8 -> {
                val w = cropRect.width()
                val h = cropRect.height()
                val newLeft = (bp.x - cropMoveOffsetX).coerceIn(pb.left, pb.right - w)
                val newTop = (bp.y - cropMoveOffsetY).coerceIn(pb.top, pb.bottom - h)
                cropRect.set(newLeft, newTop, newLeft + w, newTop + h)
            }
        }
        invalidate()
    }

    private fun drawCropOverlay(canvas: Canvas) {
        if (activePhotoIndex !in photoBounds.indices) return
        val pb = photoBounds[activePhotoIndex]

        val photoTL = transformCoordBitmapToTouch(pb.left, pb.top)
        val photoBR = transformCoordBitmapToTouch(pb.right, pb.bottom)
        val cropTL = transformCoordBitmapToTouch(cropRect.left, cropRect.top)
        val cropBR = transformCoordBitmapToTouch(cropRect.right, cropRect.bottom)

        // Dim outside crop rect within photo
        canvas.drawRect(photoTL.x, photoTL.y, photoBR.x, cropTL.y, cropDimPaint)
        canvas.drawRect(photoTL.x, cropBR.y, photoBR.x, photoBR.y, cropDimPaint)
        canvas.drawRect(photoTL.x, cropTL.y, cropTL.x, cropBR.y, cropDimPaint)
        canvas.drawRect(cropBR.x, cropTL.y, photoBR.x, cropBR.y, cropDimPaint)

        // Crop border
        canvas.drawRect(cropTL.x, cropTL.y, cropBR.x, cropBR.y, cropBorderPaint)

        // Handles
        for (handle in getCropHandlePositions()) {
            val screen = transformCoordBitmapToTouch(handle.x, handle.y)
            canvas.drawCircle(screen.x, screen.y, handleRadiusPx, handleFillPaint)
            canvas.drawCircle(screen.x, screen.y, handleRadiusPx, handleStrokePaint)
        }
    }

    // --- Frame-based animation (immune to system animator_duration_scale) ---

    private inner class FrameAnimator(
        private val durationMs: Long,
        private val interpolator: android.view.animation.Interpolator,
        private val onUpdate: (fraction: Float) -> Unit,
        private val onEnd: (() -> Unit)? = null
    ) {
        private var startTimeNs = 0L
        private var cancelled = false

        private val frameCallback = object : Runnable {
            override fun run() {
                if (cancelled) return
                val elapsed = System.nanoTime() - startTimeNs
                val rawFraction = (elapsed.toFloat() / (durationMs * 1_000_000L)).coerceAtMost(1f)
                val fraction = interpolator.getInterpolation(rawFraction)
                onUpdate(fraction)
                if (rawFraction < 1f) {
                    postOnAnimation(this)
                } else {
                    onEnd?.invoke()
                }
            }
        }

        fun start() {
            startTimeNs = System.nanoTime()
            cancelled = false
            postOnAnimation(frameCallback)
        }

        fun cancel() {
            cancelled = true
            removeCallbacks(frameCallback)
        }
    }

    // --- Animation helpers ---

    private var dragStartAnimator: FrameAnimator? = null
    private var settleAnimator: FrameAnimator? = null
    private val swapAnimators = mutableMapOf<Int, FrameAnimator>()

    private fun cancelAllReorderAnimations() {
        dragStartAnimator?.cancel()
        dragStartAnimator = null
        settleAnimator?.cancel()
        settleAnimator = null
        swapAnimators.values.forEach { it.cancel() }
        swapAnimators.clear()
        slotAnimOffsets.clear()
        slotAnimOffsetsY.clear()
    }

    private fun clearReorderState() {
        cancelAllReorderAnimations()
        isReorderDragging = false
        isSettling = false
        staleBitmapRef = null
        bridgeActive = false
        bridgeFullBitmap = null
        bridgeTargetCellIdx = -1
        photoCrops = emptyList()
        currentOrder.clear()
        activeDisplayIndex = -1
        slotCenters = emptyList()
        slotCenters2D = emptyList()
        dragOffsetPx = 0f
        dragOffsetY = 0f
        dimFactor = 1f
        activeScale = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllReorderAnimations()
    }

    // --- Reorder helpers ---

    private fun startReorderDrag() {
        val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return
        photoCrops = photoBounds.map { rect ->
            Bitmap.createBitmap(
                bmp,
                rect.left.toInt().coerceIn(0, bmp.width - 1),
                rect.top.toInt().coerceIn(0, bmp.height - 1),
                rect.width().toInt().coerceAtMost(bmp.width - rect.left.toInt()),
                rect.height().toInt().coerceAtMost(bmp.height - rect.top.toInt())
            )
        }
        currentOrder = photoBounds.indices.toMutableList()
        activeDisplayIndex = currentOrder.indexOf(activePhotoIndex)
        dragOffsetPx = 0f
        dragOffsetY = 0f
        isReorderDragging = true

        if (isHorizontalLayout) {
            slotCenters = photoBounds.map { rect ->
                val left = transformCoordBitmapToTouch(rect.left, 0f).x
                val right = transformCoordBitmapToTouch(rect.right, 0f).x
                (left + right) / 2f
            }
        } else {
            slotCenters2D = photoBounds.map { rect ->
                val tl = transformCoordBitmapToTouch(rect.left, rect.top)
                val br = transformCoordBitmapToTouch(rect.right, rect.bottom)
                PointF((tl.x + br.x) / 2f, (tl.y + br.y) / 2f)
            }
        }

        cancelAllReorderAnimations()
        dimFactor = 1f
        activeScale = 1f

        dragStartAnimator = FrameAnimator(80, DecelerateInterpolator(), { fraction ->
            dimFactor = 1f - 0.4f * fraction
            activeScale = 1f + 0.05f * fraction
            invalidate()
        }).also { it.start() }

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    // --- 1D horizontal reorder update ---

    private fun updateReorderDragH(screenDragX: Float) {
        dragOffsetPx = screenDragX

        val activeOrigBounds = photoBounds[activePhotoIndex]
        val activeOrigLeft = transformCoordBitmapToTouch(activeOrigBounds.left, 0f).x
        val activeOrigRight = transformCoordBitmapToTouch(activeOrigBounds.right, 0f).x
        val activeCenterNow = (activeOrigLeft + activeOrigRight) / 2f + dragOffsetPx

        val idx = activeDisplayIndex

        if (idx < currentOrder.size - 1) {
            val nextSlotDataIdx = currentOrder[idx + 1]
            val nextSlotCenter = slotCenters[nextSlotDataIdx]
            if (activeCenterNow > nextSlotCenter) {
                val displacedDataIdx = nextSlotDataIdx
                val activeScreenWidth = run {
                    val aL = transformCoordBitmapToTouch(photoBounds[activePhotoIndex].left, 0f).x
                    val aR = transformCoordBitmapToTouch(photoBounds[activePhotoIndex].right, 0f).x
                    aR - aL
                }
                currentOrder[idx] = currentOrder[idx + 1].also { currentOrder[idx + 1] = currentOrder[idx] }
                activeDisplayIndex = idx + 1
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animateSlotTransition(displacedDataIdx, activeScreenWidth, 0f)
            }
        }

        val idx2 = activeDisplayIndex
        if (idx2 > 0) {
            val prevSlotDataIdx = currentOrder[idx2 - 1]
            val prevSlotCenter = slotCenters[prevSlotDataIdx]
            if (activeCenterNow < prevSlotCenter) {
                val displacedDataIdx = prevSlotDataIdx
                val activeScreenWidth = run {
                    val aL = transformCoordBitmapToTouch(photoBounds[activePhotoIndex].left, 0f).x
                    val aR = transformCoordBitmapToTouch(photoBounds[activePhotoIndex].right, 0f).x
                    aR - aL
                }
                currentOrder[idx2] = currentOrder[idx2 - 1].also { currentOrder[idx2 - 1] = currentOrder[idx2] }
                activeDisplayIndex = idx2 - 1
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animateSlotTransition(displacedDataIdx, -activeScreenWidth, 0f)
            }
        }

        invalidate()
    }

    // --- 2D grid reorder update ---

    private fun updateReorderDrag2D(screenDragX: Float, screenDragY: Float) {
        dragOffsetPx = screenDragX
        dragOffsetY = screenDragY

        // Active photo center in screen coords
        val activeOrigBounds = photoBounds[activePhotoIndex]
        val aTL = transformCoordBitmapToTouch(activeOrigBounds.left, activeOrigBounds.top)
        val aBR = transformCoordBitmapToTouch(activeOrigBounds.right, activeOrigBounds.bottom)
        val activeCenterX = (aTL.x + aBR.x) / 2f + dragOffsetPx
        val activeCenterY = (aTL.y + aBR.y) / 2f + dragOffsetY

        // Check each cell by CELL index (photoBounds[cellIdx] = fixed cell position)
        for (cellIdx in currentOrder.indices) {
            if (cellIdx == activeDisplayIndex) continue

            val cellBounds = photoBounds[cellIdx]
            val cTL = transformCoordBitmapToTouch(cellBounds.left, cellBounds.top)
            val cBR = transformCoordBitmapToTouch(cellBounds.right, cellBounds.bottom)

            if (activeCenterX in cTL.x..cBR.x && activeCenterY in cTL.y..cBR.y) {
                val displacedDataIdx = currentOrder[cellIdx]

                // Animation: displaced moves from its current cell (cellIdx) to active's cell (activeDisplayIndex)
                val activeCellBounds = photoBounds[activeDisplayIndex]
                val acTL = transformCoordBitmapToTouch(activeCellBounds.left, activeCellBounds.top)
                val acBR = transformCoordBitmapToTouch(activeCellBounds.right, activeCellBounds.bottom)
                val startOffX = ((cTL.x + cBR.x) - (acTL.x + acBR.x)) / 2f
                val startOffY = ((cTL.y + cBR.y) - (acTL.y + acBR.y)) / 2f

                currentOrder[activeDisplayIndex] = displacedDataIdx
                currentOrder[cellIdx] = activePhotoIndex
                activeDisplayIndex = cellIdx
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animateSlotTransition(displacedDataIdx, startOffX, startOffY)
                break
            }
        }

        invalidate()
    }

    private fun animateSlotTransition(dataIdx: Int, startOffsetX: Float, startOffsetY: Float) {
        swapAnimators[dataIdx]?.cancel()
        val currentOffX = slotAnimOffsets[dataIdx] ?: 0f
        val currentOffY = slotAnimOffsetsY[dataIdx] ?: 0f
        val effectiveStartX = startOffsetX + currentOffX
        val effectiveStartY = startOffsetY + currentOffY
        slotAnimOffsets[dataIdx] = effectiveStartX
        slotAnimOffsetsY[dataIdx] = effectiveStartY

        val animator = FrameAnimator(120, DecelerateInterpolator(), { fraction ->
            slotAnimOffsets[dataIdx] = effectiveStartX * (1f - fraction)
            slotAnimOffsetsY[dataIdx] = effectiveStartY * (1f - fraction)
            invalidate()
        }, {
            slotAnimOffsets.remove(dataIdx)
            slotAnimOffsetsY.remove(dataIdx)
            swapAnimators.remove(dataIdx)
        })
        swapAnimators[dataIdx] = animator
        animator.start()
    }

    private fun finishReorderWithBridge() {
        swapAnimators.values.forEach { it.cancel() }
        swapAnimators.clear()
        slotAnimOffsets.clear()
        slotAnimOffsetsY.clear()

        if (isHorizontalLayout) {
            // Snap active photo to target slot (horizontal)
            var targetBitmapX = 0f
            for (j in 0 until activeDisplayIndex) {
                targetBitmapX += photoBounds[currentOrder[j]].width()
            }
            val targetScreenLeft = transformCoordBitmapToTouch(targetBitmapX, 0f).x
            val activeOrigLeft = transformCoordBitmapToTouch(photoBounds[activePhotoIndex].left, 0f).x
            dragOffsetPx = targetScreenLeft - activeOrigLeft
        } else {
            // Snap active photo center to target cell center (2D)
            val targetCellBounds = photoBounds[activeDisplayIndex]
            val targetTL = transformCoordBitmapToTouch(targetCellBounds.left, targetCellBounds.top)
            val targetBR = transformCoordBitmapToTouch(targetCellBounds.right, targetCellBounds.bottom)
            val activeBounds = photoBounds[activePhotoIndex]
            val activeTL = transformCoordBitmapToTouch(activeBounds.left, activeBounds.top)
            val activeBR = transformCoordBitmapToTouch(activeBounds.right, activeBounds.bottom)
            dragOffsetPx = (targetTL.x + targetBR.x) / 2f - (activeTL.x + activeBR.x) / 2f
            dragOffsetY = (targetTL.y + targetBR.y) / 2f - (activeTL.y + activeBR.y) / 2f
        }
        dimFactor = 1f
        activeScale = 1f

        val orderChanged = currentOrder != photoBounds.indices.toList()
        val finalOrder = currentOrder.toList()

        // For 2D grids: fetch full photo and draw center-cropped into target cell during bridge
        if (!isHorizontalLayout && activeDisplayIndex != activePhotoIndex && orderChanged) {
            bridgeFullBitmap = getTransformedBitmap?.invoke(activePhotoIndex)
            bridgeTargetCellIdx = activeDisplayIndex
        }

        if (orderChanged) {
            bridgeActive = true
            staleBitmapRef = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            cancelAllReorderAnimations()
            isSettling = false
            invalidate()
            onReorderComplete?.invoke(finalOrder)
        } else {
            clearReorderState()
        }
    }

    // --- Pan completion ---

    private fun finishPanDrag() {
        if (activePhotoIndex !in photoVisibleRects.indices) {
            clearPanState()
            invalidate()
            return
        }
        val vis = photoVisibleRects[activePhotoIndex]
        val bound = photoBounds[activePhotoIndex]
        val tl = transformCoordBitmapToTouch(bound.left, bound.top)
        val br = transformCoordBitmapToTouch(bound.right, bound.bottom)
        val cellW = br.x - tl.x
        val cellH = br.y - tl.y
        val visW = vis.right - vis.left
        val visH = vis.bottom - vis.top
        val fullW = cellW / visW
        val fullH = cellH / visH

        val newVisLeft = vis.left - panDragOffsetX / fullW
        val newVisTop = vis.top - panDragOffsetY / fullH

        val maxVisLeft = 1f - visW
        val maxVisTop = 1f - visH
        val newPanX = if (maxVisLeft > 0.001f) (newVisLeft / maxVisLeft).coerceIn(0f, 1f) else 0.5f
        val newPanY = if (maxVisTop > 0.001f) (newVisTop / maxVisTop).coerceIn(0f, 1f) else 0.5f

        isPanDragging = false
        panBridgeActive = true
        onPanComplete?.invoke(newPanX, newPanY)
        invalidate()
    }

    // --- Touch dispatch ---

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Crop mode: handle crop touches, pass rest to zoom/pan
        if (cropMode && activePhotoIndex >= 0 && photoBounds.isNotEmpty()) {
            if (handleCropTouch(event)) return true
            return super.dispatchTouchEvent(event)
        }

        if (!editMode || isZoomed || photoBounds.isEmpty()) {
            return super.dispatchTouchEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                isDragCandidate = true
                dragCancelled = false
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isPanDragging) {
                    isPanDragging = false
                    panDragOffsetX = 0f
                    panDragOffsetY = 0f
                    clearPanState()
                    invalidate()
                }
                if (isReorderDragging) {
                    clearReorderState()
                }
                isDragCandidate = false
                dragCancelled = true
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragCancelled) return super.dispatchTouchEvent(event)
                if (isSettling) return true

                // Pan drag: update offset
                if (isPanDragging) {
                    panDragOffsetX = event.x - panTouchDownX
                    panDragOffsetY = event.y - panTouchDownY
                    invalidate()
                    return true
                }

                if (!isReorderDragging && isDragCandidate) {
                    val dx = abs(event.x - touchDownX)
                    val dy = abs(event.y - touchDownY)
                    val dragStarted = if (isHorizontalLayout) {
                        dx > touchSlop && dx > dy * 1.5f
                    } else {
                        maxOf(dx, dy) > touchSlop
                    }
                    if (dragStarted) {
                        isDragCandidate = false
                        val bitmapPoint = transformCoordTouchToBitmap(touchDownX, touchDownY, true)
                        val tappedIndex = photoIndexAt(bitmapPoint.x, bitmapPoint.y)
                        if (tappedIndex < 0) return super.dispatchTouchEvent(event)

                        // Grid layout: pan mode enabled + drag on active cropped photo → pan
                        if (panEnabled && !isHorizontalLayout && tappedIndex == activePhotoIndex && isPhotoCropped(tappedIndex)) {
                            val bmp = getTransformedBitmap?.invoke(tappedIndex)
                            if (bmp != null) {
                                panBitmap = bmp
                                isPanDragging = true
                                panTouchDownX = touchDownX
                                panTouchDownY = touchDownY
                                panDragOffsetX = event.x - panTouchDownX
                                panDragOffsetY = event.y - panTouchDownY
                                val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                                super.dispatchTouchEvent(cancel)
                                cancel.recycle()
                                invalidate()
                            }
                        } else {
                            activePhotoIndex = tappedIndex
                            startReorderDrag()
                            touchDownX = event.x
                            touchDownY = event.y
                            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                            super.dispatchTouchEvent(cancel)
                            cancel.recycle()
                        }
                    } else if (isHorizontalLayout && dy > touchSlop) {
                        isDragCandidate = false
                        dragCancelled = true
                        return super.dispatchTouchEvent(event)
                    } else {
                        return super.dispatchTouchEvent(event)
                    }
                }

                if (isReorderDragging) {
                    if (isHorizontalLayout) {
                        updateReorderDragH(event.x - touchDownX)
                    } else {
                        updateReorderDrag2D(event.x - touchDownX, event.y - touchDownY)
                    }
                    return true
                }

                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                if (isPanDragging) {
                    finishPanDrag()
                    return true
                }
                if (isReorderDragging && !isSettling) {
                    finishReorderWithBridge()
                    return true
                }
                isDragCandidate = false
                dragCancelled = false
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isPanDragging) {
                    isPanDragging = false
                    panDragOffsetX = 0f
                    panDragOffsetY = 0f
                    clearPanState()
                    invalidate()
                }
                if (isReorderDragging) {
                    clearReorderState()
                }
                isDragCandidate = false
                dragCancelled = false
                return super.dispatchTouchEvent(event)
            }
        }

        return super.dispatchTouchEvent(event)
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        // Crop mode: draw image + crop overlay
        if (cropMode) {
            super.onDraw(canvas)
            drawCropOverlay(canvas)
            return
        }

        if (!isReorderDragging) {
            super.onDraw(canvas)
            if (!editMode || photoBounds.isEmpty()) return
            photoBounds.forEachIndexed { index, bound ->
                val topLeft = transformCoordBitmapToTouch(bound.left, bound.top)
                val bottomRight = transformCoordBitmapToTouch(bound.right, bound.bottom)
                val paint = if (index == activePhotoIndex) activePaint else inactivePaint
                canvas.drawRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, paint)
            }
            // Live pan: draw transformed photo at adjusted offset within cell
            if ((isPanDragging || panBridgeActive) && panBitmap != null && activePhotoIndex in photoBounds.indices && activePhotoIndex in photoVisibleRects.indices) {
                val bmp = panBitmap!!
                val vis = photoVisibleRects[activePhotoIndex]
                val bound = photoBounds[activePhotoIndex]
                val tl = transformCoordBitmapToTouch(bound.left, bound.top)
                val br = transformCoordBitmapToTouch(bound.right, bound.bottom)
                val cellW = br.x - tl.x
                val cellH = br.y - tl.y

                // Same fill scale as stitching
                val fillScale = maxOf(cellW / bmp.width, cellH / bmp.height)
                val photoW = bmp.width * fillScale
                val photoH = bmp.height * fillScale

                // Current offset from visibleRect + drag
                val visW = vis.right - vis.left
                val visH = vis.bottom - vis.top
                val fullW = cellW / visW
                val fullH = cellH / visH
                val baseLeft = tl.x - vis.left * fullW
                val baseTop = tl.y - vis.top * fullH
                val newLeft = baseLeft + panDragOffsetX
                val newTop = baseTop + panDragOffsetY

                // Clip to cell, fill black, draw photo
                canvas.save()
                canvas.clipRect(tl.x, tl.y, br.x, br.y)
                canvas.drawRect(tl.x, tl.y, br.x, br.y, blackPaint)
                canvas.drawBitmap(
                    bmp, null,
                    android.graphics.RectF(newLeft, newTop, newLeft + photoW, newTop + photoH),
                    photoPaint
                )
                canvas.restore()

                // Photo border outline (shows full photo edges)
                canvas.drawRect(newLeft, newTop, newLeft + photoW, newTop + photoH, photoOutlinePaint)
                canvas.drawRect(tl.x, tl.y, br.x, br.y, activePaint)
            }
            return
        }

        if (photoCrops.isEmpty() || currentOrder.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        dimPaint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setScale(dimFactor, dimFactor, dimFactor, 1f) }
        )

        canvas.drawColor(android.graphics.Color.BLACK)

        if (isHorizontalLayout) {
            // Horizontal row: draw non-active photos in slot order
            val screenTop = transformCoordBitmapToTouch(0f, 0f).y
            val screenBottom = transformCoordBitmapToTouch(0f, photoBounds[0].height()).y

            for (i in currentOrder.indices) {
                val dataIdx = currentOrder[i]
                if (dataIdx == activePhotoIndex) continue

                var slotX = 0f
                for (j in 0 until i) {
                    slotX += photoBounds[currentOrder[j]].width()
                }
                val slotWidth = photoBounds[dataIdx].width()

                val animOffset = slotAnimOffsets[dataIdx] ?: 0f
                val screenLeft = transformCoordBitmapToTouch(slotX, 0f).x + animOffset
                val screenRight = transformCoordBitmapToTouch(slotX + slotWidth, 0f).x + animOffset

                canvas.drawBitmap(
                    photoCrops[dataIdx],
                    null,
                    android.graphics.Rect(
                        screenLeft.toInt(), screenTop.toInt(),
                        screenRight.toInt(), screenBottom.toInt()
                    ),
                    dimPaint
                )
                canvas.drawRect(screenLeft, screenTop, screenRight, screenBottom, inactivePaint)
            }
        } else {
            // Grid layout: draw non-active photos at their CELL positions (photoBounds[cellIdx])
            for (cellIdx in currentOrder.indices) {
                val dataIdx = currentOrder[cellIdx]
                if (dataIdx == activePhotoIndex) continue

                val bounds = photoBounds[cellIdx]
                val tl = transformCoordBitmapToTouch(bounds.left, bounds.top)
                val br = transformCoordBitmapToTouch(bounds.right, bounds.bottom)
                val animOffX = slotAnimOffsets[dataIdx] ?: 0f
                val animOffY = slotAnimOffsetsY[dataIdx] ?: 0f

                canvas.drawBitmap(
                    photoCrops[dataIdx],
                    null,
                    android.graphics.Rect(
                        (tl.x + animOffX).toInt(), (tl.y + animOffY).toInt(),
                        (br.x + animOffX).toInt(), (br.y + animOffY).toInt()
                    ),
                    dimPaint
                )
                canvas.drawRect(
                    tl.x + animOffX, tl.y + animOffY,
                    br.x + animOffX, br.y + animOffY,
                    inactivePaint
                )
            }
        }

        // Active photo follows finger / snapped to target
        val activeOrigBounds = photoBounds[activePhotoIndex]
        val aLeft = transformCoordBitmapToTouch(activeOrigBounds.left, activeOrigBounds.top)
        val aRight = transformCoordBitmapToTouch(activeOrigBounds.right, activeOrigBounds.bottom)

        val centerX = (aLeft.x + aRight.x) / 2f + dragOffsetPx
        val centerY = (aLeft.y + aRight.y) / 2f + dragOffsetY

        // Bridge active + full bitmap: draw center-cropped into target cell (matches final stitched output)
        if (bridgeActive && bridgeFullBitmap != null && bridgeTargetCellIdx in photoBounds.indices) {
            val bmp = bridgeFullBitmap!!
            val targetBounds = photoBounds[bridgeTargetCellIdx]
            val tl = transformCoordBitmapToTouch(targetBounds.left, targetBounds.top)
            val br = transformCoordBitmapToTouch(targetBounds.right, targetBounds.bottom)
            val cellW = br.x - tl.x
            val cellH = br.y - tl.y
            val fillScale = maxOf(cellW / bmp.width, cellH / bmp.height)
            val scaledW = bmp.width * fillScale
            val scaledH = bmp.height * fillScale
            val photoLeft = tl.x + (cellW - scaledW) / 2f
            val photoTop = tl.y + (cellH - scaledH) / 2f

            canvas.save()
            canvas.clipRect(tl.x, tl.y, br.x, br.y)
            canvas.drawRect(tl.x, tl.y, br.x, br.y, blackPaint)
            canvas.drawBitmap(
                bmp, null,
                android.graphics.RectF(photoLeft, photoTop, photoLeft + scaledW, photoTop + scaledH),
                photoPaint
            )
            canvas.restore()
            canvas.drawRect(tl.x, tl.y, br.x, br.y, activePaint)
        } else {
            val halfW = (aRight.x - aLeft.x) / 2f * activeScale
            val halfH = (aRight.y - aLeft.y) / 2f * activeScale
            canvas.drawBitmap(
                photoCrops[activePhotoIndex],
                null,
                android.graphics.Rect(
                    (centerX - halfW).toInt(), (centerY - halfH).toInt(),
                    (centerX + halfW).toInt(), (centerY + halfH).toInt()
                ),
                photoPaint
            )
            canvas.drawRect(
                centerX - halfW, centerY - halfH,
                centerX + halfW, centerY + halfH,
                activePaint
            )
        }
    }
}
