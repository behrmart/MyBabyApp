package com.example.mybabyapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoAlbum(
    val id: Int,
    val name: String,
    val description: String? = null,
    val createdAt: String,
    val photoCount: Int
)

@Serializable
data class PhotoAlbumRef(
    val id: Int,
    val name: String
)

@Serializable
data class PhotoSummary(
    val id: Int,
    val title: String,
    val filename: String,
    val mimeType: String,
    val createdAt: String,
    val albumId: Int? = null,
    val album: PhotoAlbumRef? = null
)
