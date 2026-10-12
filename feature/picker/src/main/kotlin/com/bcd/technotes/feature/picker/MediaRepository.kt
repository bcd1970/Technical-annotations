package com.bcd.technotes.feature.picker

interface MediaRepository {
    suspend fun queryAllPhotos(): List<Photo>
    suspend fun queryAlbums(): List<Album>
    suspend fun queryAlbumPhotos(bucketId: Long): List<Photo>
}
