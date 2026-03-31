package com.bcd.technotes.data.model

import android.graphics.RectF

data class ImageEditState(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null
)

enum class EditTool { NONE, CROP }
