package com.bcd.technotes.ui.canvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
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

    // Decode bitmap off main thread
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
            selectedUris.size >= 2 -> {
                CollagePlaceholder(photoCount = selectedUris.size)
            }
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

@Composable
private fun CollagePlaceholder(photoCount: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$photoCount photos selected for collage",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
