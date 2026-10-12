package com.bcd.technotes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcd.technotes.feature.canvas.CanvasScreen
import com.bcd.technotes.feature.canvas.CanvasViewModel
import com.bcd.technotes.feature.editor.WholeImageEditScreen
import com.bcd.technotes.feature.picker.PhotoPickerScreen

@Composable
fun AppNavigation() {
    val canvasViewModel: CanvasViewModel = hiltViewModel()
    var showPicker by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var isAddingToCollage by remember { mutableStateOf(false) }
    var hasPickerBeenShown by remember { mutableStateOf(false) }

    val backgroundBitmap by canvasViewModel.backgroundBitmap.collectAsState()

    if (showPicker) {
        PhotoPickerScreen(
            onPhotosConfirmed = { uris ->
                canvasViewModel.onPhotosConfirmed(uris, isAddingToCollage)
                showPicker = false
                if (uris.size == 1 && !isAddingToCollage) {
                    showEditor = true
                }
                isAddingToCollage = false
            },
            onDismiss = {
                showPicker = false
                isAddingToCollage = false
            }
        )
        return
    }

    if (showEditor && backgroundBitmap != null) {
        WholeImageEditScreen(
            sourceBitmap = backgroundBitmap!!,
            bitmapService = canvasViewModel.bitmapService,
            onDone = { editedBitmap ->
                canvasViewModel.setBackgroundBitmap(editedBitmap)
                showEditor = false
            },
            onCancel = { showEditor = false }
        )
        return
    }

    CanvasScreen(
        viewModel = canvasViewModel,
        onOpenPicker = { showPicker = true; hasPickerBeenShown = true },
        onOpenEditor = { showEditor = true },
        autoOpenPicker = !hasPickerBeenShown
    )
}
