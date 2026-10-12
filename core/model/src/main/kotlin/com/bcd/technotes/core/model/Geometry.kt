package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
data class PointF(val x: Float, val y: Float) {

    fun distanceTo(other: PointF): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    operator fun plus(other: PointF): PointF = PointF(x + other.x, y + other.y)

    operator fun minus(other: PointF): PointF = PointF(x - other.x, y - other.y)

    operator fun times(scalar: Float): PointF = PointF(x * scalar, y * scalar)
}

@Serializable
data class SizeF(val width: Float, val height: Float)

@Serializable
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val center: PointF get() = PointF((left + right) / 2f, (top + bottom) / 2f)

    fun contains(point: PointF): Boolean =
        point.x in left..right && point.y in top..bottom

    fun intersects(other: RectF): Boolean =
        left < other.right && right > other.left &&
            top < other.bottom && bottom > other.top

    companion object {
        fun fromCenterAndSize(center: PointF, size: SizeF): RectF = RectF(
            left = center.x - size.width / 2f,
            top = center.y - size.height / 2f,
            right = center.x + size.width / 2f,
            bottom = center.y + size.height / 2f
        )

        fun fromPoints(p1: PointF, p2: PointF): RectF = RectF(
            left = minOf(p1.x, p2.x),
            top = minOf(p1.y, p2.y),
            right = maxOf(p1.x, p2.x),
            bottom = maxOf(p1.y, p2.y)
        )
    }
}
