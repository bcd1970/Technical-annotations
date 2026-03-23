package com.bcd.technotes.core

import com.bcd.technotes.core.model.PointF
import com.bcd.technotes.core.model.RectF
import com.bcd.technotes.core.model.SizeF
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometryTest {

    @Test
    fun `PointF distanceTo computes correct euclidean distance`() {
        val a = PointF(0f, 0f)
        val b = PointF(3f, 4f)
        assertEquals(5f, a.distanceTo(b))
    }

    @Test
    fun `PointF arithmetic operators work correctly`() {
        val a = PointF(1f, 2f)
        val b = PointF(3f, 4f)

        assertEquals(PointF(4f, 6f), a + b)
        assertEquals(PointF(-2f, -2f), a - b)
        assertEquals(PointF(2f, 4f), a * 2f)
    }

    @Test
    fun `RectF properties compute correctly`() {
        val rect = RectF(10f, 20f, 110f, 70f)
        assertEquals(100f, rect.width)
        assertEquals(50f, rect.height)
        assertEquals(PointF(60f, 45f), rect.center)
    }

    @Test
    fun `RectF contains point`() {
        val rect = RectF(0f, 0f, 100f, 100f)
        assertTrue(rect.contains(PointF(50f, 50f)))
        assertTrue(rect.contains(PointF(0f, 0f)))
        assertTrue(rect.contains(PointF(100f, 100f)))
        assertFalse(rect.contains(PointF(-1f, 50f)))
        assertFalse(rect.contains(PointF(50f, 101f)))
    }

    @Test
    fun `RectF intersects another rect`() {
        val a = RectF(0f, 0f, 100f, 100f)
        val b = RectF(50f, 50f, 150f, 150f)
        val c = RectF(200f, 200f, 300f, 300f)

        assertTrue(a.intersects(b))
        assertTrue(b.intersects(a))
        assertFalse(a.intersects(c))
    }

    @Test
    fun `RectF fromCenterAndSize creates correct rect`() {
        val rect = RectF.fromCenterAndSize(PointF(50f, 50f), SizeF(100f, 60f))
        assertEquals(0f, rect.left)
        assertEquals(20f, rect.top)
        assertEquals(100f, rect.right)
        assertEquals(80f, rect.bottom)
    }

    @Test
    fun `RectF fromPoints handles any point order`() {
        val rect1 = RectF.fromPoints(PointF(0f, 0f), PointF(100f, 100f))
        val rect2 = RectF.fromPoints(PointF(100f, 100f), PointF(0f, 0f))
        assertEquals(rect1, rect2)
        assertEquals(0f, rect1.left)
        assertEquals(0f, rect1.top)
        assertEquals(100f, rect1.right)
        assertEquals(100f, rect1.bottom)
    }
}
