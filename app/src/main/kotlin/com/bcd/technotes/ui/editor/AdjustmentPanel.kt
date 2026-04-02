package com.bcd.technotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcd.technotes.core.model.AdjustmentType
import com.bcd.technotes.core.model.PhotoAdjustments
import com.bcd.technotes.core.model.getValue
import com.bcd.technotes.core.model.withValue

@Composable
fun AdjustmentPanel(
    adjustments: PhotoAdjustments,
    onAdjustmentChange: (PhotoAdjustments) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    var selectedType by remember { mutableStateOf(AdjustmentType.BRIGHTNESS) }
    val currentValue = adjustments.getValue(selectedType)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close adjustments", tint = Color.White)
            }
            Text(
                text = "${selectedType.label}: ${currentValue.toInt()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onReset) {
                Text("Reset", color = Color(0xFF4FC3F7))
            }
        }

        Slider(
            value = currentValue,
            onValueChange = { newVal ->
                onAdjustmentChange(adjustments.withValue(selectedType, newVal))
            },
            valueRange = selectedType.min..selectedType.max,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF4FC3F7),
                inactiveTrackColor = Color(0x66FFFFFF)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (type in AdjustmentType.entries.filter { it != AdjustmentType.VIGNETTE }) {
                val value = adjustments.getValue(type)
                FilterChip(
                    selected = type == selectedType,
                    onClick = { selectedType = type },
                    label = {
                        Text(
                            text = if (value != 0f) "${type.label} ${value.toInt()}" else type.label,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4FC3F7),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0x33FFFFFF),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
