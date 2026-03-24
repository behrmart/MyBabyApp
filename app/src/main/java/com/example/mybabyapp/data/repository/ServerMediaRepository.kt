package com.example.mybabyapp.data.repository

import com.example.mybabyapp.data.api.ServerMediaApi
import com.example.mybabyapp.data.model.ServerMediaItem
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Response

class ServerMediaRepository(
    private val serverMediaApi: ServerMediaApi,
    private val baseUrl: String
) {
    private var cachedCatalog: List<ServerMediaItem> = emptyList()

    suspend fun getCatalog(): List<ServerMediaItem> {
        val response = serverMediaApi.getServerMedia()
        val catalog = requireBody(
            response = response,
            defaultMessage = "Unable to load external media"
        )
        cachedCatalog = catalog
        return catalog
    }

    fun getCachedItem(id: String): ServerMediaItem? {
        return cachedCatalog.firstOrNull { item -> item.id == id }
    }

    suspend fun getItem(id: String): ServerMediaItem {
        getCachedItem(id)?.let { cachedItem ->
            return cachedItem
        }

        val catalog = getCatalog()
        return catalog.firstOrNull { item -> item.id == id }
            ?: throw RepositoryException("External media item not found")
    }

    fun streamUrl(id: String): String {
        return baseUrl.toHttpUrl()
            .newBuilder()
            .addPathSegment("server-media")
            .addPathSegment("stream")
            .addPathSegment(id)
            .build()
            .toString()
    }

    private fun <T> requireBody(
        response: Response<T>,
        defaultMessage: String
    ): T {
        if (!response.isSuccessful) {
            throw RepositoryException(defaultMessage)
        }

        return response.body() ?: throw RepositoryException("Empty response from server")
    }
}
