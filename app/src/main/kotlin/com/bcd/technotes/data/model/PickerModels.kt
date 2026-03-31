package com.bcd.technotes.data.model

import android.net.Uri

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

enum class PickerView { RECENT, ALBUMS, ALBUM_PHOTOS, PHOTO_PREVIEW }
