package com.bcd.technotes.ui.canvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bcd.technotes.ui.photo.PhotoPickerScreen
import com.bcd.technotes.ui.util.updateDoubleTapScale
import com.ortiz.touchview.TouchImageView

@Composable
fun CanvasScreen() {
    val context = LocalContext.current

    var backgroundBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
                else Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) showPicker = true
    }

    // Decode single photo off main thread
    LaunchedEffect(pendingUri) {
        pendingUri?.let { uri ->
            backgroundBitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            pendingUri = null
        }
    }

    // Stitch collage: decode all photos, scale to uniform height, draw side-by-side
    LaunchedEffect(selectedUris) {
        if (selectedUris.size < 2) return@LaunchedEffect
        backgroundBitmap = withContext(Dispatchers.IO) {
            stitchCollage(context, selectedUris)
        }
    }

    // Request permission on first launch, then show picker
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            showPicker = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    if (showPicker) {
        PhotoPickerScreen(
            onPhotosConfirmed = { uris ->
                showPicker = false
                if (uris.size == 1) {
                    pendingUri = uris.first()
                    selectedUris = emptyList()
                } else {
                    backgroundBitmap = null
                    selectedUris = uris
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
        when {
            backgroundBitmap != null -> {
                val bitmap = backgroundBitmap!!
                AndroidView(
                    factory = { ctx ->
                        TouchImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageBitmap(bitmap)
                            maxZoom = 10f
                            minZoom = 1f
                            post { updateDoubleTapScale() }
                        }
                    },
                    update = { view ->
                        view.setImageBitmap(bitmap)
                        view.post { view.updateDoubleTapScale() }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = "Tap the camera button to load a photo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) showPicker = true else permissionLauncher.launch(permission)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Load photo")
        }
    }
}

private fun stitchCollage(context: android.content.Context, uris: List<Uri>): Bitmap? {
    // Decode all bitmaps with downsampling to ~2x screen height
    val displayHeight = context.resources.displayMetrics.heightPixels
    val targetMaxHeight = displayHeight * 2

    val bitmaps = uris.mapNotNull { uri ->
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, opts)
                opts
            }?.let { opts ->
                val sampleSize = calculateInSampleSize(opts.outHeight, targetMaxHeight)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream, null,
                        BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    )
                }
            }
        } catch (_: Exception) { null }
    }

    if (bitmaps.size < 2) {
        bitmaps.forEach { it.recycle() }
        return null
    }

    // Uniform height = min of all decoded heights (no upscaling)
    val uniformHeight = bitmaps.minOf { it.height }

    // Calculate scaled widths
    val scaledWidths = bitmaps.map { bmp ->
        (bmp.width * (uniformHeight.toFloat() / bmp.height)).toInt()
    }
    val totalWidth = scaledWidths.sum()

    // Create result bitmap and draw each photo side-by-side
    val result = Bitmap.createBitmap(totalWidth, uniformHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    var x = 0f
    bitmaps.forEachIndexed { i, bmp ->
        val scale = uniformHeight.toFloat() / bmp.height
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(x, 0f)
        }
        canvas.drawBitmap(bmp, matrix, null)
        x += scaledWidths[i]
        bmp.recycle()
    }

    return result
}

private fun calculateInSampleSize(actualHeight: Int, targetHeight: Int): Int {
    var sampleSize = 1
    while (actualHeight / (sampleSize * 2) >= targetHeight) {
        sampleSize *= 2
    }
    return sampleSize
}
