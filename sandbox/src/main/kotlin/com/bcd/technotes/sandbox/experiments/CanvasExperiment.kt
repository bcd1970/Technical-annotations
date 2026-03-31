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
import androidx.compose.material.icons.filled.Edit
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
import com.bcd.technotes.sandbox.util.EditableTouchImageView
import com.bcd.technotes.sandbox.util.updateDoubleTapScale
import com.bcd.technotes.core.model.CollageLayout
import com.bcd.technotes.core.model.layoutsForCount
import android.graphics.Rect
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.max

data class PhotoTransform(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null,
    val panX: Float = 0.5f,
    val panY: Float = 0.5f
)

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
    var photoTransforms by remember { mutableStateOf<List<PhotoTransform>>(emptyList()) }
    var addingToCollage by remember { mutableStateOf(false) }
    var cropMode by remember { mutableStateOf(false) }
    var panEnabled by remember { mutableStateOf(false) }
    var editableView by remember { mutableStateOf<EditableTouchImageView?>(null) }
    val bitmapCache = remember { mutableMapOf<Uri, Bitmap>() }
    var activeLayout by remember { mutableStateOf<CollageLayout?>(null) }
    var availableLayouts by remember { mutableStateOf<List<CollageLayout>>(emptyList()) }
    var showEditor by remember { mutableStateOf(false) }

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
    var photoVisibleRects by remember { mutableStateOf<List<RectF>>(emptyList()) }
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
            panEnabled = false
        }
    }

    // Recompute available layouts when photo count changes
    LaunchedEffect(selectedUris.size) {
        if (selectedUris.size >= 2) {
            availableLayouts = layoutsForCount(selectedUris.size)
            if (activeLayout == null || activeLayout!!.cells.size != selectedUris.size) {
                activeLayout = availableLayouts.first()
            }
        } else {
            availableLayouts = emptyList()
            activeLayout = null
        }
    }

    val canvasWidth = context.resources.displayMetrics.widthPixels * 2

    // Decode + stitch collage with bitmap cache (decode only on new URIs, re-stitch on transform changes)
    LaunchedEffect(selectedUris, photoTransforms, activeLayout) {
        if (selectedUris.size < 2 || activeLayout == null) {
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

        val layout = activeLayout!!
        val result = withContext(Dispatchers.IO) {
            stitchFromBitmaps(bitmaps, photoTransforms, layout, canvasWidth)
        }
        backgroundBitmap = result?.bitmap
        photoBounds = result?.photoBounds ?: emptyList()
        photoVisibleRects = result?.photoVisibleRects ?: emptyList()
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
                        showEditor = true
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

    if (showEditor && backgroundBitmap != null) {
        WholeImageEditScreen(
            sourceBitmap = backgroundBitmap!!,
            onDone = { editedBitmap ->
                backgroundBitmap = editedBitmap
                showEditor = false
            },
            onCancel = { showEditor = false }
        )
        return
    }

    val isCollage = selectedUris.size >= 2 && backgroundBitmap != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                                    val index = photoIndexAt(lastBitmapPoint.x, lastBitmapPoint.y)
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
                                    val index = photoIndexAt(lastBitmapPoint.x, lastBitmapPoint.y)
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
                            onPanComplete = { newPanX, newPanY ->
                                val i = this.activePhotoIndex
                                if (i in photoTransforms.indices) {
                                    photoTransforms = photoTransforms.toMutableList().apply {
                                        this[i] = this[i].copy(panX = newPanX, panY = newPanY)
                                    }
                                }
                            }
                            getTransformedBitmap = { index ->
                                val uri = selectedUris.getOrNull(index)
                                val bmp = if (uri != null) bitmapCache[uri] else null
                                if (bmp != null) applyTransform(bmp, photoTransforms.getOrElse(index) { PhotoTransform() }) else null
                            }
                        }.also { editableView = it }
                    },
                    update = { view ->
                        view.setImageBitmap(bitmap)
                        view.photoBounds = photoBounds
                        view.isHorizontalLayout = activeLayout?.aspectRatio == 0f
                        view.photoVisibleRects = photoVisibleRects
                        view.panEnabled = panEnabled
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
                val activeTransform = photoTransforms.getOrElse(activePhotoIndex) { PhotoTransform() }
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
                        Icon(Icons.Default.Flip, "Flip horizontal", tint = if (activeTransform.flipH) Color(0xFF4FC3F7) else Color.White)
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
                            tint = if (activeTransform.flipV) Color(0xFF4FC3F7) else Color.White,
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
                        Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90\u00B0", tint = if (activeTransform.rotation != 0) Color(0xFF4FC3F7) else Color.White)
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
                        Icon(Icons.Default.Crop, "Crop", tint = if (activeTransform.cropRect != null) Color(0xFF4FC3F7) else Color.White)
                    }
                    // Pan toggle: only for grid layouts with cropped photos
                    if (activeLayout != null && activeLayout!!.aspectRatio != 0f) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            panEnabled = !panEnabled
                        }) {
                            Icon(
                                Icons.Default.OpenWith, "Pan",
                                tint = if (panEnabled) Color(0xFF4FC3F7) else Color.White
                            )
                        }
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        addingToCollage = true
                        showPicker = true
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Add photo", tint = Color.White)
                    }
                    IconButton(onClick = { showEditor = true }) {
                        Icon(Icons.Default.Edit, "Edit whole image", tint = Color.White)
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

            // Layout selector (visible when collage shown, not in crop mode)
            if (isCollage && !cropMode && availableLayouts.size > 1) {
                LayoutSelector(
                    layouts = availableLayouts,
                    activeLayoutId = activeLayout?.id ?: "",
                    onLayoutSelected = { layout ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        activeLayout = layout
                    }
                )
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

        if (!editMode) {
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
                            isCollage && availableLayouts.size > 1 -> 136.dp
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

private val layoutPreviewColors = listOf(
    Color(0xFFB0BEC5),
    Color(0xFF78909C),
    Color(0xFF546E7A)
)

@Composable
private fun LayoutSelector(
    layouts: List<CollageLayout>,
    activeLayoutId: String,
    onLayoutSelected: (CollageLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        layouts.forEach { layout ->
            val isActive = layout.id == activeLayoutId
            val borderColor = if (isActive) Color(0xFF4FC3F7) else Color.Transparent
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(Color(0xFF263238), RoundedCornerShape(6.dp))
                    .clickable { onLayoutSelected(layout) }
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
                        val pad = 4.dp.toPx()
                        val innerW = w - pad * 2
                        val innerH = h - pad * 2
                        layout.cells.forEachIndexed { i, cell ->
                            val gap = 1.dp.toPx()
                            drawRect(
                                color = layoutPreviewColors[i % layoutPreviewColors.size],
                                topLeft = Offset(pad + cell.left * innerW + gap, pad + cell.top * innerH + gap),
                                size = Size(
                                    (cell.right - cell.left) * innerW - gap * 2,
                                    (cell.bottom - cell.top) * innerH - gap * 2
                                )
                            )
                        }
                    }
            )
        }
    }
}

data class CollageResult(
    val bitmap: Bitmap,
    val photoBounds: List<RectF>,
    val photoVisibleRects: List<RectF>
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

private fun stitchFromBitmaps(
    bitmaps: List<Bitmap>,
    transforms: List<PhotoTransform>,
    layout: CollageLayout,
    canvasWidth: Int
): CollageResult? {
    if (bitmaps.size < 2) return null

    val transformed = bitmaps.mapIndexed { i, bmp ->
        applyTransform(bmp, transforms.getOrElse(i) { PhotoTransform() })
    }

    // Horizontal row: content-determined canvas size (existing behavior)
    if (layout.aspectRatio == 0f) {
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

        transformed.forEachIndexed { i, tbmp ->
            if (tbmp !== bitmaps[i]) tbmp.recycle()
        }

        val bounds = mutableListOf<RectF>()
        var bx = 0f
        scaledWidths.forEach { w ->
            bounds.add(RectF(bx, 0f, bx + w, uniformHeight.toFloat()))
            bx += w
        }
        val fullVisible = bounds.map { RectF(0f, 0f, 1f, 1f) }
        return CollageResult(result, bounds, fullVisible)
    }

    // Grid layout: fixed aspect ratio canvas with center-crop + pan
    val cw = canvasWidth
    val ch = (cw / layout.aspectRatio).toInt()
    val result = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val bounds = mutableListOf<RectF>()
    val visibleRects = mutableListOf<RectF>()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)

    layout.cells.forEachIndexed { i, cell ->
        val bmp = transformed.getOrNull(i) ?: return@forEachIndexed
        val transform = transforms.getOrElse(i) { PhotoTransform() }

        val cellLeft = (cell.left * cw).toInt()
        val cellTop = (cell.top * ch).toInt()
        val cellRight = (cell.right * cw).toInt()
        val cellBottom = (cell.bottom * ch).toInt()
        val cellW = cellRight - cellLeft
        val cellH = cellBottom - cellTop

        // Scale to fill cell (center-crop with pan offset)
        val scale = max(cellW.toFloat() / bmp.width, cellH.toFloat() / bmp.height)
        val scaledW = (bmp.width * scale).toInt()
        val scaledH = (bmp.height * scale).toInt()
        val overflowX = scaledW - cellW
        val overflowY = scaledH - cellH
        val offsetX = (transform.panX * overflowX).toInt()
        val offsetY = (transform.panY * overflowY).toInt()

        // Source rect: the portion of the bitmap visible in the cell
        val srcLeft = (offsetX / scale).toInt().coerceIn(0, bmp.width - 1)
        val srcTop = (offsetY / scale).toInt().coerceIn(0, bmp.height - 1)
        val srcRight = (srcLeft + cellW / scale).toInt().coerceAtMost(bmp.width)
        val srcBottom = (srcTop + cellH / scale).toInt().coerceAtMost(bmp.height)

        canvas.drawBitmap(
            bmp,
            Rect(srcLeft, srcTop, srcRight, srcBottom),
            Rect(cellLeft, cellTop, cellRight, cellBottom),
            paint
        )
        bounds.add(RectF(cellLeft.toFloat(), cellTop.toFloat(), cellRight.toFloat(), cellBottom.toFloat()))

        // Visible fraction of the full photo (0-1 normalized)
        if (overflowX == 0 && overflowY == 0) {
            visibleRects.add(RectF(0f, 0f, 1f, 1f))
        } else {
            visibleRects.add(RectF(
                offsetX.toFloat() / scaledW,
                offsetY.toFloat() / scaledH,
                (offsetX + cellW).toFloat() / scaledW,
                (offsetY + cellH).toFloat() / scaledH
            ))
        }
    }

    transformed.forEachIndexed { i, tbmp ->
        if (tbmp !== bitmaps[i]) tbmp.recycle()
    }

    return CollageResult(result, bounds, visibleRects)
}

private fun calculateInSampleSize(actualHeight: Int, targetHeight: Int): Int {
    var sampleSize = 1
    while (actualHeight / (sampleSize * 2) >= targetHeight) {
        sampleSize *= 2
    }
    return sampleSize
}
