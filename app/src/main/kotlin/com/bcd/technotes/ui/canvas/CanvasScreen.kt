package com.bcd.technotes.ui.canvas

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.bcd.technotes.ui.photo.PhotoPickerScreen
import com.bcd.technotes.ui.util.EditableTouchImageView
import com.bcd.technotes.ui.util.updateDoubleTapScale

data class PhotoTransform(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null
)

@Composable
fun CanvasScreen() {
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
    var photoTransforms by remember { mutableStateOf<List<PhotoTransform>>(emptyList()) }
    var addingToCollage by remember { mutableStateOf(false) }
    var cropMode by remember { mutableStateOf(false) }
    var editableView by remember { mutableStateOf<EditableTouchImageView?>(null) }
    val bitmapCache = remember { mutableMapOf<Uri, Bitmap>() }

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

    BackHandler(enabled = editMode || cropMode) {
        if (cropMode) {
            cropMode = false
            editableView?.cropMode = false
        } else {
            editMode = false
            activePhotoIndex = -1
        }
    }

    // Decode + stitch collage with bitmap cache (decode only on new URIs, re-stitch on transform changes)
    LaunchedEffect(selectedUris, photoTransforms) {
        if (selectedUris.size < 2) {
            bitmapCache.keys.retainAll(selectedUris.toSet())
            if (selectedUris.isEmpty()) photoBounds = emptyList()
            return@LaunchedEffect
        }

        val missing = selectedUris.filter { it !in bitmapCache }
        if (missing.isNotEmpty()) {
            val decoded = withContext(Dispatchers.IO) {
                missing.mapNotNull { uri ->
                    decodePhoto(context, uri)?.let { uri to it }
                }
            }
            decoded.forEach { (uri, bmp) -> bitmapCache[uri] = bmp }
        }
        bitmapCache.keys.retainAll(selectedUris.toSet())

        val bitmaps = selectedUris.mapNotNull { bitmapCache[it] }
        if (bitmaps.size < 2) return@LaunchedEffect

        val result = withContext(Dispatchers.IO) {
            stitchFromBitmaps(bitmaps, photoTransforms)
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
                if (addingToCollage) {
                    addingToCollage = false
                    selectedUris = selectedUris + uris
                    photoTransforms = photoTransforms + uris.map { PhotoTransform() }
                } else {
                    editMode = false
                    activePhotoIndex = -1
                    if (uris.size == 1) {
                        pendingUri = uris.first()
                        selectedUris = emptyList()
                        photoTransforms = emptyList()
                    } else {
                        backgroundBitmap = null
                        selectedUris = uris
                        photoTransforms = uris.map { PhotoTransform() }
                    }
                }
            },
            onDismiss = {
                showPicker = false
                addingToCollage = false
            }
        )
        return
    }

    val isCollage = selectedUris.size >= 2 && backgroundBitmap != null

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
                                photoTransforms = newOrder.map { photoTransforms[it] }
                                activePhotoIndex = newOrder.indexOf(viewActiveIndex)
                            }
                        }.also { editableView = it }
                    },
                    update = { view ->
                        view.setImageBitmap(bitmap)
                        view.photoBounds = photoBounds
                        view.editMode = editMode
                        view.activePhotoIndex = activePhotoIndex
                        if (cropMode && !view.cropMode) {
                            view.cropMode = true
                            view.initCropRect()
                        } else if (!cropMode && view.cropMode) {
                            view.cropMode = false
                        }
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

        // Bottom controls: command bar + thumbnail bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            // Crop mode: confirm / cancel
            if (cropMode && activePhotoIndex >= 0 && isCollage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x99000000)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        cropMode = false
                        editableView?.cropMode = false
                    }) {
                        Icon(Icons.Default.Close, "Cancel crop", tint = Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val view = editableView ?: return@IconButton
                        val i = activePhotoIndex
                        val pb = photoBounds.getOrNull(i) ?: return@IconButton
                        val cr = view.cropRect
                        val normalizedCrop = RectF(
                            (cr.left - pb.left) / pb.width(),
                            (cr.top - pb.top) / pb.height(),
                            (cr.right - pb.left) / pb.width(),
                            (cr.bottom - pb.top) / pb.height()
                        )
                        photoTransforms = photoTransforms.toMutableList().apply {
                            this[i] = this[i].copy(cropRect = normalizedCrop)
                        }
                        cropMode = false
                        view.cropMode = false
                    }) {
                        Icon(Icons.Default.Check, "Confirm crop", tint = Color.White)
                    }
                }
            }
            // Edit command bar
            else if (editMode && activePhotoIndex >= 0 && isCollage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x99000000)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val i = activePhotoIndex
                        photoTransforms = photoTransforms.toMutableList().apply {
                            this[i] = this[i].copy(flipH = !this[i].flipH)
                        }
                    }) {
                        Icon(Icons.Default.Flip, "Flip horizontal", tint = Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val i = activePhotoIndex
                        photoTransforms = photoTransforms.toMutableList().apply {
                            this[i] = this[i].copy(flipV = !this[i].flipV)
                        }
                    }) {
                        Icon(
                            Icons.Default.Flip, "Flip vertical",
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer { rotationZ = 90f }
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val i = activePhotoIndex
                        photoTransforms = photoTransforms.toMutableList().apply {
                            this[i] = this[i].copy(rotation = (this[i].rotation + 90) % 360)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90\u00B0", tint = Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Clear existing crop so user crops from full photo
                        val i = activePhotoIndex
                        if (photoTransforms[i].cropRect != null) {
                            photoTransforms = photoTransforms.toMutableList().apply {
                                this[i] = this[i].copy(cropRect = null)
                            }
                        }
                        cropMode = true
                    }) {
                        Icon(Icons.Default.Crop, "Crop", tint = Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        addingToCollage = true
                        showPicker = true
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Add photo", tint = Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val i = activePhotoIndex
                        val newUris = selectedUris.toMutableList().apply { removeAt(i) }
                        val newTransforms = photoTransforms.toMutableList().apply { removeAt(i) }
                        if (newUris.size < 2) {
                            editMode = false
                            activePhotoIndex = -1
                            if (newUris.size == 1) {
                                pendingUri = newUris.first()
                                selectedUris = emptyList()
                                photoTransforms = emptyList()
                            } else {
                                selectedUris = emptyList()
                                photoTransforms = emptyList()
                                backgroundBitmap = null
                            }
                        } else {
                            selectedUris = newUris
                            photoTransforms = newTransforms
                            activePhotoIndex = i.coerceAtMost(newUris.size - 1)
                        }
                    }) {
                        Icon(Icons.Default.Delete, "Remove photo", tint = Color.White)
                    }
                }
            }

            // Collage thumbnail bar
            if (isCollage) {
                CollageThumbBar(
                    bitmap = backgroundBitmap!!,
                    viewportRect = viewportRect,
                    isZoomed = isZoomed
                )
            }
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
                .padding(
                    bottom = when {
                        (cropMode || editMode) && activePhotoIndex >= 0 && isCollage -> 128.dp
                        isCollage -> 80.dp
                        else -> 24.dp
                    },
                    end = 24.dp, top = 24.dp, start = 24.dp
                ),
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

