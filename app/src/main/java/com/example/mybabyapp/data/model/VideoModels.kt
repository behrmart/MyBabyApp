package com.example.mybabyapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoSummary(
    val id: Int,
    val title: String,
    val views: Int,
    val description: String? = null,
    val mimeType: String,
    val createdAt: String
)

@Serializable
data class VideoDetail(
    val id: Int,
    val title: String,
    val filename: String,
    val mimeType: String,
    val description: String? = null,
    val views: Int,
    val createdAt: String
)

@Serializable
data class VideoComment(
    val id: Int,
    val content: String,
    val createdAt: String,
    val username: String
)

@Serializable
data class CreateCommentRequest(
    val content: String
)

@Serializable
data class CreatedCommentResponse(
    val id: Int,
    val content: String,
    val createdAt: String,
    val videoId: Int,
    val userId: Int
)

@Serializable
data class ViewIncrementRequest(
    val ignored: String? = null
)

@Serializable
data class OkResponse(
    val ok: Boolean
)

enum class MediaCategory {
    VIDEOS,
    GIFS
}

fun VideoSummary.matchesCategory(category: MediaCategory): Boolean {
    return when (category) {
        MediaCategory.VIDEOS -> mimeType.startsWith("video/")
        MediaCategory.GIFS -> mimeType == "image/gif"
    }
}

fun VideoDetail.isVideoItem(): Boolean = mimeType.startsWith("video/")

fun VideoDetail.isGifItem(): Boolean = mimeType == "image/gif"
