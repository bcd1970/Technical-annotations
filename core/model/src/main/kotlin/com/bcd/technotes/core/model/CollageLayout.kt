package com.bcd.technotes.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CollageCell(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class CollageLayout(
    val id: String,
    val cells: List<CollageCell>,
    val aspectRatio: Float // width/height. 0f = content-determined (horizontal row)
)

fun layoutsForCount(count: Int): List<CollageLayout> = when {
    count == 2 -> layouts2
    count == 3 -> layouts3
    count == 4 -> layouts4
    count == 5 -> layouts5
    count == 6 -> layouts6
    count >= 7 -> layoutsN(count)
    else -> emptyList()
}

// --- 2 photos ---

private val layouts2 = listOf(
    // Horizontal row (content-determined aspect ratio)
    CollageLayout(
        id = "h-row-2",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 1f, 1f)
        ),
        aspectRatio = 0f
    ),
    // Vertical stack
    CollageLayout(
        id = "v-stack-2",
        cells = listOf(
            CollageCell(0f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 0.75f // 3:4 portrait
    ),
    // 70/30 left dominant
    CollageLayout(
        id = "split-70-30-2",
        cells = listOf(
            CollageCell(0f, 0f, 0.7f, 1f),
            CollageCell(0.7f, 0f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    )
)

// --- 3 photos ---

private val layouts3 = listOf(
    // Horizontal row
    CollageLayout(
        id = "h-row-3",
        cells = listOf(
            CollageCell(0f, 0f, 0.333f, 1f),
            CollageCell(0.333f, 0f, 0.667f, 1f),
            CollageCell(0.667f, 0f, 1f, 1f)
        ),
        aspectRatio = 0f
    ),
    // 1 large left + 2 stacked right
    CollageLayout(
        id = "1L-2R-3",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 1f, 0.5f),
            CollageCell(0.5f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // 1 large top + 2 side-by-side bottom
    CollageLayout(
        id = "1T-2B-3",
        cells = listOf(
            CollageCell(0f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 0.5f, 1f),
            CollageCell(0.5f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 3f / 4f
    ),
    // 2 stacked left + 1 large right
    CollageLayout(
        id = "2L-1R-3",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 0.5f),
            CollageCell(0f, 0.5f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    )
)

// --- 4 photos ---

private val layouts4 = listOf(
    // 2x2 grid
    CollageLayout(
        id = "grid-2x2-4",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 0.5f),
            CollageCell(0.5f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 0.5f, 1f),
            CollageCell(0.5f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 1f
    ),
    // Horizontal row
    CollageLayout(
        id = "h-row-4",
        cells = listOf(
            CollageCell(0f, 0f, 0.25f, 1f),
            CollageCell(0.25f, 0f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 0.75f, 1f),
            CollageCell(0.75f, 0f, 1f, 1f)
        ),
        aspectRatio = 0f
    ),
    // 1 large left + 3 stacked right
    CollageLayout(
        id = "1L-3R-4",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 1f, 0.333f),
            CollageCell(0.5f, 0.333f, 1f, 0.667f),
            CollageCell(0.5f, 0.667f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // 1 large top + 3 bottom
    CollageLayout(
        id = "1T-3B-4",
        cells = listOf(
            CollageCell(0f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 0.333f, 1f),
            CollageCell(0.333f, 0.5f, 0.667f, 1f),
            CollageCell(0.667f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 3f / 4f
    )
)

// --- 5 photos ---

private val layouts5 = listOf(
    // 2 top + 3 bottom
    CollageLayout(
        id = "2T-3B-5",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 0.5f),
            CollageCell(0.5f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 0.333f, 1f),
            CollageCell(0.333f, 0.5f, 0.667f, 1f),
            CollageCell(0.667f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // 3 top + 2 bottom
    CollageLayout(
        id = "3T-2B-5",
        cells = listOf(
            CollageCell(0f, 0f, 0.333f, 0.5f),
            CollageCell(0.333f, 0f, 0.667f, 0.5f),
            CollageCell(0.667f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 0.5f, 1f),
            CollageCell(0.5f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // 1 large left + 4 grid right
    CollageLayout(
        id = "1L-4R-5",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 0.75f, 0.5f),
            CollageCell(0.75f, 0f, 1f, 0.5f),
            CollageCell(0.5f, 0.5f, 0.75f, 1f),
            CollageCell(0.75f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // Horizontal row
    CollageLayout(
        id = "h-row-5",
        cells = listOf(
            CollageCell(0f, 0f, 0.2f, 1f),
            CollageCell(0.2f, 0f, 0.4f, 1f),
            CollageCell(0.4f, 0f, 0.6f, 1f),
            CollageCell(0.6f, 0f, 0.8f, 1f),
            CollageCell(0.8f, 0f, 1f, 1f)
        ),
        aspectRatio = 0f
    )
)

// --- 6 photos ---

private val layouts6 = listOf(
    // 3x2 grid (3 cols, 2 rows)
    CollageLayout(
        id = "grid-3x2-6",
        cells = listOf(
            CollageCell(0f, 0f, 0.333f, 0.5f),
            CollageCell(0.333f, 0f, 0.667f, 0.5f),
            CollageCell(0.667f, 0f, 1f, 0.5f),
            CollageCell(0f, 0.5f, 0.333f, 1f),
            CollageCell(0.333f, 0.5f, 0.667f, 1f),
            CollageCell(0.667f, 0.5f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // 2x3 grid (2 cols, 3 rows)
    CollageLayout(
        id = "grid-2x3-6",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 0.333f),
            CollageCell(0.5f, 0f, 1f, 0.333f),
            CollageCell(0f, 0.333f, 0.5f, 0.667f),
            CollageCell(0.5f, 0.333f, 1f, 0.667f),
            CollageCell(0f, 0.667f, 0.5f, 1f),
            CollageCell(0.5f, 0.667f, 1f, 1f)
        ),
        aspectRatio = 3f / 4f
    ),
    // 1 large left + 5 stacked right (2 cols on right)
    CollageLayout(
        id = "1L-5R-6",
        cells = listOf(
            CollageCell(0f, 0f, 0.5f, 1f),
            CollageCell(0.5f, 0f, 0.75f, 0.333f),
            CollageCell(0.75f, 0f, 1f, 0.333f),
            CollageCell(0.5f, 0.333f, 0.75f, 0.667f),
            CollageCell(0.75f, 0.333f, 1f, 0.667f),
            CollageCell(0.5f, 0.667f, 1f, 1f)
        ),
        aspectRatio = 4f / 3f
    ),
    // Horizontal row
    CollageLayout(
        id = "h-row-6",
        cells = listOf(
            CollageCell(0f, 0f, 1f / 6f, 1f),
            CollageCell(1f / 6f, 0f, 2f / 6f, 1f),
            CollageCell(2f / 6f, 0f, 3f / 6f, 1f),
            CollageCell(3f / 6f, 0f, 4f / 6f, 1f),
            CollageCell(4f / 6f, 0f, 5f / 6f, 1f),
            CollageCell(5f / 6f, 0f, 1f, 1f)
        ),
        aspectRatio = 0f
    )
)

// --- 7+ photos: dynamic grid ---

private fun layoutsN(count: Int): List<CollageLayout> {
    val cols = when {
        count <= 4 -> 2
        count <= 9 -> 3
        else -> 4
    }
    val rows = (count + cols - 1) / cols
    val remainder = count % cols

    val gridCells = mutableListOf<CollageCell>()
    var photoIdx = 0
    for (row in 0 until rows) {
        val colsInRow = if (remainder != 0 && row == rows - 1) remainder else cols
        val cellWidth = 1f / colsInRow
        val cellHeight = 1f / rows
        for (col in 0 until colsInRow) {
            gridCells.add(
                CollageCell(
                    left = col * cellWidth,
                    top = row * cellHeight,
                    right = (col + 1) * cellWidth,
                    bottom = (row + 1) * cellHeight
                )
            )
            photoIdx++
        }
    }

    val gridLayout = CollageLayout(
        id = "grid-${cols}c-$count",
        cells = gridCells,
        aspectRatio = cols.toFloat() / rows
    )

    // Horizontal row option
    val hRowCells = (0 until count).map { i ->
        CollageCell(i.toFloat() / count, 0f, (i + 1).toFloat() / count, 1f)
    }
    val hRowLayout = CollageLayout(
        id = "h-row-$count",
        cells = hRowCells,
        aspectRatio = 0f
    )

    // 1 large left + (N-1) stacked right
    val rightCount = count - 1
    val rightCells = (0 until rightCount).map { i ->
        CollageCell(
            left = 0.5f,
            top = i.toFloat() / rightCount,
            right = 1f,
            bottom = (i + 1).toFloat() / rightCount
        )
    }
    val largeLeftLayout = CollageLayout(
        id = "1L-${rightCount}R-$count",
        cells = listOf(CollageCell(0f, 0f, 0.5f, 1f)) + rightCells,
        aspectRatio = 4f / 3f
    )

    return listOf(gridLayout, hRowLayout, largeLeftLayout)
}
