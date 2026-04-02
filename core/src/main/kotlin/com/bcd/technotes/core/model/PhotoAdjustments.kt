package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Float = 0f
) {
    val isDefault: Boolean
        get() = this == DEFAULT

    companion object {
        val DEFAULT = PhotoAdjustments()
    }
}

@Serializable
enum class AdjustmentType(val label: String, val min: Float, val max: Float) {
    BRIGHTNESS("Brightness", -100f, 100f),
    CONTRAST("Contrast", -100f, 100f),
    SATURATION("Saturation", -100f, 100f),
    WARMTH("Warmth", -100f, 100f),
    TINT("Tint", -100f, 100f),
    HIGHLIGHTS("Highlights", -100f, 100f),
    SHADOWS("Shadows", -100f, 100f),
    SHARPNESS("Sharpness", 0f, 100f),
    VIGNETTE("Vignette", 0f, 100f);
}

fun PhotoAdjustments.getValue(type: AdjustmentType): Float = when (type) {
    AdjustmentType.BRIGHTNESS -> brightness
    AdjustmentType.CONTRAST -> contrast
    AdjustmentType.SATURATION -> saturation
    AdjustmentType.WARMTH -> warmth
    AdjustmentType.TINT -> tint
    AdjustmentType.HIGHLIGHTS -> highlights
    AdjustmentType.SHADOWS -> shadows
    AdjustmentType.SHARPNESS -> sharpness
    AdjustmentType.VIGNETTE -> vignette
}

fun PhotoAdjustments.withValue(type: AdjustmentType, value: Float): PhotoAdjustments = when (type) {
    AdjustmentType.BRIGHTNESS -> copy(brightness = value)
    AdjustmentType.CONTRAST -> copy(contrast = value)
    AdjustmentType.SATURATION -> copy(saturation = value)
    AdjustmentType.WARMTH -> copy(warmth = value)
    AdjustmentType.TINT -> copy(tint = value)
    AdjustmentType.HIGHLIGHTS -> copy(highlights = value)
    AdjustmentType.SHADOWS -> copy(shadows = value)
    AdjustmentType.SHARPNESS -> copy(sharpness = value)
    AdjustmentType.VIGNETTE -> copy(vignette = value)
}
