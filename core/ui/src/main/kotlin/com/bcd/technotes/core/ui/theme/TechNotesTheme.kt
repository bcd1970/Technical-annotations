package com.bcd.technotes.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun TechNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalSpacing provides TechNotesSpacing,
        LocalRadius provides TechNotesRadius,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TechNotesTypography,
            shapes = TechNotesShapes,
            content = content
        )
    }
}

object TechNotesTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
    val radius: Radius
        @Composable get() = LocalRadius.current
}
