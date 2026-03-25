package com.bcd.technotes.ui.util

import com.ortiz.touchview.TouchImageView

/**
 * Sets doubleTapScale so double-tap toggles between fit (full image visible)
 * and fill (image fills viewport). Must be called inside post{} after image is loaded.
 */
fun TouchImageView.updateDoubleTapScale() {
    val d = drawable ?: return
    val imgW = d.intrinsicWidth.toFloat()
    val imgH = d.intrinsicHeight.toFloat()
    val vW = width.toFloat()
    val vH = height.toFloat()
    if (imgW > 0 && imgH > 0 && vW > 0 && vH > 0) {
        doubleTapScale = maxOf(vW / imgW, vH / imgH) / minOf(vW / imgW, vH / imgH)
    }
}
