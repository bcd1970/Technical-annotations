package com.bcd.technotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcd.technotes.data.model.Album
import com.bcd.technotes.data.model.Photo
import com.bcd.technotes.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoPickerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _recentPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val recentPhotos: StateFlow<List<Photo>> = _recentPhotos.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _albumPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val albumPhotos: StateFlow<List<Photo>> = _albumPhotos.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    private val selectedPhotoCache = mutableMapOf<Long, Photo>()

    private val _selectedPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val selectedPhotos: StateFlow<List<Photo>> = _selectedPhotos.asStateFlow()

    init {
        loadRecentPhotos()
    }

    fun loadRecentPhotos() {
        viewModelScope.launch {
            _recentPhotos.value = mediaRepository.queryAllPhotos()
        }
    }

    fun loadAlbums() {
        if (_albums.value.isNotEmpty()) return
        viewModelScope.launch {
            _albums.value = mediaRepository.queryAlbums()
        }
    }

    fun loadAlbumPhotos(bucketId: Long) {
        viewModelScope.launch {
            _albumPhotos.value = mediaRepository.queryAlbumPhotos(bucketId)
        }
    }

    fun toggleSelection(photo: Photo) {
        val currentIds = _selectedPhotoIds.value
        if (currentIds.contains(photo.id)) {
            selectedPhotoCache.remove(photo.id)
            _selectedPhotoIds.value = currentIds - photo.id
        } else {
            selectedPhotoCache[photo.id] = photo
            _selectedPhotoIds.value = currentIds + photo.id
        }
        refreshSelectedPhotos()
    }

    fun removePhoto(id: Long) {
        selectedPhotoCache.remove(id)
        _selectedPhotoIds.value = _selectedPhotoIds.value - id
        refreshSelectedPhotos()
    }

    fun clearSelection() {
        selectedPhotoCache.clear()
        _selectedPhotoIds.value = emptySet()
        _selectedPhotos.value = emptyList()
    }

    private fun refreshSelectedPhotos() {
        _selectedPhotos.value = _selectedPhotoIds.value.mapNotNull { selectedPhotoCache[it] }
    }
}
