package com.bcd.technotes.sandbox.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import com.ortiz.touchview.TouchImageView

class WholeImageEditView(context: Context) : TouchImageView(context) {

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
        val bm = drawable ?: return
        cropRect = RectF(0f, 0f, bm.intrinsicWidth.toFloat(), bm.intrinsicHeight.toFloat())
    }

    // --- Paints ---

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
        val bm = drawable ?: return
        val imgW = bm.intrinsicWidth.toFloat()
        val imgH = bm.intrinsicHeight.toFloat()

        when (activeCropHandle) {
            0 -> {
                cropRect.left = bp.x.coerceIn(0f, cropRect.right - minCropSizePx)
                cropRect.top = bp.y.coerceIn(0f, cropRect.bottom - minCropSizePx)
            }
            1 -> cropRect.top = bp.y.coerceIn(0f, cropRect.bottom - minCropSizePx)
            2 -> {
                cropRect.right = bp.x.coerceIn(cropRect.left + minCropSizePx, imgW)
                cropRect.top = bp.y.coerceIn(0f, cropRect.bottom - minCropSizePx)
            }
            3 -> cropRect.left = bp.x.coerceIn(0f, cropRect.right - minCropSizePx)
            4 -> cropRect.right = bp.x.coerceIn(cropRect.left + minCropSizePx, imgW)
            5 -> {
                cropRect.left = bp.x.coerceIn(0f, cropRect.right - minCropSizePx)
                cropRect.bottom = bp.y.coerceIn(cropRect.top + minCropSizePx, imgH)
            }
            6 -> cropRect.bottom = bp.y.coerceIn(cropRect.top + minCropSizePx, imgH)
            7 -> {
                cropRect.right = bp.x.coerceIn(cropRect.left + minCropSizePx, imgW)
                cropRect.bottom = bp.y.coerceIn(cropRect.top + minCropSizePx, imgH)
            }
            8 -> {
                val w = cropRect.width()
                val h = cropRect.height()
                val newLeft = (bp.x - cropMoveOffsetX).coerceIn(0f, imgW - w)
                val newTop = (bp.y - cropMoveOffsetY).coerceIn(0f, imgH - h)
                cropRect.set(newLeft, newTop, newLeft + w, newTop + h)
            }
        }
        invalidate()
    }

    private fun drawCropOverlay(canvas: Canvas) {
        val bm = drawable ?: return
        val imgW = bm.intrinsicWidth.toFloat()
        val imgH = bm.intrinsicHeight.toFloat()

        val imgTL = transformCoordBitmapToTouch(0f, 0f)
        val imgBR = transformCoordBitmapToTouch(imgW, imgH)
        val cropTL = transformCoordBitmapToTouch(cropRect.left, cropRect.top)
        val cropBR = transformCoordBitmapToTouch(cropRect.right, cropRect.bottom)

        // Dim outside crop rect within image
        canvas.drawRect(imgTL.x, imgTL.y, imgBR.x, cropTL.y, cropDimPaint)
        canvas.drawRect(imgTL.x, cropBR.y, imgBR.x, imgBR.y, cropDimPaint)
        canvas.drawRect(imgTL.x, cropTL.y, cropTL.x, cropBR.y, cropDimPaint)
        canvas.drawRect(cropBR.x, cropTL.y, imgBR.x, cropBR.y, cropDimPaint)

        // Crop border
        canvas.drawRect(cropTL.x, cropTL.y, cropBR.x, cropBR.y, cropBorderPaint)

        // Handles
        for (handle in getCropHandlePositions()) {
            val screen = transformCoordBitmapToTouch(handle.x, handle.y)
            canvas.drawCircle(screen.x, screen.y, handleRadiusPx, handleFillPaint)
            canvas.drawCircle(screen.x, screen.y, handleRadiusPx, handleStrokePaint)
        }
    }

    // --- Touch dispatch ---

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (cropMode) {
            if (handleCropTouch(event)) return true
            return super.dispatchTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cropMode) {
            drawCropOverlay(canvas)
        }
    }
}
