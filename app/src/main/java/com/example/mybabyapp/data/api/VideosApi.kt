package com.example.mybabyapp.data.api

import com.example.mybabyapp.data.model.CreateCommentRequest
import com.example.mybabyapp.data.model.CreatedCommentResponse
import com.example.mybabyapp.data.model.OkResponse
import com.example.mybabyapp.data.model.VideoComment
import com.example.mybabyapp.data.model.VideoDetail
import com.example.mybabyapp.data.model.VideoSummary
import com.example.mybabyapp.data.model.ViewIncrementRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VideosApi {
    @GET("videos")
    suspend fun getVideos(): Response<List<VideoSummary>>

    @GET("videos/{id}")
    suspend fun getVideoDetail(
        @Path("id") id: Int
    ): Response<VideoDetail>

    @GET("videos/{id}/comments")
    suspend fun getComments(
        @Path("id") id: Int
    ): Response<List<VideoComment>>

    @POST("videos/{id}/comments")
    suspend fun addComment(
        @Path("id") id: Int,
        @Body request: CreateCommentRequest
    ): Response<CreatedCommentResponse>

    @POST("videos/{id}/view")
    suspend fun incrementView(
        @Path("id") id: Int,
        @Body request: ViewIncrementRequest = ViewIncrementRequest()
    ): Response<OkResponse>
}
