package com.example.mybabyapp.data.api

import com.example.mybabyapp.data.model.PhotoAlbum
import com.example.mybabyapp.data.model.PhotoSummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PhotosApi {
    @GET("photos/albums")
    suspend fun getAlbums(): Response<List<PhotoAlbum>>

    @GET("photos")
    suspend fun getPhotos(
        @Query("albumId") albumId: Int? = null
    ): Response<List<PhotoSummary>>
}
