package com.bcd.technotes.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Annotation {
    abstract val id: String
    abstract val layerId: String
    abstract val style: AnnotationStyle
    abstract val transform: AnnotationTransform
    abstract val isLocked: Boolean
}

@Serializable
@SerialName("line")
data class LineAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val start: PointF,
    val end: PointF,
    val hasArrowStart: Boolean = false,
    val hasArrowEnd: Boolean = false
) : Annotation()

@Serializable
@SerialName("rectangle")
data class RectangleAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val topLeft: PointF,
    val size: SizeF,
    val cornerRadius: Float = 0f
) : Annotation()

@Serializable
@SerialName("circle")
data class CircleAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val center: PointF,
    val radius: Float
) : Annotation()

@Serializable
@SerialName("ellipse")
data class EllipseAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val center: PointF,
    val radiusX: Float,
    val radiusY: Float
) : Annotation()

@Serializable
@SerialName("polygon")
data class PolygonAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val points: List<PointF>,
    val isClosed: Boolean = true
) : Annotation()

@Serializable
@SerialName("freehand")
data class FreehandAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val points: List<PointF>,
    val smoothing: Float = 0.5f
) : Annotation()

@Serializable
@SerialName("text")
data class TextAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val position: PointF,
    val content: String,
    val fontSize: Float = 14f,
    val fontFamily: String = "default"
) : Annotation()

@Serializable
@SerialName("callout")
data class CalloutAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val anchorPoint: PointF,
    val textBoxRect: RectF,
    val content: String,
    val leaderStyle: LeaderStyle = LeaderStyle.STRAIGHT
) : Annotation()

@Serializable
@SerialName("numbered_marker")
data class NumberedMarkerAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val position: PointF,
    val number: Int,
    val markerStyle: MarkerStyle = MarkerStyle.CIRCLE
) : Annotation()

@Serializable
@SerialName("dimension_line")
data class DimensionLineAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val start: PointF,
    val end: PointF,
    val offset: Float = 20f,
    val displayValue: String = "",
    val unit: MeasurementUnit = MeasurementUnit.PIXELS
) : Annotation()

@Serializable
@SerialName("angle")
data class AngleAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val vertex: PointF,
    val armA: PointF,
    val armB: PointF,
    val displayValue: String = ""
) : Annotation()

@Serializable
@SerialName("area")
data class AreaAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val points: List<PointF>,
    val displayValue: String = "",
    val unit: MeasurementUnit = MeasurementUnit.PIXELS
) : Annotation()

@Serializable
@SerialName("symbol")
data class SymbolAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val position: PointF,
    val symbolId: String,
    val category: SymbolCategory = SymbolCategory.GENERAL,
    val size: SizeF = SizeF(48f, 48f)
) : Annotation()

@Serializable
@SerialName("image_overlay")
data class ImageOverlayAnnotation(
    override val id: String,
    override val layerId: String,
    override val style: AnnotationStyle = AnnotationStyle.DEFAULT,
    override val transform: AnnotationTransform = AnnotationTransform.IDENTITY,
    override val isLocked: Boolean = false,
    val imageUri: String,
    val rect: RectF,
    val overlayOpacity: Float = 1f
) : Annotation()

@Serializable
enum class LeaderStyle { STRAIGHT, ELBOW, CURVED }

@Serializable
enum class MarkerStyle { CIRCLE, SQUARE, DIAMOND }

@Serializable
enum class MeasurementUnit {
    PIXELS, MM, CM, M, INCHES, FEET;

    val abbreviation: String
        get() = when (this) {
            PIXELS -> "px"
            MM -> "mm"
            CM -> "cm"
            M -> "m"
            INCHES -> "in"
            FEET -> "ft"
        }
}

@Serializable
enum class SymbolCategory {
    ELECTRICAL, ARCHITECTURAL, MEDICAL, HVAC, PLUMBING, WARNING, GENERAL
}
