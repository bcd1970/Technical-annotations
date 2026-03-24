package com.bcd.technotes.ui.canvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import com.bcd.technotes.ui.photo.PhotoPickerScreen

@Composable
fun CanvasScreen() {
    val context = LocalContext.current

    var backgroundImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showPicker by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    fun minScale(): Float {
        val image = backgroundImage ?: return 1f
        if (canvasSize == Size.Zero) return 1f
        return maxOf(canvasSize.width / image.width, canvasSize.height / image.height)
    }

    fun clampOffset(rawOffset: Offset, currentScale: Float): Offset {
        val image = backgroundImage ?: return Offset.Zero
        val scaledW = currentScale * image.width
        val scaledH = currentScale * image.height
        val maxX = maxOf(0f, (scaledW - canvasSize.width) / 2f)
        val maxY = maxOf(0f, (scaledH - canvasSize.height) / 2f)
        val centerX = (canvasSize.width - scaledW) / 2f
        val centerY = (canvasSize.height - scaledH) / 2f
        return if (scaledW <= canvasSize.width && scaledH <= canvasSize.height) {
            Offset(centerX, centerY)
        } else {
            Offset(
                x = rawOffset.x.coerceIn(centerX.coerceAtMost(-maxX), maxX.coerceAtLeast(-centerX)),
                y = rawOffset.y.coerceIn(centerY.coerceAtMost(-maxY), maxY.coerceAtLeast(-centerY))
            )
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPicker = true
        }
    }

    if (showPicker) {
        PhotoPickerScreen(
            onPhotoSelected = { uri ->
                showPicker = false
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        backgroundImage = bitmap.asImageBitmap()
                        offset = Offset.Zero
                        scale = 1f
                    }
                }
            },
            onDismiss = { showPicker = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { canvasSize = it.toSize() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val fitScale = minScale()
                            if (scale > fitScale) {
                                scale = fitScale
                                offset = clampOffset(Offset.Zero, fitScale)
                            } else {
                                val targetScale = 2.5f
                                val newOffset = (offset - tapOffset) * (targetScale / scale) + tapOffset
                                scale = targetScale
                                offset = clampOffset(newOffset, targetScale)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(minScale(), 10f)
                        val newOffset = (offset - centroid) * (newScale / oldScale) + centroid + pan

                        scale = newScale
                        offset = clampOffset(newOffset, newScale)
                    }
                }
        ) {
            withTransform({
                translate(left = offset.x, top = offset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                backgroundImage?.let { image ->
                    drawImage(
                        image = image,
                        dstSize = IntSize(image.width, image.height)
                    )
                }
            }
        }

        if (backgroundImage == null) {
            Text(
                text = "Tap the camera button to load a photo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        FloatingActionButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, permission
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    showPicker = true
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Load photo"
            )
        }
    }
}
