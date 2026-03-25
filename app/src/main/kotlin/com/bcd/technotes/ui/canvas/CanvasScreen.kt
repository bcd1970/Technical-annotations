package com.bcd.technotes.ui.canvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import com.ortiz.touchview.TouchImageView

@Composable
fun CanvasScreen() {
    val context = LocalContext.current

    var backgroundBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showPicker = true
    }

    if (showPicker) {
        PhotoPickerScreen(
            onPhotoSelected = { uri ->
                showPicker = false
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    backgroundBitmap = BitmapFactory.decodeStream(stream)
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
        backgroundBitmap?.let { bitmap ->
            AndroidView(
                factory = { ctx ->
                    TouchImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageBitmap(bitmap)
                        maxZoom = 10f
                        minZoom = 1f
                        post {
                            val d = drawable ?: return@post
                            val imgW = d.intrinsicWidth.toFloat()
                            val imgH = d.intrinsicHeight.toFloat()
                            val vW = width.toFloat()
                            val vH = height.toFloat()
                            if (imgW > 0 && imgH > 0 && vW > 0 && vH > 0) {
                                doubleTapScale = maxOf(vW / imgW, vH / imgH) / minOf(vW / imgW, vH / imgH)
                            }
                        }
                    }
                },
                update = { view ->
                    view.setImageBitmap(bitmap)
                    view.post {
                        val d = view.drawable ?: return@post
                        val imgW = d.intrinsicWidth.toFloat()
                        val imgH = d.intrinsicHeight.toFloat()
                        val vW = view.width.toFloat()
                        val vH = view.height.toFloat()
                        if (imgW > 0 && imgH > 0 && vW > 0 && vH > 0) {
                            view.doubleTapScale = maxOf(vW / imgW, vH / imgH) / minOf(vW / imgW, vH / imgH)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (backgroundBitmap == null) {
            Text(
                text = "Tap the camera button to load a photo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
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
