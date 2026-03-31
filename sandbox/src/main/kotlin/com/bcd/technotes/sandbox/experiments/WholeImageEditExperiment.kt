package com.bcd.technotes.sandbox.experiments

import android.graphics.Bitmap
import android.graphics.Matrix
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bcd.technotes.sandbox.util.WholeImageEditView
import com.bcd.technotes.sandbox.util.updateDoubleTapScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageEditState(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null
)

enum class EditTool { NONE, CROP }

@Composable
fun WholeImageEditScreen(
    sourceBitmap: Bitmap,
    onDone: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var currentSource by remember { mutableStateOf(sourceBitmap) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editState by remember { mutableStateOf(ImageEditState()) }
    var activeTool by remember { mutableStateOf(EditTool.NONE) }
    var editView by remember { mutableStateOf<WholeImageEditView?>(null) }

    BackHandler {
        if (activeTool == EditTool.CROP) {
            activeTool = EditTool.NONE
            editView?.cropMode = false
        } else {
            onCancel()
        }
    }

    // Recompute display bitmap when source or edit state changes
    LaunchedEffect(currentSource, editState) {
        displayBitmap = withContext(Dispatchers.IO) {
            applyEditState(currentSource, editState)
        }
    }

    // Update the view when display bitmap changes
    LaunchedEffect(displayBitmap) {
        displayBitmap?.let { bmp ->
            editView?.let { view ->
                view.setImageBitmap(bmp)
                view.post { view.updateDoubleTapScale() }
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

        // Bottom toolbar
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
                                currentSource = applyEditState(currentSource, editState)
                                editState = ImageEditState()
                            }
                            activeTool = EditTool.NONE
                            view.cropMode = false
                        }) {
                            Icon(Icons.Default.Check, "Confirm crop", tint = Color.Green)
                        }
                    }
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
                            Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90°", tint = Color.White)
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
                        // Done button
                        IconButton(onClick = { onDone(displayBitmap ?: currentSource) }) {
                            Icon(Icons.Default.Check, "Done", tint = Color.Green)
                        }
                    }
                }
            }
        }
    }
}

private fun applyEditState(source: Bitmap, state: ImageEditState): Bitmap {
    var current = source

    if (state.cropRect != null) {
        val cr = state.cropRect
        val x = (cr.left * current.width).toInt().coerceIn(0, current.width - 1)
        val y = (cr.top * current.height).toInt().coerceIn(0, current.height - 1)
        val w = ((cr.right - cr.left) * current.width).toInt().coerceAtMost(current.width - x).coerceAtLeast(1)
        val h = ((cr.bottom - cr.top) * current.height).toInt().coerceAtMost(current.height - y).coerceAtLeast(1)
        current = Bitmap.createBitmap(current, x, y, w, h)
    }

    if (state.flipH || state.flipV || state.rotation != 0) {
        val matrix = Matrix()
        if (state.flipH) matrix.postScale(1f, -1f, current.width / 2f, current.height / 2f)
        if (state.flipV) matrix.postScale(-1f, 1f, current.width / 2f, current.height / 2f)
        if (state.rotation != 0) matrix.postRotate(state.rotation.toFloat(), current.width / 2f, current.height / 2f)
        current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
    }

    return current
}
