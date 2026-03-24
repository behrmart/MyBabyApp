package com.example.mybabyapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerMediaItem(
    val id: String,
    val name: String,
    val relativePath: String,
    val mimeType: String,
    val type: String,
    val size: Long,
    val modifiedAt: String
)

enum class ServerMediaFilter {
    ALL,
    VIDEO,
    IMAGE,
    AUDIO
}

fun ServerMediaItem.matchesFilter(filter: ServerMediaFilter): Boolean {
    return when (filter) {
        ServerMediaFilter.ALL -> true
        ServerMediaFilter.VIDEO -> isVideoType()
        ServerMediaFilter.IMAGE -> isImageType()
        ServerMediaFilter.AUDIO -> isAudioType()
    }
}

fun ServerMediaItem.isVideoType(): Boolean = type.equals("video", ignoreCase = true)

fun ServerMediaItem.isImageType(): Boolean = type.equals("image", ignoreCase = true)

fun ServerMediaItem.isAudioType(): Boolean = type.equals("audio", ignoreCase = true)
