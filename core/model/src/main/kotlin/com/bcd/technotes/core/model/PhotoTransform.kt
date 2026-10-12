package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoTransform(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null,
    val panX: Float = 0.5f,
    val panY: Float = 0.5f
)
