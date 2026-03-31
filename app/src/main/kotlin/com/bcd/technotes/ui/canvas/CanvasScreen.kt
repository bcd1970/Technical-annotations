package com.bcd.technotes.ui.canvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
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
import androidx.compose.runtime.collectAsState
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
import com.ortiz.touchview.OnTouchImageViewListener
import com.ortiz.touchview.OnTouchCoordinatesListener
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcd.technotes.ui.photo.PhotoPickerScreen
import com.bcd.technotes.ui.util.EditableTouchImageView
import com.bcd.technotes.ui.util.updateDoubleTapScale
import com.bcd.technotes.core.model.CollageLayout
import com.bcd.technotes.data.model.PhotoTransform
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.OpenWith
import com.bcd.technotes.ui.editor.WholeImageEditScreen
import com.bcd.technotes.viewmodel.CanvasViewModel

@Composable
fun CanvasScreen(viewModel: CanvasViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // ViewModel state
    val backgroundBitmap by viewModel.backgroundBitmap.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val photoTransforms by viewModel.photoTransforms.collectAsState()
    val photoBounds by viewModel.photoBounds.collectAsState()
    val photoVisibleRects by viewModel.photoVisibleRects.collectAsState()
    val editMode by viewModel.editMode.collectAsState()
    val activePhotoIndex by viewModel.activePhotoIndex.collectAsState()
    val cropMode by viewModel.cropMode.collectAsState()
    val panEnabled by viewModel.panEnabled.collectAsState()
    val activeLayout by viewModel.activeLayout.collectAsState()
    val availableLayouts by viewModel.availableLayouts.collectAsState()

    // UI-only state
    var showPicker by remember { mutableStateOf(false) }
    var addingToCollage by remember { mutableStateOf(false) }
    var viewportRect by remember { mutableStateOf<RectF?>(null) }
    var isZoomed by remember { mutableStateOf(false) }
    var editableView by remember { mutableStateOf<EditableTouchImageView?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var lastBitmapPoint by remember { mutableStateOf(PointF(0f, 0f)) }
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

    BackHandler(enabled = editMode || cropMode) {
        if (cropMode) {
            viewModel.cancelCrop()
            editableView?.cropMode = false
        } else {
            viewModel.exitEditMode()
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
                val wasAdding = addingToCollage
                viewModel.onPhotosConfirmed(uris, addingToCollage)
                addingToCollage = false
                if (uris.size == 1 && !wasAdding) {
                    showEditor = true
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
            bitmapService = viewModel.bitmapService,
            onDone = { editedBitmap ->
                viewModel.setBackgroundBitmap(editedBitmap)
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
                                        viewModel.enterEditMode(index)
                                        this.editMode = true
                                        this.activePhotoIndex = index
                                    }
                                }
                                true
                            }
                            setOnClickListener { view ->
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (viewModel.editMode.value && this.photoBounds.isNotEmpty()) {
                                    val index = photoIndexAt(lastBitmapPoint.x, lastBitmapPoint.y)
                                    if (index >= 0) {
                                        viewModel.setActivePhoto(index)
                                        this.activePhotoIndex = index
                                    }
                                }
                            }
                            onReorderComplete = { newOrder -> viewModel.onReorder(newOrder) }
                            onPanComplete = { newPanX, newPanY -> viewModel.onPanComplete(newPanX, newPanY) }
                            getTransformedBitmap = { index -> viewModel.getTransformedBitmap(index) }
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
                        viewModel.cancelCrop()
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
                        viewModel.confirmCrop(normalizedCrop)
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
                        viewModel.flipHorizontal()
                    }) {
                        Icon(Icons.Default.Flip, "Flip horizontal", tint = if (activeTransform.flipH) Color(0xFF4FC3F7) else Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.flipVertical()
                    }) {
                        Icon(
                            Icons.Default.Flip, "Flip vertical",
                            tint = if (activeTransform.flipV) Color(0xFF4FC3F7) else Color.White,
                            modifier = Modifier.graphicsLayer { rotationZ = 90f }
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.rotate90()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90\u00B0", tint = if (activeTransform.rotation != 0) Color(0xFF4FC3F7) else Color.White)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.enterCropMode()
                    }) {
                        Icon(Icons.Default.Crop, "Crop", tint = if (activeTransform.cropRect != null) Color(0xFF4FC3F7) else Color.White)
                    }
                    // Pan toggle: only for grid layouts with cropped photos
                    if (activeLayout != null && activeLayout!!.aspectRatio != 0f) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.togglePan()
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
                        viewModel.removeActivePhoto()
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
                        viewModel.selectLayout(layout)
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

