package com.bcd.technotes.data.repository

import com.bcd.technotes.data.model.Album
import com.bcd.technotes.data.model.Photo

interface MediaRepository {
    suspend fun queryAllPhotos(): List<Photo>
    suspend fun queryAlbums(): List<Album>
    suspend fun queryAlbumPhotos(bucketId: Long): List<Photo>
}
