package com.bcd.technotes.sandbox.experiments

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.bcd.technotes.sandbox.util.updateDoubleTapScale
import com.ortiz.touchview.TouchImageView
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
    onPhotosConfirmed: (List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var currentView by remember { mutableStateOf(PickerView.RECENT) }
    var previousView by remember { mutableStateOf(PickerView.RECENT) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var recentPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var albumPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var previewPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var previewStartIndex by remember { mutableIntStateOf(0) }

    // Selection state
    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val selectedPhotoCache = remember { mutableMapOf<Long, Photo>() }
    val selectedPhotoList = remember(selectedPhotoIds) {
        selectedPhotoIds.mapNotNull { selectedPhotoCache[it] }
    }

    fun navigateBack() {
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

    fun toggleSelection(photo: Photo) {
        selectedPhotoIds = if (selectedPhotoIds.contains(photo.id)) {
            selectedPhotoCache.remove(photo.id)
            selectedPhotoIds - photo.id
        } else {
            selectedPhotoCache[photo.id] = photo
            selectedPhotoIds + photo.id
        }
    }

    LaunchedEffect(Unit) {
        recentPhotos = queryAllPhotos(context)
    }

    LaunchedEffect(selectedAlbum) {
        selectedAlbum?.let { album ->
            albumPhotos = queryPhotos(context, album.id)
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
                            onPhotoTap = { photo -> toggleSelection(photo) },
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
                            if (albums.isEmpty()) {
                                albums = queryAlbums(context)
                            }
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
                            onPhotoTap = { photo -> toggleSelection(photo) },
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
                            onPhotoTap = { photo -> toggleSelection(photo) },
                            onPhotoLongPress = { photo -> toggleSelection(photo) }
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
                    onRemovePhoto = { id ->
                        selectedPhotoCache.remove(id)
                        selectedPhotoIds = selectedPhotoIds - id
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    onAlbumSelected: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
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
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() })
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(album.coverUri)
                .size(Size(512, 512))
                .precision(Precision.INEXACT)
                .memoryCacheKey("album_cover_${album.id}")
                .build(),
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(placeholderColor),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        Text(
            text = "${album.photoCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selectedPhotoIds: Set<Long>,
    onPhotoTap: (Photo) -> Unit,
    onPhotoLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val haptic = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(photos.size, key = { photos[it].id }) { index ->
            val photo = photos[index]
            val isSelected = selectedPhotoIds.contains(photo.id)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPhotoTap(photo) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPhotoLongPress(index)
                        }
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.uri)
                        .size(Size(256, 256))
                        .precision(Precision.INEXACT)
                        .memoryCacheKey("thumb_${photo.id}")
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(placeholderColor),
                    modifier = Modifier.fillMaxSize()
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x4D000000))
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPhotosStrip(
    selectedPhotos: List<Photo>,
    onRemovePhoto: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val selectedIds = remember(selectedPhotos) { selectedPhotos.map { it.id }.toSet() }

    // Keep removed photos alive for exit animation
    val photoCache = remember { mutableMapOf<Long, Photo>() }
    var exitingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Cache all photos we see so we can display them during exit
    selectedPhotos.forEach { photoCache[it.id] = it }

    // Detect removals by diffing against previous selected IDs
    val prevIds = remember { mutableStateOf(selectedIds) }
    if (prevIds.value != selectedIds) {
        val removed = prevIds.value - selectedIds - exitingIds
        if (removed.isNotEmpty()) {
            exitingIds = exitingIds + removed
        }
        prevIds.value = selectedIds
    }

    // Display list: active photos + exiting photos (kept alive for animation)
    val displayPhotos = remember(selectedPhotos, exitingIds) {
        selectedPhotos + exitingIds.mapNotNull { photoCache[it] }
    }

    // Auto-scroll to end when new photos added
    LaunchedEffect(selectedPhotos.size) {
        if (selectedPhotos.isNotEmpty()) {
            kotlinx.coroutines.delay(50)
            val target = scrollState.maxValue
            val start = scrollState.value
            val distance = target - start
            if (distance > 0) {
                val startTime = withFrameNanos { it }
                val durationNs = 350_000_000L // 350ms
                var t = 0f
                while (t < 1f) {
                    val frameTime = withFrameNanos { it }
                    val elapsed = frameTime - startTime
                    t = (elapsed.toFloat() / durationNs).coerceIn(0f, 1f)
                    val eased = 1f - (1f - t) * (1f - t)
                    scrollState.scrollTo(start + (distance * eased).toInt())
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        displayPhotos.forEach { photo ->
            val isExiting = exitingIds.contains(photo.id)

            key(photo.id) {
                var progress by remember { mutableFloatStateOf(if (isExiting) 1f else 0f) }

                if (isExiting) {
                    // Exit: shrink + slide down + fade out
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        val durationNs = 150_000_000L // 150ms
                        var done = false
                        while (!done) {
                            withFrameNanos { frameTime ->
                                val elapsed = frameTime - startTime
                                val t = (elapsed.toFloat() / durationNs).coerceIn(0f, 1f)
                                progress = 1f - (t * t)
                                if (t >= 1f) done = true
                            }
                        }
                        exitingIds = exitingIds - photo.id
                        photoCache.remove(photo.id)
                    }
                } else {
                    // Entry: bounce in with overshoot
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        val durationNs = 300_000_000L // 300ms
                        var done = false
                        while (!done) {
                            withFrameNanos { frameTime ->
                                val elapsed = frameTime - startTime
                                val t = (elapsed.toFloat() / durationNs).coerceIn(0f, 1f)
                                progress = if (t < 1f) {
                                    val t2 = t - 1f
                                    t2 * t2 * (3f * t2 + 2f) + 1f
                                } else 1f
                                if (t >= 1f) done = true
                            }
                        }
                    }
                }

                Box(modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = progress
                        scaleY = progress
                        translationY = (1f - progress) * 100f
                        alpha = progress
                    }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .size(Size(104, 104))
                            .precision(Precision.INEXACT)
                            .memoryCacheKey("strip_${photo.id}")
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                    )
                    if (!isExiting) {
                        SmallFloatingActionButton(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRemovePhoto(photo.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp),
                            shape = CircleShape,
                            containerColor = Color(0xCC000000),
                            contentColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoPreview(
    photos: List<Photo>,
    startIndex: Int,
    selectedPhotoIds: Set<Long>,
    onPhotoTap: (Photo) -> Unit,
    onPhotoLongPress: (Photo) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { photos.size }
    )
    var isZoomed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]

            AndroidView(
                factory = { ctx ->
                    TouchImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        maxZoom = 10f
                        minZoom = 1f
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setOnClickListener { view -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onPhotoTap(photo) }
                        setOnLongClickListener { view ->
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onPhotoLongPress(photo)
                            true
                        }
                        setOnTouchImageViewListener(object : com.ortiz.touchview.OnTouchImageViewListener {
                            override fun onMove() {
                                isZoomed = this@apply.isZoomed
                            }
                        })
                    }
                },
                update = { view ->
                    view.resetZoom()
                    isZoomed = false
                    // Downsample to ~2x screen size for smooth pager swipes with large photos
                    val dw = view.resources.displayMetrics.widthPixels
                    val dh = view.resources.displayMetrics.heightPixels
                    try {
                        view.context.contentResolver.openInputStream(photo.uri)?.use { stream ->
                            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                            var sampleSize = 1
                            while (opts.outWidth / sampleSize > dw * 2 || opts.outHeight / sampleSize > dh * 2) {
                                sampleSize *= 2
                            }
                            view.context.contentResolver.openInputStream(photo.uri)?.use { stream2 ->
                                val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                val bitmap = android.graphics.BitmapFactory.decodeStream(stream2, null, decodeOpts)
                                if (bitmap != null) view.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {
                        view.setImageURI(photo.uri)
                    }
                    // doOnLayout fires after TouchImageView processes the new bitmap's layout,
                    // guaranteed even for newly created views that don't have dimensions yet
                    view.doOnLayout { view.updateDoubleTapScale() }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${photos.size}",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        )

        // Selection badge on current photo
        if (selectedPhotoIds.contains(photos.getOrNull(pagerState.currentPage)?.id)) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(32.dp)
            )
        }
    }
}

private suspend fun queryAllPhotos(context: Context): List<Photo> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<Photo>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            photos.add(
                Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                )
            )
        }
    }
    photos
}

private suspend fun queryAlbums(context: Context): List<Album> = withContext(Dispatchers.IO) {
    val albums = LinkedHashMap<Long, Album>()
    val projection = arrayOf(
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media._ID
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

        while (cursor.moveToNext()) {
            val bucketId = cursor.getLong(bucketIdCol)
            val existing = albums[bucketId]
            if (existing != null) {
                albums[bucketId] = existing.copy(photoCount = existing.photoCount + 1)
            } else {
                val imageId = cursor.getLong(idCol)
                val coverUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
                )
                albums[bucketId] = Album(
                    id = bucketId,
                    name = cursor.getString(bucketNameCol) ?: "Unknown",
                    coverUri = coverUri,
                    photoCount = 1
                )
            }
        }
    }
    albums.values.toList()
}

private suspend fun queryPhotos(context: Context, bucketId: Long): List<Photo> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<Photo>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
    val selectionArgs = arrayOf(bucketId.toString())
    val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            photos.add(
                Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                )
            )
        }
    }
    photos
}
