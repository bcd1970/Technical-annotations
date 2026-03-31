package com.bcd.technotes.core

import com.bcd.technotes.core.model.CollageCell
import com.bcd.technotes.core.model.CollageLayout
import com.bcd.technotes.core.model.layoutsForCount
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollageLayoutTest {

    @Test
    fun `layoutsForCount returns results for counts 2 through 10`() {
        for (count in 2..10) {
            val layouts = layoutsForCount(count)
            assertTrue(layouts.isNotEmpty(), "No layouts for count=$count")
            layouts.forEach { layout ->
                assertEquals(count, layout.cells.size, "Layout ${layout.id} has ${layout.cells.size} cells, expected $count")
            }
        }
    }

    @Test
    fun `all cells are within 0-1 bounds`() {
        for (count in 2..10) {
            for (layout in layoutsForCount(count)) {
                for (cell in layout.cells) {
                    assertTrue(cell.left >= 0f && cell.left <= 1f, "${layout.id}: left=${cell.left} out of bounds")
                    assertTrue(cell.top >= 0f && cell.top <= 1f, "${layout.id}: top=${cell.top} out of bounds")
                    assertTrue(cell.right >= 0f && cell.right <= 1f, "${layout.id}: right=${cell.right} out of bounds")
                    assertTrue(cell.bottom >= 0f && cell.bottom <= 1f, "${layout.id}: bottom=${cell.bottom} out of bounds")
                    assertTrue(cell.right > cell.left, "${layout.id}: right <= left")
                    assertTrue(cell.bottom > cell.top, "${layout.id}: bottom <= top")
                }
            }
        }
    }

    @Test
    fun `cells do not overlap`() {
        for (count in 2..10) {
            for (layout in layoutsForCount(count)) {
                for (i in layout.cells.indices) {
                    for (j in i + 1 until layout.cells.size) {
                        val a = layout.cells[i]
                        val b = layout.cells[j]
                        val overlapX = a.left < b.right - 0.001f && a.right > b.left + 0.001f
                        val overlapY = a.top < b.bottom - 0.001f && a.bottom > b.top + 0.001f
                        assertTrue(
                            !(overlapX && overlapY),
                            "${layout.id}: cells $i and $j overlap"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `cells cover full area for fixed-aspect layouts`() {
        for (count in 2..10) {
            for (layout in layoutsForCount(count)) {
                if (layout.aspectRatio == 0f) continue
                val totalArea = layout.cells.sumOf {
                    ((it.right - it.left) * (it.bottom - it.top)).toDouble()
                }
                assertTrue(
                    totalArea > 0.99 && totalArea < 1.01,
                    "${layout.id}: total cell area=$totalArea, expected ~1.0"
                )
            }
        }
    }

    @Test
    fun `aspect ratios are positive or zero`() {
        for (count in 2..10) {
            for (layout in layoutsForCount(count)) {
                assertTrue(layout.aspectRatio >= 0f, "${layout.id}: negative aspectRatio=${layout.aspectRatio}")
            }
        }
    }

    @Test
    fun `layout IDs are unique within each count`() {
        for (count in 2..10) {
            val ids = layoutsForCount(count).map { it.id }
            assertEquals(ids.size, ids.distinct().size, "Duplicate layout IDs for count=$count: $ids")
        }
    }

    @Test
    fun `horizontal row layout exists for every count`() {
        for (count in 2..10) {
            val hasHRow = layoutsForCount(count).any { it.aspectRatio == 0f }
            assertTrue(hasHRow, "No horizontal-row layout for count=$count")
        }
    }

    @Test
    fun `serialization round-trip`() {
        val json = Json { prettyPrint = true }
        val layout = layoutsForCount(4).first()
        val encoded = json.encodeToString(layout)
        val decoded = json.decodeFromString<CollageLayout>(encoded)
        assertEquals(layout, decoded)
    }

    @Test
    fun `returns empty for count less than 2`() {
        assertTrue(layoutsForCount(0).isEmpty())
        assertTrue(layoutsForCount(1).isEmpty())
    }
}
