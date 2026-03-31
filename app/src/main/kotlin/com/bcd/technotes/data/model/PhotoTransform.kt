package com.bcd.technotes.data.model

import android.graphics.Bitmap
import android.graphics.RectF

data class PhotoTransform(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null,
    val panX: Float = 0.5f,
    val panY: Float = 0.5f
)

data class CollageResult(
    val bitmap: Bitmap,
    val photoBounds: List<RectF>,
    val photoVisibleRects: List<RectF>
)
