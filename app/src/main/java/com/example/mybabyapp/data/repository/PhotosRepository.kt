package com.example.mybabyapp.data.repository

import com.example.mybabyapp.data.api.PhotosApi
import com.example.mybabyapp.data.model.PhotoAlbum
import com.example.mybabyapp.data.model.PhotoSummary
import retrofit2.Response

class PhotosRepository(
    private val photosApi: PhotosApi,
    private val baseUrl: String
) {
    suspend fun getAlbums(): List<PhotoAlbum> {
        val response = photosApi.getAlbums()
        return requireBody(
            response = response,
            defaultMessage = "Unable to load albums"
        )
    }

    suspend fun getPhotos(albumId: Int? = null): List<PhotoSummary> {
        val response = photosApi.getPhotos(albumId = albumId)
        return requireBody(
            response = response,
            defaultMessage = "Unable to load photos",
            badRequestMessage = "Invalid album selection"
        )
    }

    fun streamUrl(id: Int): String = "${baseUrl}photos/$id/stream"

    private fun <T> requireBody(
        response: Response<T>,
        defaultMessage: String,
        badRequestMessage: String? = null
    ): T {
        if (!response.isSuccessful) {
            throw RepositoryException(
                when (response.code()) {
                    400 -> badRequestMessage ?: defaultMessage
                    else -> defaultMessage
                }
            )
        }

        return response.body() ?: throw RepositoryException("Empty response from server")
    }
}
