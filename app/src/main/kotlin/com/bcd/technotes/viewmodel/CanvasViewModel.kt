package com.bcd.technotes.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bcd.technotes.core.model.CollageLayout
import com.bcd.technotes.core.model.layoutsForCount
import com.bcd.technotes.data.model.PhotoTransform
import com.bcd.technotes.data.service.BitmapService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor(
    val bitmapService: BitmapService,
    application: Application
) : AndroidViewModel(application) {

    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap.asStateFlow()

    private val _selectedUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedUris: StateFlow<List<Uri>> = _selectedUris.asStateFlow()

    private val _photoTransforms = MutableStateFlow<List<PhotoTransform>>(emptyList())
    val photoTransforms: StateFlow<List<PhotoTransform>> = _photoTransforms.asStateFlow()

    private val _photoBounds = MutableStateFlow<List<RectF>>(emptyList())
    val photoBounds: StateFlow<List<RectF>> = _photoBounds.asStateFlow()

    private val _photoVisibleRects = MutableStateFlow<List<RectF>>(emptyList())
    val photoVisibleRects: StateFlow<List<RectF>> = _photoVisibleRects.asStateFlow()

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    private val _activePhotoIndex = MutableStateFlow(-1)
    val activePhotoIndex: StateFlow<Int> = _activePhotoIndex.asStateFlow()

    private val _cropMode = MutableStateFlow(false)
    val cropMode: StateFlow<Boolean> = _cropMode.asStateFlow()

    private val _panEnabled = MutableStateFlow(false)
    val panEnabled: StateFlow<Boolean> = _panEnabled.asStateFlow()

    private val _activeLayout = MutableStateFlow<CollageLayout?>(null)
    val activeLayout: StateFlow<CollageLayout?> = _activeLayout.asStateFlow()

    private val _availableLayouts = MutableStateFlow<List<CollageLayout>>(emptyList())
    val availableLayouts: StateFlow<List<CollageLayout>> = _availableLayouts.asStateFlow()

    private val bitmapCache = mutableMapOf<Uri, Bitmap>()
    private val canvasWidth = application.resources.displayMetrics.widthPixels * 2

    init {
        // Recompute layouts when photo count changes
        viewModelScope.launch {
            _selectedUris.collect { uris ->
                if (uris.size >= 2) {
                    _availableLayouts.value = layoutsForCount(uris.size)
                    val current = _activeLayout.value
                    if (current == null || current.cells.size != uris.size) {
                        _activeLayout.value = _availableLayouts.value.first()
                    }
                } else {
                    _availableLayouts.value = emptyList()
                    _activeLayout.value = null
                }
            }
        }

        // Decode + stitch collage when inputs change
        viewModelScope.launch {
            combine(
                _selectedUris,
                _photoTransforms,
                _activeLayout
            ) { uris, transforms, layout -> Triple(uris, transforms, layout) }
                .collect { (uris, transforms, layout) ->
                    if (uris.size < 2 || layout == null) {
                        bitmapCache.keys.retainAll(uris.toSet())
                        if (uris.isEmpty()) _photoBounds.value = emptyList()
                        return@collect
                    }

                    val missing = uris.filter { it !in bitmapCache }
                    if (missing.isNotEmpty()) {
                        missing.forEach { uri ->
                            bitmapService.decodeBitmap(uri)?.let { bmp ->
                                bitmapCache[uri] = bmp
                            }
                        }
                    }
                    bitmapCache.keys.retainAll(uris.toSet())

                    val bitmaps = uris.mapNotNull { bitmapCache[it] }
                    if (bitmaps.size < 2) return@collect

                    val result = bitmapService.stitchCollage(bitmaps, transforms, layout, canvasWidth)
                    _backgroundBitmap.value = result?.bitmap
                    _photoBounds.value = result?.photoBounds ?: emptyList()
                    _photoVisibleRects.value = result?.photoVisibleRects ?: emptyList()
                }
        }
    }

    fun loadSinglePhoto(uri: Uri) {
        viewModelScope.launch {
            _backgroundBitmap.value = bitmapService.decodeBitmap(uri)
        }
    }

    fun onPhotosConfirmed(uris: List<Uri>, isAddingToCollage: Boolean) {
        if (isAddingToCollage) {
            _selectedUris.value = _selectedUris.value + uris
            _photoTransforms.value = _photoTransforms.value + uris.map { PhotoTransform() }
        } else {
            _editMode.value = false
            _activePhotoIndex.value = -1
            if (uris.size == 1) {
                loadSinglePhoto(uris.first())
                _selectedUris.value = emptyList()
                _photoTransforms.value = emptyList()
            } else {
                _backgroundBitmap.value = null
                _selectedUris.value = uris
                _photoTransforms.value = uris.map { PhotoTransform() }
            }
        }
    }

    fun setBackgroundBitmap(bitmap: Bitmap) {
        _backgroundBitmap.value = bitmap
    }

    fun enterEditMode(photoIndex: Int) {
        _editMode.value = true
        _activePhotoIndex.value = photoIndex
    }

    fun exitEditMode() {
        _editMode.value = false
        _activePhotoIndex.value = -1
        _panEnabled.value = false
    }

    fun setActivePhoto(index: Int) {
        _activePhotoIndex.value = index
    }

    fun flipHorizontal() {
        updateActiveTransform { it.copy(flipH = !it.flipH) }
    }

    fun flipVertical() {
        updateActiveTransform { it.copy(flipV = !it.flipV) }
    }

    fun rotate90() {
        updateActiveTransform { it.copy(rotation = (it.rotation + 90) % 360) }
    }

    fun enterCropMode() {
        val i = _activePhotoIndex.value
        if (i >= 0 && _photoTransforms.value[i].cropRect != null) {
            updateActiveTransform { it.copy(cropRect = null) }
        }
        _cropMode.value = true
    }

    fun confirmCrop(normalizedCropRect: RectF) {
        updateActiveTransform { it.copy(cropRect = normalizedCropRect) }
        _cropMode.value = false
    }

    fun cancelCrop() {
        _cropMode.value = false
    }

    fun togglePan() {
        _panEnabled.value = !_panEnabled.value
    }

    fun onReorder(newOrder: List<Int>) {
        val viewActiveIndex = _activePhotoIndex.value
        _selectedUris.value = newOrder.map { _selectedUris.value[it] }
        _photoTransforms.value = newOrder.map { _photoTransforms.value[it] }
        _activePhotoIndex.value = newOrder.indexOf(viewActiveIndex)
    }

    fun onPanComplete(panX: Float, panY: Float) {
        val i = _activePhotoIndex.value
        if (i in _photoTransforms.value.indices) {
            updateActiveTransform { it.copy(panX = panX, panY = panY) }
        }
    }

    fun selectLayout(layout: CollageLayout) {
        _activeLayout.value = layout
    }

    fun removeActivePhoto() {
        val i = _activePhotoIndex.value
        val newUris = _selectedUris.value.toMutableList().apply { removeAt(i) }
        val newTransforms = _photoTransforms.value.toMutableList().apply { removeAt(i) }
        if (newUris.size < 2) {
            _editMode.value = false
            _activePhotoIndex.value = -1
            if (newUris.size == 1) {
                loadSinglePhoto(newUris.first())
                _selectedUris.value = emptyList()
                _photoTransforms.value = emptyList()
            } else {
                _selectedUris.value = emptyList()
                _photoTransforms.value = emptyList()
                _backgroundBitmap.value = null
            }
        } else {
            _selectedUris.value = newUris
            _photoTransforms.value = newTransforms
            _activePhotoIndex.value = i.coerceAtMost(newUris.size - 1)
        }
    }

    fun getTransformedBitmap(index: Int): Bitmap? {
        val uri = _selectedUris.value.getOrNull(index)
        val bmp = if (uri != null) bitmapCache[uri] else null
        return if (bmp != null) {
            bitmapService.applyTransform(bmp, _photoTransforms.value.getOrElse(index) { PhotoTransform() })
        } else null
    }

    val isCollage: Boolean
        get() = _selectedUris.value.size >= 2 && _backgroundBitmap.value != null

    private fun updateActiveTransform(update: (PhotoTransform) -> PhotoTransform) {
        val i = _activePhotoIndex.value
        if (i in _photoTransforms.value.indices) {
            _photoTransforms.value = _photoTransforms.value.toMutableList().apply {
                this[i] = update(this[i])
            }
        }
    }
}
