package com.bcd.technotes.core.image

import android.graphics.Bitmap
import android.graphics.RectF

data class CollageResult(
    val bitmap: Bitmap,
    val photoBounds: List<RectF>,
    val photoVisibleRects: List<RectF>
)
