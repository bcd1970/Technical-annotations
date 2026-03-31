package com.bcd.technotes.ui.photo

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.bcd.technotes.data.model.Photo
import com.bcd.technotes.ui.util.updateDoubleTapScale
import com.ortiz.touchview.TouchImageView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPreview(
    photos: List<Photo>,
    startIndex: Int,
    selectedPhotoIds: Set<Long>,
    onPhotoTap: (Photo) -> Unit,
    onPhotoLongPress: (Photo) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { photos.size }
    )
    var isZoomed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]

            AndroidView(
                factory = { ctx ->
                    TouchImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        maxZoom = 10f
                        minZoom = 1f
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setOnClickListener { view -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onPhotoTap(photo) }
                        setOnLongClickListener { view ->
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onPhotoLongPress(photo)
                            true
                        }
                        setOnTouchImageViewListener(object : com.ortiz.touchview.OnTouchImageViewListener {
                            override fun onMove() {
                                isZoomed = this@apply.isZoomed
                            }
                        })
                    }
                },
                update = { view ->
                    view.resetZoom()
                    isZoomed = false
                    val dw = view.resources.displayMetrics.widthPixels
                    val dh = view.resources.displayMetrics.heightPixels
                    try {
                        view.context.contentResolver.openInputStream(photo.uri)?.use { stream ->
                            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                            var sampleSize = 1
                            while (opts.outWidth / sampleSize > dw * 2 || opts.outHeight / sampleSize > dh * 2) {
                                sampleSize *= 2
                            }
                            view.context.contentResolver.openInputStream(photo.uri)?.use { stream2 ->
                                val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                val bitmap = android.graphics.BitmapFactory.decodeStream(stream2, null, decodeOpts)
                                if (bitmap != null) view.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {
                        view.setImageURI(photo.uri)
                    }
                    view.doOnLayout { view.updateDoubleTapScale() }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${photos.size}",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        )

        if (selectedPhotoIds.contains(photos.getOrNull(pagerState.currentPage)?.id)) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(32.dp)
            )
        }
    }
}
