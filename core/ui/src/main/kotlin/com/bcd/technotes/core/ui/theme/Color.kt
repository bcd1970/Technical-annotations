package com.bcd.technotes.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Gray090909 = Color(0xFF090909)
val Gray0e0e0e = Color(0xFF0e0e0e)
val Gray171717 = Color(0xFF171717)
val Gray1f1f1f = Color(0xFF1f1f1f)
val Gray282828 = Color(0xFF282828)
val Gray313131 = Color(0xFF313131)
val Gray4e4e4e = Color(0xFF4e4e4e)
val Gray727272 = Color(0xFF727272)
val Grayb0b0b0 = Color(0xFFb0b0b0)
val Graye0e0e0 = Color(0xFFe0e0e0)
val Grayf0f0f0 = Color(0xFFf0f0f0)
val Grayf8f8f8 = Color(0xFFf8f8f8)
val Graye4e4e4 = Color(0xFFe4e4e4)
val Grayeaeaea = Color(0xFFeaeaea)
val Graydddddd = Color(0xFFdddddd)
val Grayc4c4c4 = Color(0xFFc4c4c4)
val Gray999999 = Color(0xFF999999)
val Gray666666 = Color(0xFF666666)
val Gray222222 = Color(0xFF222222)
val Gray111111 = Color(0xFF111111)

val DarkColorScheme = darkColorScheme(
    primary = Graye0e0e0,
    onPrimary = Gray090909,
    primaryContainer = Color(0x12E0E0E0),
    background = Gray0e0e0e,
    onBackground = Grayb0b0b0,
    surface = Gray171717,
    onSurface = Gray727272,
    surfaceVariant = Gray1f1f1f,
    onSurfaceVariant = Gray4e4e4e,
    surfaceBright = Gray282828,
    outline = Gray313131,
    scrim = Color(0xE00E0E0E),
)

val LightColorScheme = lightColorScheme(
    primary = Gray111111,
    onPrimary = Grayf8f8f8,
    primaryContainer = Color(0x0D111111),
    background = Grayf0f0f0,
    onBackground = Gray222222,
    surface = Grayf8f8f8,
    onSurface = Gray666666,
    surfaceVariant = Grayeaeaea,
    onSurfaceVariant = Gray999999,
    surfaceBright = Graydddddd,
    outline = Grayc4c4c4,
    scrim = Color(0xE0F0F0F0),
)

@Immutable
data class ExtendedColors(
    val canvas: Color,
    val modeBrowse: Color,
    val modeAnnotate: Color,
    val modeEdit: Color,
)

val DarkExtendedColors = ExtendedColors(
    canvas = Gray090909,
    modeBrowse = Color(0xFF606060),
    modeAnnotate = Color(0xFFd0d0d0),
    modeEdit = Color(0xFF909090),
)

val LightExtendedColors = ExtendedColors(
    canvas = Graye4e4e4,
    modeBrowse = Color(0xFFaaaaaa),
    modeAnnotate = Color(0xFF333333),
    modeEdit = Color(0xFF777777),
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
