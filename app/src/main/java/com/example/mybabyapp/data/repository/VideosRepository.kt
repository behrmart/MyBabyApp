package com.example.mybabyapp.data.repository

import com.example.mybabyapp.data.api.VideosApi
import com.example.mybabyapp.data.model.CreateCommentRequest
import com.example.mybabyapp.data.model.VideoComment
import com.example.mybabyapp.data.model.VideoDetail
import com.example.mybabyapp.data.model.VideoSummary
import retrofit2.Response

class VideosRepository(
    private val videosApi: VideosApi,
    private val baseUrl: String
) {
    suspend fun getMediaCatalog(): List<VideoSummary> {
        val response = videosApi.getVideos()
        return requireBody(
            response = response,
            defaultMessage = "Unable to load media"
        )
    }

    suspend fun getMediaDetail(id: Int): VideoDetail {
        val response = videosApi.getVideoDetail(id)
        return requireBody(
            response = response,
            defaultMessage = "Unable to load media details",
            notFoundMessage = "Media item not found"
        )
    }

    suspend fun getComments(id: Int): List<VideoComment> {
        val response = videosApi.getComments(id)
        return requireBody(
            response = response,
            defaultMessage = "Unable to load comments"
        )
    }

    suspend fun addComment(
        id: Int,
        content: String
    ) {
        val response = videosApi.addComment(
            id = id,
            request = CreateCommentRequest(content = content)
        )
        requireBody(
            response = response,
            defaultMessage = "Unable to post comment",
            badRequestMessage = "Comment is required"
        )
    }

    suspend fun incrementView(id: Int) {
        val response = videosApi.incrementView(id)
        requireBody(
            response = response,
            defaultMessage = "Unable to record view"
        )
    }

    fun streamUrl(id: Int): String = "${baseUrl}videos/$id/stream"

    private fun <T> requireBody(
        response: Response<T>,
        defaultMessage: String,
        notFoundMessage: String? = null,
        badRequestMessage: String? = null
    ): T {
        if (!response.isSuccessful) {
            throw RepositoryException(
                when (response.code()) {
                    400 -> badRequestMessage ?: defaultMessage
                    404 -> notFoundMessage ?: defaultMessage
                    else -> defaultMessage
                }
            )
        }

        return response.body() ?: throw RepositoryException("Empty response from server")
    }
}

class RepositoryException(message: String) : Exception(message)
