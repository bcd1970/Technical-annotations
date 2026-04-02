package com.bcd.technotes.core.processing

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuidedFilterTest {

    @Test
    fun `constant image returns itself`() {
        val w = 20
        val h = 20
        val input = FloatArray(w * h) { 0.5f }
        val base = GuidedFilter(w, h).filter(input, radius = 3, eps = 0.001f)
        for (i in base.indices) {
            assertEquals(0.5f, base[i], 1e-4f, "Pixel $i deviated: ${base[i]}")
        }
    }

    @Test
    fun `step edge is preserved with small eps`() {
        val w = 100
        val h = 50
        val input = FloatArray(w * h) { i ->
            if (i % w < w / 2) 0.2f else 0.8f
        }
        val base = GuidedFilter(w, h).filter(input, radius = 5, eps = 0.0001f)

        // Far from edge: base should match input closely
        val leftInterior = 10 * w + 10 // well inside left half
        val rightInterior = 10 * w + 90 // well inside right half
        assertEquals(0.2f, base[leftInterior], 0.02f, "Left interior: ${base[leftInterior]}")
        assertEquals(0.8f, base[rightInterior], 0.02f, "Right interior: ${base[rightInterior]}")
    }

    @Test
    fun `smooth gradient passes through unchanged in interior`() {
        val w = 100
        val h = 1
        val radius = 5
        val input = FloatArray(w) { it.toFloat() / (w - 1) }
        val base = GuidedFilter(w, h).filter(input, radius = radius, eps = 0.001f)

        // Skip boundary pixels within radius of edges (SAT boundary effects)
        for (i in (radius + 1) until (w - radius - 1)) {
            assertEquals(input[i], base[i], 0.02f, "Pixel $i: input=${input[i]}, base=${base[i]}")
        }
    }

    @Test
    fun `texture lands in detail layer`() {
        val w = 100
        val h = 100
        // Slow gradient + high-frequency noise
        val input = FloatArray(w * h) { i ->
            val x = i % w
            val gradient = x.toFloat() / (w - 1) * 0.5f + 0.25f
            val noise = if ((i / w + x) % 2 == 0) 0.05f else -0.05f
            (gradient + noise).coerceIn(0f, 1f)
        }
        val base = GuidedFilter(w, h).filter(input, radius = 10, eps = 0.01f)

        // Detail = input - base should capture the noise
        var totalAbsDetail = 0f
        for (i in base.indices) {
            totalAbsDetail += abs(input[i] - base[i])
        }
        val meanAbsDetail = totalAbsDetail / base.size
        assertTrue(meanAbsDetail > 0.01f, "Detail too small: $meanAbsDetail — noise not captured")
    }

    @Test
    fun `1x1 image returns input value`() {
        val input = floatArrayOf(0.42f)
        val base = GuidedFilter(1, 1).filter(input, radius = 5, eps = 0.001f)
        assertEquals(0.42f, base[0], 1e-5f)
    }

    @Test
    fun `3x3 image with large radius does not crash`() {
        val w = 3
        val h = 3
        val input = FloatArray(w * h) { it.toFloat() / 8f }
        val base = GuidedFilter(w, h).filter(input, radius = 10, eps = 0.001f)
        assertEquals(w * h, base.size)
        for (v in base) {
            assertTrue(v.isFinite(), "Non-finite value in output: $v")
        }
    }

    @Test
    fun `smaller eps preserves edges more`() {
        val w = 100
        val h = 50
        val input = FloatArray(w * h) { i ->
            if (i % w < w / 2) 0.1f else 0.9f
        }
        val baseSharp = GuidedFilter(w, h).filter(input, radius = 5, eps = 0.0001f)
        val baseSoft = GuidedFilter(w, h).filter(input, radius = 5, eps = 0.1f)

        // Near the edge, sharp base should be closer to the input step
        val edgePixel = 25 * w + (w / 2) // right at the transition
        val sharpDiff = abs(baseSharp[edgePixel] - input[edgePixel])
        val softDiff = abs(baseSoft[edgePixel] - input[edgePixel])
        assertTrue(sharpDiff <= softDiff, "Sharp eps should preserve edge better: sharp=$sharpDiff, soft=$softDiff")
    }
}
