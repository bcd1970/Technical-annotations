package com.bcd.technotes.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.ortiz.touchview.TouchImageView
import kotlin.math.abs

class EditableTouchImageView(context: Context) : TouchImageView(context) {

    var editMode = false
        set(value) { field = value; invalidate() }

    var activePhotoIndex = -1
        set(value) { field = value; invalidate() }

    var photoBounds: List<RectF> = emptyList()

    var onReorderComplete: ((newOrder: List<Int>) -> Unit)? = null

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

    private val dimPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.6f, 0.6f, 0.6f, 1f) })
    }

    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Reorder drag state
    private var isReorderDragging = false
    private var isDragCandidate = false
    private var dragCancelled = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var dragOffsetPx = 0f
    private var photoCrops: List<Bitmap> = emptyList()
    private var currentOrder: MutableList<Int> = mutableListOf()
    private var activeDisplayIndex = -1
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    // Screen positions of each photo slot (computed once at drag start)
    private var slotCenters: List<Float> = emptyList()

    fun photoIndexAt(bitmapX: Float): Int =
        photoBounds.indexOfFirst { bitmapX >= it.left && bitmapX <= it.right }

    private fun imageScale(): Float {
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        return values[android.graphics.Matrix.MSCALE_X]
    }

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
        isReorderDragging = true

        slotCenters = photoBounds.map { rect ->
            val left = transformCoordBitmapToTouch(rect.left, 0f).x
            val right = transformCoordBitmapToTouch(rect.right, 0f).x
            (left + right) / 2f
        }

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    private fun updateReorderDrag(screenDragX: Float) {
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
                currentOrder[idx] = currentOrder[idx + 1].also { currentOrder[idx + 1] = currentOrder[idx] }
                activeDisplayIndex = idx + 1
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        val idx2 = activeDisplayIndex
        if (idx2 > 0) {
            val prevSlotDataIdx = currentOrder[idx2 - 1]
            val prevSlotCenter = slotCenters[prevSlotDataIdx]
            if (activeCenterNow < prevSlotCenter) {
                currentOrder[idx2] = currentOrder[idx2 - 1].also { currentOrder[idx2 - 1] = currentOrder[idx2] }
                activeDisplayIndex = idx2 - 1
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        invalidate()
    }

    private fun finishReorderDrag() {
        isReorderDragging = false
        if (currentOrder != photoBounds.indices.toList()) {
            onReorderComplete?.invoke(currentOrder.toList())
        }
        photoCrops = emptyList()
        currentOrder.clear()
        activeDisplayIndex = -1
        slotCenters = emptyList()
        dragOffsetPx = 0f
        invalidate()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
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
                if (isReorderDragging) {
                    isReorderDragging = false
                    photoCrops = emptyList()
                    currentOrder.clear()
                    invalidate()
                }
                isDragCandidate = false
                dragCancelled = true
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragCancelled) return super.dispatchTouchEvent(event)

                if (!isReorderDragging && isDragCandidate) {
                    val dx = abs(event.x - touchDownX)
                    val dy = abs(event.y - touchDownY)
                    if (dx > touchSlop && dx > dy * 1.5f) {
                        isDragCandidate = false
                        val bitmapPoint = transformCoordTouchToBitmap(touchDownX, touchDownY, true)
                        val tappedIndex = photoIndexAt(bitmapPoint.x)
                        if (tappedIndex < 0) return super.dispatchTouchEvent(event)
                        activePhotoIndex = tappedIndex
                        startReorderDrag()
                        touchDownX = event.x
                        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                    } else if (dy > touchSlop) {
                        isDragCandidate = false
                        dragCancelled = true
                        return super.dispatchTouchEvent(event)
                    } else {
                        return super.dispatchTouchEvent(event)
                    }
                }

                if (isReorderDragging) {
                    updateReorderDrag(event.x - touchDownX)
                    return true
                }

                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                if (isReorderDragging) {
                    finishReorderDrag()
                    return true
                }
                isDragCandidate = false
                dragCancelled = false
                return super.dispatchTouchEvent(event)
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isReorderDragging) {
                    isReorderDragging = false
                    photoCrops = emptyList()
                    currentOrder.clear()
                    invalidate()
                }
                isDragCandidate = false
                dragCancelled = false
                return super.dispatchTouchEvent(event)
            }
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        if (!isReorderDragging) {
            super.onDraw(canvas)
            if (!editMode || photoBounds.isEmpty()) return
            photoBounds.forEachIndexed { index, bound ->
                val topLeft = transformCoordBitmapToTouch(bound.left, bound.top)
                val bottomRight = transformCoordBitmapToTouch(bound.right, bound.bottom)
                val paint = if (index == activePhotoIndex) activePaint else inactivePaint
                canvas.drawRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, paint)
            }
            return
        }

        if (photoCrops.isEmpty() || currentOrder.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        canvas.drawColor(android.graphics.Color.BLACK)

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

            val screenLeft = transformCoordBitmapToTouch(slotX, 0f).x
            val screenRight = transformCoordBitmapToTouch(slotX + slotWidth, 0f).x

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

        val activeOrigBounds = photoBounds[activePhotoIndex]
        val aLeft = transformCoordBitmapToTouch(activeOrigBounds.left, activeOrigBounds.top)
        val aRight = transformCoordBitmapToTouch(activeOrigBounds.right, activeOrigBounds.bottom)

        canvas.drawBitmap(
            photoCrops[activePhotoIndex],
            null,
            android.graphics.Rect(
                (aLeft.x + dragOffsetPx).toInt(), aLeft.y.toInt(),
                (aRight.x + dragOffsetPx).toInt(), aRight.y.toInt()
            ),
            photoPaint
        )
        canvas.drawRect(
            aLeft.x + dragOffsetPx, aLeft.y,
            aRight.x + dragOffsetPx, aRight.y,
            activePaint
        )
    }
}
