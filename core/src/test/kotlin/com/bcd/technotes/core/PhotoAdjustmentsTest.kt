package com.bcd.technotes.core

import com.bcd.technotes.core.model.AdjustmentType
import com.bcd.technotes.core.model.PhotoAdjustments
import com.bcd.technotes.core.model.getValue
import com.bcd.technotes.core.model.withValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoAdjustmentsTest {

    @Test
    fun `default adjustments have all zeros`() {
        val adj = PhotoAdjustments()
        assertEquals(0f, adj.brightness)
        assertEquals(0f, adj.contrast)
        assertEquals(0f, adj.saturation)
        assertEquals(0f, adj.warmth)
        assertEquals(0f, adj.tint)
        assertEquals(0f, adj.highlights)
        assertEquals(0f, adj.shadows)
        assertEquals(0f, adj.sharpness)
        assertEquals(0f, adj.vignette)
    }

    @Test
    fun `isDefault returns true for default instance`() {
        assertTrue(PhotoAdjustments().isDefault)
        assertTrue(PhotoAdjustments.DEFAULT.isDefault)
    }

    @Test
    fun `isDefault returns false when any field is non-zero`() {
        assertFalse(PhotoAdjustments(brightness = 1f).isDefault)
        assertFalse(PhotoAdjustments(sharpness = 50f).isDefault)
        assertFalse(PhotoAdjustments(vignette = 10f).isDefault)
    }

    @Test
    fun `getValue returns correct field for each type`() {
        val adj = PhotoAdjustments(
            brightness = 10f, contrast = 20f, saturation = 30f,
            warmth = 40f, tint = 50f, highlights = 60f,
            shadows = 70f, sharpness = 80f, vignette = 90f
        )
        assertEquals(10f, adj.getValue(AdjustmentType.BRIGHTNESS))
        assertEquals(20f, adj.getValue(AdjustmentType.CONTRAST))
        assertEquals(30f, adj.getValue(AdjustmentType.SATURATION))
        assertEquals(40f, adj.getValue(AdjustmentType.WARMTH))
        assertEquals(50f, adj.getValue(AdjustmentType.TINT))
        assertEquals(60f, adj.getValue(AdjustmentType.HIGHLIGHTS))
        assertEquals(70f, adj.getValue(AdjustmentType.SHADOWS))
        assertEquals(80f, adj.getValue(AdjustmentType.SHARPNESS))
        assertEquals(90f, adj.getValue(AdjustmentType.VIGNETTE))
    }

    @Test
    fun `withValue sets correct field for each type`() {
        for (type in AdjustmentType.entries) {
            val adj = PhotoAdjustments().withValue(type, 42f)
            assertEquals(42f, adj.getValue(type))
            for (other in AdjustmentType.entries) {
                if (other != type) assertEquals(0f, adj.getValue(other))
            }
        }
    }

    @Test
    fun `serialization round-trip preserves values`() {
        val original = PhotoAdjustments(
            brightness = -50f, contrast = 25f, saturation = -100f,
            warmth = 75f, tint = -30f, highlights = 100f,
            shadows = -80f, sharpness = 60f, vignette = 45f
        )
        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<PhotoAdjustments>(json)
        assertEquals(original, restored)
    }

    @Test
    fun `serialization of default produces valid json`() {
        val json = Json.encodeToString(PhotoAdjustments.DEFAULT)
        val restored = Json.decodeFromString<PhotoAdjustments>(json)
        assertTrue(restored.isDefault)
    }

    @Test
    fun `AdjustmentType entries cover all nine adjustments`() {
        assertEquals(9, AdjustmentType.entries.size)
    }

    @Test
    fun `AdjustmentType ranges are correct`() {
        for (type in AdjustmentType.entries) {
            when (type) {
                AdjustmentType.SHARPNESS, AdjustmentType.VIGNETTE -> {
                    assertEquals(0f, type.min)
                    assertEquals(100f, type.max)
                }
                else -> {
                    assertEquals(-100f, type.min)
                    assertEquals(100f, type.max)
                }
            }
        }
    }
}
