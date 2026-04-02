package com.bcd.technotes.data.model

import android.graphics.RectF
import com.bcd.technotes.core.model.PhotoAdjustments

data class ImageEditState(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null,
    val adjustments: PhotoAdjustments = PhotoAdjustments()
)

enum class EditTool { NONE, CROP, ADJUST }
