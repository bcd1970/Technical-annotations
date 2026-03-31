package com.bcd.technotes.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.bcd.technotes.data.model.Photo

@Composable
fun SelectedPhotosStrip(
    selectedPhotos: List<Photo>,
    onRemovePhoto: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val selectedIds = remember(selectedPhotos) { selectedPhotos.map { it.id }.toSet() }

    // Keep removed photos alive for exit animation
    val photoCache = remember { mutableMapOf<Long, Photo>() }
    var exitingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Cache all photos we see so we can display them during exit
    selectedPhotos.forEach { photoCache[it.id] = it }

    // Detect removals by diffing against previous selected IDs
    val prevIds = remember { mutableStateOf(selectedIds) }
    if (prevIds.value != selectedIds) {
        val removed = prevIds.value - selectedIds - exitingIds
        if (removed.isNotEmpty()) {
            exitingIds = exitingIds + removed
        }
        prevIds.value = selectedIds
    }

    // Display list: active photos + exiting photos (kept alive for animation)
    val displayPhotos = remember(selectedPhotos, exitingIds) {
        selectedPhotos + exitingIds.mapNotNull { photoCache[it] }
    }

    // Auto-scroll to end when new photos added
    LaunchedEffect(selectedPhotos.size) {
        if (selectedPhotos.isNotEmpty()) {
            kotlinx.coroutines.delay(50)
            val target = scrollState.maxValue
            val start = scrollState.value
            val distance = target - start
            if (distance > 0) {
                val startTime = withFrameNanos { it }
                val durationNs = 350_000_000L // 350ms
                var t = 0f
                while (t < 1f) {
                    val frameTime = withFrameNanos { it }
                    val elapsed = frameTime - startTime
                    t = (elapsed.toFloat() / durationNs).coerceIn(0f, 1f)
                    val eased = 1f - (1f - t) * (1f - t)
                    scrollState.scrollTo(start + (distance * eased).toInt())
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        displayPhotos.forEach { photo ->
            val isExiting = exitingIds.contains(photo.id)

            key(photo.id) {
                var progress by remember { mutableFloatStateOf(if (isExiting) 1f else 0f) }

                if (isExiting) {
                    // Exit: shrink + slide down + fade out
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        val durationNs = 150_000_000L // 150ms
                        var done = false
                        while (!done) {
                            withFrameNanos { frameTime ->
                                val elapsed = frameTime - startTime
                                val t = (elapsed.toFloat() / durationNs).coerceIn(0f, 1f)
                                progress = 1f - (t * t)
                                if (t >= 1f) done = true
                            }
                        }
                        exitingIds = exitingIds - photo.id
                        photoCache.remove(photo.id)
                    }
                } else {
                    // Entry: bounce in with overshoot
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        val durationNs = 300_000_000L // 300ms
                        var done = false
                        while (!done) {
                            withFrameNanos { frameTime ->
                                val elapsed = frameTime - startTime
                                val t = (elapsed.toFloat() / durationNs).coerceIn(0f, 1f)
                                progress = if (t < 1f) {
                                    val t2 = t - 1f
                                    t2 * t2 * (3f * t2 + 2f) + 1f
                                } else 1f
                                if (t >= 1f) done = true
                            }
                        }
                    }
                }

                Box(modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = progress
                        scaleY = progress
                        translationY = (1f - progress) * 100f
                        alpha = progress
                    }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .size(Size(104, 104))
                            .precision(Precision.INEXACT)
                            .memoryCacheKey("strip_${photo.id}")
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                    )
                    if (!isExiting) {
                        SmallFloatingActionButton(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRemovePhoto(photo.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp),
                            shape = CircleShape,
                            containerColor = Color(0xCC000000),
                            contentColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
