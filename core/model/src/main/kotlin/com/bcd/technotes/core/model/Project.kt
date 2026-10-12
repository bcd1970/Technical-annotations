package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val version: Int = CURRENT_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val backgroundPhotos: List<PhotoReference> = emptyList(),
    val layers: List<Layer> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val scaleCalibration: ScaleCalibration? = null
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class PhotoReference(
    val id: String,
    val uri: String,
    val rect: RectF,
    val opacity: Float = 1f,
    val order: Int = 0
)

@Serializable
data class ScaleCalibration(
    val pixelDistance: Float,
    val realDistance: Float,
    val unit: MeasurementUnit
) {
    val pixelsPerUnit: Float
        get() = if (realDistance != 0f) pixelDistance / realDistance else 0f
}