private fun decodePhoto(context: android.content.Context, uri: Uri): Bitmap? {
    val displayHeight = context.resources.displayMetrics.heightPixels
    val targetMaxHeight = displayHeight * 2
    return try {
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

private fun applyTransform(bitmap: Bitmap, transform: PhotoTransform): Bitmap {
    if (!transform.flipH && !transform.flipV && transform.rotation == 0 && transform.cropRect == null) return bitmap

    var current = bitmap

    // Rotate + flip first
    if (transform.flipH || transform.flipV || transform.rotation != 0) {
        val matrix = Matrix()
        if (transform.flipH) matrix.postScale(-1f, 1f)
        if (transform.flipV) matrix.postScale(1f, -1f)
        matrix.postRotate(transform.rotation.toFloat())
        current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
    }

    // Crop last (applied to post-transform photo)
    if (transform.cropRect != null) {
        val cr = transform.cropRect
        val x = (cr.left * current.width).toInt().coerceIn(0, current.width - 1)
        val y = (cr.top * current.height).toInt().coerceIn(0, current.height - 1)
        val w = ((cr.right - cr.left) * current.width).toInt().coerceAtMost(current.width - x).coerceAtLeast(1)
        val h = ((cr.bottom - cr.top) * current.height).toInt().coerceAtMost(current.height - y).coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(current, x, y, w, h)
        if (current !== bitmap) current.recycle()
        current = cropped
    }

    return current
}

private fun stitchFromBitmaps(bitmaps: List<Bitmap>, transforms: List<PhotoTransform>): CollageResult? {
    if (bitmaps.size < 2) return null

    val transformed = bitmaps.mapIndexed { i, bmp ->
        applyTransform(bmp, transforms.getOrElse(i) { PhotoTransform() })
    }

    val uniformHeight = transformed.minOf { it.height }
    val scaledWidths = transformed.map { bmp ->
        (bmp.width * (uniformHeight.toFloat() / bmp.height)).toInt()
    }
    val totalWidth = scaledWidths.sum()

    val result = Bitmap.createBitmap(totalWidth, uniformHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    var x = 0f
    transformed.forEachIndexed { i, bmp ->
        val scale = uniformHeight.toFloat() / bmp.height
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(x, 0f)
        }
        canvas.drawBitmap(bmp, matrix, null)
        x += scaledWidths[i]
    }

    // Recycle only transform-created copies, not cached originals
    transformed.forEachIndexed { i, tbmp ->
        if (tbmp !== bitmaps[i]) tbmp.recycle()
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
