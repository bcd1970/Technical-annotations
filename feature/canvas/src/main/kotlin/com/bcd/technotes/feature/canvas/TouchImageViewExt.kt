package com.bcd.technotes.feature.canvas

import com.ortiz.touchview.TouchImageView

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
