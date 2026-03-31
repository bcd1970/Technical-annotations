package com.bcd.technotes.ui.canvas

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun CollageThumbBar(
    bitmap: Bitmap,
    viewportRect: RectF?,
    isZoomed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (isZoomed) 1f else 0f,
            label = "viewportAlpha"
        )
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Collage preview",
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .height(56.dp)
                .horizontalScroll(rememberScrollState())
                .drawWithContent {
                    drawContent()
                    if (indicatorAlpha > 0f && viewportRect != null) {
                        val left = viewportRect.left * size.width
                        val top = viewportRect.top * size.height
                        val right = viewportRect.right * size.width
                        val bottom = viewportRect.bottom * size.height
                        drawRect(
                            color = Color.White.copy(alpha = 0.25f * indicatorAlpha),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.85f * indicatorAlpha),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
        )
    }
}
