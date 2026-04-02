package com.bcd.technotes.core.processing

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorSpaceTest {

    @Test
    fun `srgb-linear round trip preserves values`() {
        val values = floatArrayOf(0f, 0.01f, 0.04045f, 0.1f, 0.25f, 0.5f, 0.75f, 1f)
        for (v in values) {
            val roundTrip = linearToSrgb(srgbToLinear(v))
            assertEquals(v, roundTrip, 1e-5f, "Round-trip failed for $v")
        }
    }

    @Test
    fun `srgb 0_5 linearizes to approximately 0_214`() {
        val linear = srgbToLinear(0.5f)
        assertTrue(abs(linear - 0.214f) < 0.001f, "sRGB 0.5 -> linear = $linear, expected ~0.214")
    }

    @Test
    fun `boundary values are exact`() {
        assertEquals(0f, srgbToLinear(0f))
        assertEquals(1f, srgbToLinear(1f), 1e-6f)
        assertEquals(0f, linearToSrgb(0f))
        assertEquals(1f, linearToSrgb(1f), 1e-5f)
    }

    @Test
    fun `srgbToLinear is monotonically increasing`() {
        var prev = srgbToLinear(0f)
        for (i in 1..100) {
            val v = i / 100f
            val cur = srgbToLinear(v)
            assertTrue(cur > prev, "Not monotonic at $v: $cur <= $prev")
            prev = cur
        }
    }

    @Test
    fun `luminance of white is 1`() {
        assertEquals(1f, extractLuminance(1f, 1f, 1f), 1e-6f)
    }

    @Test
    fun `luminance of black is 0`() {
        assertEquals(0f, extractLuminance(0f, 0f, 0f))
    }

    @Test
    fun `luminance of pure channels matches BT_709 coefficients`() {
        assertEquals(LUM_R, extractLuminance(1f, 0f, 0f), 1e-6f)
        assertEquals(LUM_G, extractLuminance(0f, 1f, 0f), 1e-6f)
        assertEquals(LUM_B, extractLuminance(0f, 0f, 1f), 1e-6f)
    }
}
