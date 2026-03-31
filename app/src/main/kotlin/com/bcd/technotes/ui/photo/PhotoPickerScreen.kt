package com.bcd.technotes.ui.photo

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcd.technotes.data.model.Album
import com.bcd.technotes.data.model.Photo
import com.bcd.technotes.data.model.PickerView
import com.bcd.technotes.viewmodel.PhotoPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerScreen(
    onPhotosConfirmed: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PhotoPickerViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current

    // ViewModel state
    val recentPhotos by viewModel.recentPhotos.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val albumPhotos by viewModel.albumPhotos.collectAsState()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsState()
    val selectedPhotoList by viewModel.selectedPhotos.collectAsState()

    // UI navigation state (stays local)
    var currentView by remember { mutableStateOf(PickerView.RECENT) }
    var previousView by remember { mutableStateOf(PickerView.RECENT) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var previewPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var previewStartIndex by remember { mutableIntStateOf(0) }

    fun navigateBack() {
        when (currentView) {
            PickerView.RECENT -> onDismiss()
            PickerView.ALBUMS -> currentView = PickerView.RECENT
            PickerView.ALBUM_PHOTOS -> {
                selectedAlbum = null
                currentView = PickerView.ALBUMS
            }
            PickerView.PHOTO_PREVIEW -> currentView = previousView
        }
    }

    LaunchedEffect(selectedAlbum) {
        selectedAlbum?.let { album ->
            viewModel.loadAlbumPhotos(album.id)
        }
    }

    BackHandler { navigateBack() }

    val title = when (currentView) {
        PickerView.RECENT -> if (selectedPhotoIds.isNotEmpty()) "${selectedPhotoIds.size} selected" else "Recent"
        PickerView.ALBUMS -> "Albums"
        PickerView.ALBUM_PHOTOS -> selectedAlbum?.name ?: "Photos"
        PickerView.PHOTO_PREVIEW -> "Preview"
    }

    Scaffold(
        topBar = {
            if (currentView != PickerView.PHOTO_PREVIEW) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        modifier = Modifier.navigationBarsPadding()
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    PickerView.RECENT -> {
                        PhotoGrid(
                            photos = recentPhotos,
                            selectedPhotoIds = selectedPhotoIds,
                            onPhotoTap = { photo -> viewModel.toggleSelection(photo) },
                            onPhotoLongPress = { index ->
                                previewPhotos = recentPhotos
                                previewStartIndex = index
                                previousView = PickerView.RECENT
                                currentView = PickerView.PHOTO_PREVIEW
                            }
                        )
                    }
                    PickerView.ALBUMS -> {
                        LaunchedEffect(Unit) {
                            viewModel.loadAlbums()
                        }
                        AlbumGrid(
                            albums = albums,
                            onAlbumSelected = { album ->
                                selectedAlbum = album
                                currentView = PickerView.ALBUM_PHOTOS
                            }
                        )
                    }
                    PickerView.ALBUM_PHOTOS -> {
                        PhotoGrid(
                            photos = albumPhotos,
                            selectedPhotoIds = selectedPhotoIds,
                            onPhotoTap = { photo -> viewModel.toggleSelection(photo) },
                            onPhotoLongPress = { index ->
                                previewPhotos = albumPhotos
                                previewStartIndex = index
                                previousView = PickerView.ALBUM_PHOTOS
                                currentView = PickerView.PHOTO_PREVIEW
                            }
                        )
                    }
                    PickerView.PHOTO_PREVIEW -> {
                        PhotoPreview(
                            photos = previewPhotos,
                            startIndex = previewStartIndex,
                            selectedPhotoIds = selectedPhotoIds,
                            onPhotoTap = { photo -> viewModel.toggleSelection(photo) },
                            onPhotoLongPress = { photo -> viewModel.toggleSelection(photo) }
                        )
                    }
                }

                // FAB row — sits above the thumbnail strip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                    if (selectedPhotoIds.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPhotosConfirmed(selectedPhotoList.map { it.uri }) },
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedPhotoIds.size == 1) Icons.Default.Edit else Icons.Default.Dashboard,
                                    contentDescription = if (selectedPhotoIds.size == 1) "Edit" else "Collage"
                                )
                                if (selectedPhotoIds.size > 1) {
                                    Text(
                                        text = "${selectedPhotoIds.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-10).dp)
                                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    when (currentView) {
                        PickerView.RECENT -> {
                            FloatingActionButton(
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); currentView = PickerView.ALBUMS },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "Albums")
                            }
                        }
                        PickerView.ALBUMS -> {
                            FloatingActionButton(
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); currentView = PickerView.RECENT },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = "Recent")
                            }
                        }
                        PickerView.ALBUM_PHOTOS -> {
                            FloatingActionButton(
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); navigateBack() },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "Albums")
                            }
                        }
                        PickerView.PHOTO_PREVIEW -> {
                            FloatingActionButton(
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); currentView = previousView },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = "Back to grid")
                            }
                        }
                    }
                }
            }

            // Selected photos thumbnail strip
            if (selectedPhotoList.isNotEmpty()) {
                SelectedPhotosStrip(
                    selectedPhotos = selectedPhotoList,
                    onRemovePhoto = { id -> viewModel.removePhoto(id) }
                )
            }
        }
    }
}


