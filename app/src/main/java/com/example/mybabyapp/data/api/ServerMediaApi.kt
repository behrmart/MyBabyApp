package com.example.mybabyapp.data.api

import com.example.mybabyapp.data.model.ServerMediaItem
import retrofit2.Response
import retrofit2.http.GET

interface ServerMediaApi {
    @GET("server-media")
    suspend fun getServerMedia(): Response<List<ServerMediaItem>>
}
