package com.example.mybabyapp.data.api

import com.example.mybabyapp.data.model.AdminPasswordChangeRequest
import com.example.mybabyapp.data.model.AdminUser
import com.example.mybabyapp.data.model.CreateAdminAlbumRequest
import com.example.mybabyapp.data.model.OkResponse
import com.example.mybabyapp.data.model.PhotoAlbum
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AdminApi {
    @GET("admin/users")
    suspend fun getUsers(): Response<List<AdminUser>>

    @POST("admin/users/{id}/password")
    suspend fun changeUserPassword(
        @Path("id") id: Int,
        @Body request: AdminPasswordChangeRequest
    ): Response<OkResponse>

    @GET("admin/albums")
    suspend fun getAlbums(): Response<List<PhotoAlbum>>

    @POST("admin/albums")
    suspend fun createAlbum(
        @Body request: CreateAdminAlbumRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("admin/videos")
    suspend fun uploadVideo(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @Multipart
    @POST("admin/albums/{id}/photos")
    suspend fun uploadPhotoToAlbum(
        @Path("id") albumId: Int,
        @Part("title") title: RequestBody?,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @DELETE("admin/videos/{id}")
    suspend fun deleteVideo(
        @Path("id") id: Int
    ): Response<OkResponse>

    @DELETE("admin/photos/{id}")
    suspend fun deletePhoto(
        @Path("id") id: Int
    ): Response<OkResponse>
}
