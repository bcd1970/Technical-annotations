package com.bcd.technotes.ui.photo

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri,
    val photoCount: Int
)

data class Photo(
    val id: Long,
    val uri: Uri
)

private enum class PickerView { RECENT, ALBUMS, ALBUM_PHOTOS, PHOTO_PREVIEW }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoPickerScreen(
    onPhotoSelected: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentView by remember { mutableStateOf(PickerView.RECENT) }
    var previousView by remember { mutableStateOf(PickerView.RECENT) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var recentPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var albumPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var previewPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var previewStartIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        recentPhotos = queryAllPhotos(context)
    }

    LaunchedEffect(selectedAlbum) {
        selectedAlbum?.let { album ->
            albumPhotos = queryPhotos(context, album.id)
        }
    }

    BackHandler {
        when (currentView) {
            PickerView.RECENT -> onDismiss()
            PickerView.ALBUMS -> currentView = PickerView.RECENT
            PickerView.ALBUM_PHOTOS -> {
                selectedAlbum = null
                albumPhotos = emptyList()
                currentView = PickerView.ALBUMS
            }
            PickerView.PHOTO_PREVIEW -> currentView = previousView
        }
    }

    val title = when (currentView) {
        PickerView.RECENT -> "Recent"
        PickerView.ALBUMS -> "Albums"
        PickerView.ALBUM_PHOTOS -> selectedAlbum?.name ?: "Photos"
        PickerView.PHOTO_PREVIEW -> "Preview"
    }

    Scaffold(
        topBar = {
            if (currentView != PickerView.PHOTO_PREVIEW) {
                TopAppBar(
                    title = {
                        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when (currentView) {
                                PickerView.RECENT -> onDismiss()
                                PickerView.ALBUMS -> currentView = PickerView.RECENT
                                PickerView.ALBUM_PHOTOS -> {
                                    selectedAlbum = null
                                    albumPhotos = emptyList()
                                    currentView = PickerView.ALBUMS
                                }
                                PickerView.PHOTO_PREVIEW -> currentView = previousView
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        floatingActionButton = {
            when (currentView) {
                PickerView.RECENT -> {
                    FloatingActionButton(
                        onClick = { currentView = PickerView.ALBUMS },
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.Folder, contentDescription = "Albums") }
                }
                PickerView.ALBUMS -> {
                    FloatingActionButton(
                        onClick = { currentView = PickerView.RECENT },
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.GridView, contentDescription = "Recent") }
                }
                PickerView.ALBUM_PHOTOS -> {
                    FloatingActionButton(
                        onClick = {
                            selectedAlbum = null
                            albumPhotos = emptyList()
                            currentView = PickerView.ALBUMS
                        },
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.Folder, contentDescription = "Albums") }
                }
                PickerView.PHOTO_PREVIEW -> {
                    FloatingActionButton(
                        onClick = { currentView = previousView },
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.GridView, contentDescription = "Back to grid") }
                }
            }
        },
        modifier = Modifier.navigationBarsPadding()
    ) { paddingValues ->
        when (currentView) {
            PickerView.RECENT -> {
                PhotoGrid(
                    photos = recentPhotos,
                    onPhotoTap = onPhotoSelected,
                    onPhotoLongPress = { index ->
                        previewPhotos = recentPhotos
                        previewStartIndex = index
                        previousView = PickerView.RECENT
                        currentView = PickerView.PHOTO_PREVIEW
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            PickerView.ALBUMS -> {
                LaunchedEffect(Unit) {
                    if (albums.isEmpty()) { albums = queryAlbums(context) }
                }
                AlbumGrid(
                    albums = albums,
                    onAlbumSelected = { album ->
                        selectedAlbum = album
                        currentView = PickerView.ALBUM_PHOTOS
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            PickerView.ALBUM_PHOTOS -> {
                PhotoGrid(
                    photos = albumPhotos,
                    onPhotoTap = onPhotoSelected,
                    onPhotoLongPress = { index ->
                        previewPhotos = albumPhotos
                        previewStartIndex = index
                        previousView = PickerView.ALBUM_PHOTOS
                        currentView = PickerView.PHOTO_PREVIEW
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            PickerView.PHOTO_PREVIEW -> {
                PhotoPreview(
                    photos = previewPhotos,
                    startIndex = previewStartIndex,
                    onPhotoLongPress = { uri -> onPhotoSelected(uri) }
                )
            }
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, onAlbumSelected: (Album) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items = albums, key = { it.id }) { album ->
            AlbumItem(album = album, onClick = { onAlbumSelected(album) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumItem(album: Album, onClick: () -> Unit) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).combinedClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(album.coverUri).size(Size(512, 512)).precision(Precision.INEXACT)
                .memoryCacheKey("album_cover_${album.id}").build(),
            contentDescription = album.name, contentScale = ContentScale.Crop,
            placeholder = ColorPainter(placeholderColor),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp))
        )
        Text(album.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        Text("${album.photoCount}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(photos: List<Photo>, onPhotoTap: (Uri) -> Unit, onPhotoLongPress: (Int) -> Unit, modifier: Modifier = Modifier) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(photos.size, key = { photos[it].id }) { index ->
            val photo = photos[index]
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.uri).size(Size(256, 256)).precision(Precision.INEXACT)
                    .memoryCacheKey("thumb_${photo.id}").build(),
                contentDescription = null, contentScale = ContentScale.Crop,
                placeholder = ColorPainter(placeholderColor),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .combinedClickable(onClick = { onPhotoTap(photo.uri) }, onLongClick = { onPhotoLongPress(index) })
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoPreview(photos: List<Photo>, startIndex: Int, onPhotoLongPress: (Uri) -> Unit) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { photos.size })
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val photo = photos[page]
            val zoomableState = rememberZoomableState()
            if (pagerState.settledPage != page) {
                LaunchedEffect(Unit) { zoomableState.resetZoom() }
            }
            Box(modifier = Modifier.fillMaxSize().combinedClickable(onClick = {}, onLongClick = { onPhotoLongPress(photo.uri) })) {
                ZoomableAsyncImage(model = photo.uri, contentDescription = null,
                    state = rememberZoomableImageState(zoomableState), modifier = Modifier.fillMaxSize())
            }
        }
        Text("${pagerState.currentPage + 1} / ${photos.size}", color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp))
    }
}

private suspend fun queryAllPhotos(context: Context): List<Photo> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<Photo>()
    context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID), null, null,
        "${MediaStore.Images.Media.DATE_MODIFIED} DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            photos.add(Photo(id, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)))
        }
    }
    photos
}

private suspend fun queryAlbums(context: Context): List<Album> = withContext(Dispatchers.IO) {
    val albums = LinkedHashMap<Long, Album>()
    context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media._ID),
        null, null, "${MediaStore.Images.Media.DATE_MODIFIED} DESC")?.use { cursor ->
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val bucketId = cursor.getLong(bucketIdCol)
            val existing = albums[bucketId]
            if (existing != null) {
                albums[bucketId] = existing.copy(photoCount = existing.photoCount + 1)
            } else {
                albums[bucketId] = Album(bucketId, cursor.getString(bucketNameCol) ?: "Unknown",
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol)), 1)
            }
        }
    }
    albums.values.toList()
}

private suspend fun queryPhotos(context: Context, bucketId: Long): List<Photo> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<Photo>()
    context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID), "${MediaStore.Images.Media.BUCKET_ID} = ?",
        arrayOf(bucketId.toString()), "${MediaStore.Images.Media.DATE_MODIFIED} DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            photos.add(Photo(id, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)))
        }
    }
    photos
}
