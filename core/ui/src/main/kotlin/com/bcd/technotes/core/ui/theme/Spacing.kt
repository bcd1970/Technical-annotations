package com.bcd.technotes.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
)

@Immutable
data class Radius(
    val sm: Dp = 6.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 14.dp,
    val xl: Dp = 16.dp,
    val pill: Dp = 999.dp,
)

val TechNotesSpacing = Spacing()
val TechNotesRadius = Radius()

val LocalSpacing = staticCompositionLocalOf { TechNotesSpacing }
val LocalRadius = staticCompositionLocalOf { TechNotesRadius }

val TechNotesShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)
