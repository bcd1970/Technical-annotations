package com.bcd.technotes.sandbox.experiments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.ContextCompat
import android.graphics.PointF
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableIntStateOf
import com.ortiz.touchview.OnTouchImageViewListener
import com.ortiz.touchview.OnTouchCoordinatesListener
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bcd.technotes.sandbox.util.EditableTouchImageView
import com.bcd.technotes.sandbox.util.updateDoubleTapScale

@Composable
fun CanvasExperiment() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var backgroundBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var viewportRect by remember { mutableStateOf<RectF?>(null) }
    var isZoomed by remember { mutableStateOf(false) }
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

    var photoBounds by remember { mutableStateOf<List<RectF>>(emptyList()) }
    var editMode by remember { mutableStateOf(false) }
    var activePhotoIndex by remember { mutableIntStateOf(-1) }
    var lastBitmapPoint by remember { mutableStateOf(PointF(0f, 0f)) }

    BackHandler(enabled = editMode) {
        editMode = false
        activePhotoIndex = -1
    }

    // Stitch collage: decode all photos, scale to uniform height, draw side-by-side
    LaunchedEffect(selectedUris) {
        if (selectedUris.size < 2) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            stitchCollage(context, selectedUris)
        }
        backgroundBitmap = result?.bitmap
        photoBounds = result?.photoBounds ?: emptyList()
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
                editMode = false
                activePhotoIndex = -1
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
                        EditableTouchImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageBitmap(bitmap)
                            maxZoom = 10f
                            minZoom = 1f
                            this.photoBounds = photoBounds
                            post { updateDoubleTapScale() }
                            setOnTouchImageViewListener(object : OnTouchImageViewListener {
                                override fun onMove() {
                                    isZoomed = this@apply.isZoomed
                                    viewportRect = this@apply.zoomedRect
                                }
                            })
                            setOnTouchCoordinatesListener(object : OnTouchCoordinatesListener {
                                override fun onTouchCoordinate(view: android.view.View, event: android.view.MotionEvent, bitmapPoint: PointF) {
                                    lastBitmapPoint = bitmapPoint
                                }
                            })
                            setOnLongClickListener { view ->
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                if (this.photoBounds.isNotEmpty()) {
                                    val index = photoIndexAt(lastBitmapPoint.x)
                                    if (index >= 0) {
                                        editMode = true
                                        activePhotoIndex = index
                                        this.editMode = true
                                        this.activePhotoIndex = index
                                    }
                                }
                                true
                            }
                            setOnClickListener { view ->
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (editMode && this.photoBounds.isNotEmpty()) {
                                    val index = photoIndexAt(lastBitmapPoint.x)
                                    if (index >= 0) {
                                        activePhotoIndex = index
                                        this.activePhotoIndex = index
                                    }
                                }
                            }
                            onReorderComplete = { newOrder ->
                                val viewActiveIndex = this.activePhotoIndex
                                selectedUris = newOrder.map { selectedUris[it] }
                                activePhotoIndex = newOrder.indexOf(viewActiveIndex)
                            }
                        }
                    },
                    update = { view ->
                        view.setImageBitmap(bitmap)
                        view.photoBounds = photoBounds
                        view.editMode = editMode
                        view.activePhotoIndex = activePhotoIndex
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

        // Collage thumbnail bar
        if (selectedUris.size >= 2 && backgroundBitmap != null) {
            CollageThumbBar(
                bitmap = backgroundBitmap!!,
                viewportRect = viewportRect,
                isZoomed = isZoomed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) showPicker = true else permissionLauncher.launch(permission)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = if (selectedUris.size >= 2 && backgroundBitmap != null) 80.dp else 24.dp, end = 24.dp, top = 24.dp, start = 24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Load photo")
        }
    }
}

@Composable
private fun CollageThumbBar(
    bitmap: Bitmap,
    viewportRect: RectF?,
    isZoomed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (isZoomed) 1f else 0f,
            label = "viewportAlpha"
        )
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Collage preview",
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .height(56.dp)
                .horizontalScroll(rememberScrollState())
                .drawWithContent {
                    drawContent()
                    if (indicatorAlpha > 0f && viewportRect != null) {
                        val left = viewportRect.left * size.width
                        val top = viewportRect.top * size.height
                        val right = viewportRect.right * size.width
                        val bottom = viewportRect.bottom * size.height
                        drawRect(
                            color = Color.White.copy(alpha = 0.25f * indicatorAlpha),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.85f * indicatorAlpha),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
        )
    }
}

data class CollageResult(
    val bitmap: Bitmap,
    val photoBounds: List<RectF>
)

private fun stitchCollage(context: android.content.Context, uris: List<Uri>): CollageResult? {
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

    val bounds = mutableListOf<RectF>()
    var bx = 0f
    scaledWidths.forEach { w ->
        bounds.add(RectF(bx, 0f, bx + w, uniformHeight.toFloat()))
        bx += w
    }

    return CollageResult(result, bounds)
}

private fun calculateInSampleSize(actualHeight: Int, targetHeight: Int): Int {
    var sampleSize = 1
    while (actualHeight / (sampleSize * 2) >= targetHeight) {
        sampleSize *= 2
    }
    return sampleSize
}
