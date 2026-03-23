package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Layer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val order: Int,
    val type: LayerType = LayerType.ANNOTATION
)

@Serializable
enum class LayerType {
    PHOTO,
    ANNOTATION,
    MEASUREMENT,
    SYMBOL,
    TEXT
}
