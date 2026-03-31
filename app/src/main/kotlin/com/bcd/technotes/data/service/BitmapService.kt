package com.bcd.technotes.data.service

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.bcd.technotes.core.model.CollageLayout
import com.bcd.technotes.data.model.CollageResult
import com.bcd.technotes.data.model.ImageEditState
import com.bcd.technotes.data.model.PhotoTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

class BitmapService @Inject constructor(
    private val application: Application
) {

    suspend fun decodeBitmap(uri: Uri): Bitmap? {
        val displayHeight = application.resources.displayMetrics.heightPixels
        val targetMaxHeight = displayHeight * 2
        return withContext(Dispatchers.IO) {
            try {
                application.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    opts
                }?.let { opts ->
                    val sampleSize = calculateInSampleSize(opts.outHeight, targetMaxHeight)
                    application.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(
                            stream, null,
                            BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        )
                    }
                }
            } catch (_: Exception) { null }
        }
    }

    fun cropBitmap(bitmap: Bitmap, normalizedRect: RectF): Bitmap {
        val x = (normalizedRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (normalizedRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((normalizedRect.right - normalizedRect.left) * bitmap.width).toInt()
            .coerceAtMost(bitmap.width - x).coerceAtLeast(1)
        val h = ((normalizedRect.bottom - normalizedRect.top) * bitmap.height).toInt()
            .coerceAtMost(bitmap.height - y).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix()
        if (horizontal) matrix.postScale(-1f, 1f) else matrix.postScale(1f, -1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyTransform(bitmap: Bitmap, transform: PhotoTransform): Bitmap {
        if (!transform.flipH && !transform.flipV && transform.rotation == 0 && transform.cropRect == null) return bitmap

        var current = bitmap

        if (transform.flipH || transform.flipV || transform.rotation != 0) {
            val matrix = Matrix()
            if (transform.flipH) matrix.postScale(-1f, 1f)
            if (transform.flipV) matrix.postScale(1f, -1f)
            matrix.postRotate(transform.rotation.toFloat())
            current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        }

        if (transform.cropRect != null) {
            val cropped = cropBitmap(current, transform.cropRect)
            if (current !== bitmap) current.recycle()
            current = cropped
        }

        return current
    }

    suspend fun stitchCollage(
        bitmaps: List<Bitmap>,
        transforms: List<PhotoTransform>,
        layout: CollageLayout,
        canvasWidth: Int
    ): CollageResult? = withContext(Dispatchers.IO) {
        if (bitmaps.size < 2) return@withContext null

        val transformed = bitmaps.mapIndexed { i, bmp ->
            applyTransform(bmp, transforms.getOrElse(i) { PhotoTransform() })
        }

        if (layout.aspectRatio == 0f) {
            stitchHorizontalRow(bitmaps, transformed)
        } else {
            stitchGrid(bitmaps, transformed, transforms, layout, canvasWidth)
        }
    }

    private fun stitchHorizontalRow(
        originals: List<Bitmap>,
        transformed: List<Bitmap>
    ): CollageResult {
        val uniformHeight = transformed.minOf { it.height }
        val scaledWidths = transformed.map { bmp ->
            (bmp.width * (uniformHeight.toFloat() / bmp.height)).toInt()
        }
        val totalWidth = scaledWidths.sum()

        val result = Bitmap.createBitmap(totalWidth, uniformHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        var x = 0f
        transformed.forEachIndexed { i, bmp ->
            val scale = uniformHeight.toFloat() / bmp.height
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(x, 0f)
            }
            canvas.drawBitmap(bmp, matrix, null)
            x += scaledWidths[i]
        }

        transformed.forEachIndexed { i, tbmp ->
            if (tbmp !== originals[i]) tbmp.recycle()
        }

        val bounds = mutableListOf<RectF>()
        var bx = 0f
        scaledWidths.forEach { w ->
            bounds.add(RectF(bx, 0f, bx + w, uniformHeight.toFloat()))
            bx += w
        }
        val fullVisible = bounds.map { RectF(0f, 0f, 1f, 1f) }
        return CollageResult(result, bounds, fullVisible)
    }

    private fun stitchGrid(
        originals: List<Bitmap>,
        transformed: List<Bitmap>,
        transforms: List<PhotoTransform>,
        layout: CollageLayout,
        canvasWidth: Int
    ): CollageResult {
        val cw = canvasWidth
        val ch = (cw / layout.aspectRatio).toInt()
        val result = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val bounds = mutableListOf<RectF>()
        val visibleRects = mutableListOf<RectF>()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        layout.cells.forEachIndexed { i, cell ->
            val bmp = transformed.getOrNull(i) ?: return@forEachIndexed
            val transform = transforms.getOrElse(i) { PhotoTransform() }

            val cellLeft = (cell.left * cw).toInt()
            val cellTop = (cell.top * ch).toInt()
            val cellRight = (cell.right * cw).toInt()
            val cellBottom = (cell.bottom * ch).toInt()
            val cellW = cellRight - cellLeft
            val cellH = cellBottom - cellTop

            val scale = max(cellW.toFloat() / bmp.width, cellH.toFloat() / bmp.height)
            val scaledW = (bmp.width * scale).toInt()
            val scaledH = (bmp.height * scale).toInt()
            val overflowX = scaledW - cellW
            val overflowY = scaledH - cellH
            val offsetX = (transform.panX * overflowX).toInt()
            val offsetY = (transform.panY * overflowY).toInt()

            val srcLeft = (offsetX / scale).toInt().coerceIn(0, bmp.width - 1)
            val srcTop = (offsetY / scale).toInt().coerceIn(0, bmp.height - 1)
            val srcRight = (srcLeft + cellW / scale).toInt().coerceAtMost(bmp.width)
            val srcBottom = (srcTop + cellH / scale).toInt().coerceAtMost(bmp.height)

            canvas.drawBitmap(
                bmp,
                Rect(srcLeft, srcTop, srcRight, srcBottom),
                Rect(cellLeft, cellTop, cellRight, cellBottom),
                paint
            )
            bounds.add(RectF(cellLeft.toFloat(), cellTop.toFloat(), cellRight.toFloat(), cellBottom.toFloat()))

            if (overflowX == 0 && overflowY == 0) {
                visibleRects.add(RectF(0f, 0f, 1f, 1f))
            } else {
                visibleRects.add(RectF(
                    offsetX.toFloat() / scaledW,
                    offsetY.toFloat() / scaledH,
                    (offsetX + cellW).toFloat() / scaledW,
                    (offsetY + cellH).toFloat() / scaledH
                ))
            }
        }

        transformed.forEachIndexed { i, tbmp ->
            if (tbmp !== originals[i]) tbmp.recycle()
        }

        return CollageResult(result, bounds, visibleRects)
    }

    fun applyEditState(source: Bitmap, state: ImageEditState): Bitmap {
        var current = source

        if (state.cropRect != null) {
            current = cropBitmap(current, state.cropRect)
        }

        if (state.flipH || state.flipV || state.rotation != 0) {
            val matrix = Matrix()
            if (state.flipH) matrix.postScale(1f, -1f, current.width / 2f, current.height / 2f)
            if (state.flipV) matrix.postScale(-1f, 1f, current.width / 2f, current.height / 2f)
            if (state.rotation != 0) matrix.postRotate(state.rotation.toFloat(), current.width / 2f, current.height / 2f)
            current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        }

        return current
    }

    private fun calculateInSampleSize(actualHeight: Int, targetHeight: Int): Int {
        var sampleSize = 1
        while (actualHeight / (sampleSize * 2) >= targetHeight) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
