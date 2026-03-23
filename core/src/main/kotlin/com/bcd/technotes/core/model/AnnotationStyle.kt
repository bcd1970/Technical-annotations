package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AnnotationStyle(
    val strokeColor: Long = 0xFF000000,
    val fillColor: Long? = null,
    val strokeWidth: Float = 2f,
    val strokeCap: StrokeCap = StrokeCap.ROUND,
    val strokeJoin: StrokeJoin = StrokeJoin.ROUND,
    val dashPattern: List<Float>? = null,
    val opacity: Float = 1f
) {
    companion object {
        val DEFAULT = AnnotationStyle()
    }
}

@Serializable
enum class StrokeCap { BUTT, ROUND, SQUARE }

@Serializable
enum class StrokeJoin { MITER, ROUND, BEVEL }

@Serializable
data class AnnotationTransform(
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f
) {
    val isIdentity: Boolean
        get() = rotation == 0f && scaleX == 1f && scaleY == 1f &&
            translateX == 0f && translateY == 0f

    companion object {
        val IDENTITY = AnnotationTransform()
    }
}
