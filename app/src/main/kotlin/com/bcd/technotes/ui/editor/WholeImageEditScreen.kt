package com.bcd.technotes.ui.editor

import android.graphics.Bitmap
import android.graphics.RectF
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bcd.technotes.core.model.PhotoAdjustments
import com.bcd.technotes.data.model.EditTool
import com.bcd.technotes.data.model.ImageEditState
import com.bcd.technotes.data.service.BitmapService
import com.bcd.technotes.ui.util.WholeImageEditView
import com.bcd.technotes.ui.util.updateDoubleTapScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

@Composable
fun WholeImageEditScreen(
    sourceBitmap: Bitmap,
    bitmapService: BitmapService,
    onDone: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var currentSource by remember { mutableStateOf(sourceBitmap) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var baseDetailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editState by remember { mutableStateOf(ImageEditState()) }
    var activeTool by remember { mutableStateOf(EditTool.NONE) }
    var editView by remember { mutableStateOf<WholeImageEditView?>(null) }

    BackHandler {
        when (activeTool) {
            EditTool.CROP -> {
                activeTool = EditTool.NONE
                editView?.cropMode = false
            }
            EditTool.ADJUST -> {
                activeTool = EditTool.NONE
            }
            EditTool.NONE -> onCancel()
        }
    }

    LaunchedEffect(currentSource, editState.flipH, editState.flipV, editState.rotation, editState.cropRect) {
        displayBitmap = withContext(Dispatchers.IO) {
            bitmapService.applyEditState(currentSource, editState)
        }
    }

    LaunchedEffect(displayBitmap) {
        val bmp = displayBitmap ?: return@LaunchedEffect
        editView?.let { view ->
            view.setImageBitmap(bmp)
            view.post { view.updateDoubleTapScale() }
        }
        baseDetailBitmap = null
        baseDetailBitmap = withContext(Dispatchers.Default) {
            bitmapService.computeBaseDetailTexture(bmp)
        }
    }

    LaunchedEffect(editView, displayBitmap) {
        val view = editView ?: return@LaunchedEffect
        val bmp = displayBitmap ?: return@LaunchedEffect

        snapshotFlow { Pair(editState.adjustments, baseDetailBitmap) }
            .conflate()
            .collect { (adj, bd) ->
                if (adj.isDefault) {
                    view.setRenderEffect(null)
                    view.setImageBitmap(bmp)
                } else {
                    val adjusted = withContext(Dispatchers.Default) {
                        bitmapService.applyAdjustments(bmp, adj, bd)
                    }
                    view.setRenderEffect(null)
                    view.setImageBitmap(adjusted)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (displayBitmap != null) {
            AndroidView(
                factory = { ctx ->
                    WholeImageEditView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        maxZoom = 10f
                        minZoom = 1f
                        setImageBitmap(displayBitmap)
                        editView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            when (activeTool) {
                EditTool.CROP -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = {
                            activeTool = EditTool.NONE
                            editView?.cropMode = false
                        }) {
                            Icon(Icons.Default.Close, "Cancel crop", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val view = editView ?: return@IconButton
                            val bmp = displayBitmap ?: return@IconButton
                            val rect = view.cropRect
                            if (!rect.isEmpty) {
                                val normRect = RectF(
                                    rect.left / bmp.width,
                                    rect.top / bmp.height,
                                    rect.right / bmp.width,
                                    rect.bottom / bmp.height
                                )
                                editState = editState.copy(cropRect = normRect)
                                currentSource = bitmapService.applyEditState(currentSource, editState)
                                editState = ImageEditState(adjustments = editState.adjustments)
                            }
                            activeTool = EditTool.NONE
                            view.cropMode = false
                        }) {
                            Icon(Icons.Default.Check, "Confirm crop", tint = Color.Green)
                        }
                    }
                }

                EditTool.ADJUST -> {
                    AdjustmentPanel(
                        adjustments = editState.adjustments,
                        onAdjustmentChange = { newAdj ->
                            editState = editState.copy(adjustments = newAdj)
                        },
                        onReset = {
                            editState = editState.copy(adjustments = PhotoAdjustments())
                        },
                        onClose = {
                            activeTool = EditTool.NONE
                        }
                    )
                }

                EditTool.NONE -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { editState = editState.copy(flipV = !editState.flipV) }) {
                            Icon(Icons.Default.Flip, "Flip vertical", tint = Color.White)
                        }
                        IconButton(onClick = { editState = editState.copy(flipH = !editState.flipH) }) {
                            Icon(
                                Icons.Default.Flip, "Flip horizontal", tint = Color.White,
                                modifier = Modifier.graphicsLayer(rotationZ = 90f)
                            )
                        }
                        IconButton(onClick = { editState = editState.copy(rotation = (editState.rotation + 90) % 360) }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90\u00B0", tint = Color.White)
                        }
                        IconButton(onClick = {
                            activeTool = EditTool.CROP
                            editView?.let {
                                it.cropMode = true
                                it.initCropRect()
                            }
                        }) {
                            Icon(Icons.Default.Crop, "Crop", tint = Color.White)
                        }
                        IconButton(onClick = {
                            activeTool = EditTool.ADJUST
                        }) {
                            Icon(Icons.Default.Tune, "Adjust", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val bmp = displayBitmap ?: currentSource
                            if (!editState.adjustments.isDefault) {
                                onDone(bitmapService.applyAdjustments(bmp, editState.adjustments, baseDetailBitmap))
                            } else {
                                onDone(bmp)
                            }
                        }) {
                            Icon(Icons.Default.Check, "Done", tint = Color.Green)
                        }
                    }
                }
            }
        }
    }
}
