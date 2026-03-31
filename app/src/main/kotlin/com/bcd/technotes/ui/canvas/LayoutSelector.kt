package com.bcd.technotes.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bcd.technotes.core.model.CollageLayout

private val layoutPreviewColors = listOf(
    Color(0xFFB0BEC5),
    Color(0xFF78909C),
    Color(0xFF546E7A)
)

@Composable
fun LayoutSelector(
    layouts: List<CollageLayout>,
    activeLayoutId: String,
    onLayoutSelected: (CollageLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        layouts.forEach { layout ->
            val isActive = layout.id == activeLayoutId
            val borderColor = if (isActive) Color(0xFF4FC3F7) else Color.Transparent
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(Color(0xFF263238), RoundedCornerShape(6.dp))
                    .clickable { onLayoutSelected(layout) }
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
                        val pad = 4.dp.toPx()
                        val innerW = w - pad * 2
                        val innerH = h - pad * 2
                        layout.cells.forEachIndexed { i, cell ->
                            val gap = 1.dp.toPx()
                            drawRect(
                                color = layoutPreviewColors[i % layoutPreviewColors.size],
                                topLeft = Offset(pad + cell.left * innerW + gap, pad + cell.top * innerH + gap),
                                size = Size(
                                    (cell.right - cell.left) * innerW - gap * 2,
                                    (cell.bottom - cell.top) * innerH - gap * 2
                                )
                            )
                        }
                    }
            )
        }
    }
}
