package com.example.mybabyapp.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.mybabyapp.data.api.AdminApi
import com.example.mybabyapp.data.model.AdminPasswordChangeRequest
import com.example.mybabyapp.data.model.AdminUser
import com.example.mybabyapp.data.model.CreateAdminAlbumRequest
import com.example.mybabyapp.data.model.OkResponse
import com.example.mybabyapp.data.model.PhotoAlbum
import java.io.IOException
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import retrofit2.Response

class AdminRepository(
    private val adminApi: AdminApi,
    private val contentResolver: ContentResolver
) {
    fun describeSelectedFile(uri: Uri): String {
        return queryDisplayName(uri) ?: "selected file"
    }

    suspend fun getUsers(): List<AdminUser> {
        val response = adminApi.getUsers()
        return requireBody(
            response = response,
            defaultMessage = "Unable to load users"
        )
    }

    suspend fun getAlbums(): List<PhotoAlbum> {
        val response = adminApi.getAlbums()
        return requireBody(
            response = response,
            defaultMessage = "Unable to load albums"
        )
    }

    suspend fun changeUserPassword(
        id: Int,
        password: String
    ) {
        val response = adminApi.changeUserPassword(
            id = id,
            request = AdminPasswordChangeRequest(password = password)
        )
        requireBody(
            response = response,
            defaultMessage = "Unable to change password",
            badRequestMessage = "Password must include uppercase, lowercase, number, and symbol",
            notFoundMessage = "User not found"
        )
    }

    suspend fun createAlbum(
        name: String,
        description: String?
    ) {
        val response = adminApi.createAlbum(
            request = CreateAdminAlbumRequest(
                name = name,
                description = description
            )
        )
        requireSuccess(
            response = response,
            defaultMessage = "Unable to create album",
            badRequestMessage = "Album name is required",
            conflictMessage = "Album name already exists"
        )
    }

    suspend fun uploadVideo(
        title: String,
        description: String?,
        fileUri: Uri
    ) {
        val mimeType = contentResolver.getType(fileUri)
        if (mimeType != null && !mimeType.startsWith("video/") && mimeType != "image/gif") {
            throw RepositoryException("Select a video or GIF file")
        }

        val response = adminApi.uploadVideo(
            title = title.toPlainTextRequestBody(),
            description = description?.takeIf { it.isNotBlank() }?.toPlainTextRequestBody(),
            file = createFilePart(fileUri)
        )
        requireSuccess(
            response = response,
            defaultMessage = "Unable to upload media",
            badRequestMessage = "Title and file are required"
        )
    }

    suspend fun uploadPhotoToAlbum(
        albumId: Int,
        title: String?,
        fileUri: Uri
    ) {
        val mimeType = contentResolver.getType(fileUri)
        if (mimeType != null && !mimeType.startsWith("image/")) {
            throw RepositoryException("Select an image file")
        }

        val response = adminApi.uploadPhotoToAlbum(
            albumId = albumId,
            title = title?.takeIf { it.isNotBlank() }?.toPlainTextRequestBody(),
            file = createFilePart(fileUri)
        )
        requireSuccess(
            response = response,
            defaultMessage = "Unable to upload photo",
            badRequestMessage = "Photo upload is invalid",
            notFoundMessage = "Album not found"
        )
    }

    suspend fun deleteVideo(id: Int) {
        val response = adminApi.deleteVideo(id)
        requireBody(
            response = response,
            defaultMessage = "Unable to delete media",
            notFoundMessage = "Media item not found"
        )
    }

    suspend fun deletePhoto(id: Int) {
        val response = adminApi.deletePhoto(id)
        requireBody(
            response = response,
            defaultMessage = "Unable to delete photo",
            notFoundMessage = "Photo not found"
        )
    }

    private fun createFilePart(fileUri: Uri): MultipartBody.Part {
        val fileName = queryDisplayName(fileUri) ?: "upload"
        val mimeType = contentResolver.getType(fileUri).orEmpty()
            .ifBlank { "application/octet-stream" }
        val requestBody = ContentUriRequestBody(
            contentResolver = contentResolver,
            uri = fileUri,
            mediaType = mimeType.toMediaTypeOrNull()
        )
        return MultipartBody.Part.createFormData(
            name = "file",
            filename = fileName,
            body = requestBody
        )
    }

    private fun <T> requireBody(
        response: Response<T>,
        defaultMessage: String,
        badRequestMessage: String? = null,
        notFoundMessage: String? = null,
        conflictMessage: String? = null
    ): T {
        if (!response.isSuccessful) {
            throw RepositoryException(
                httpMessage(
                    code = response.code(),
                    defaultMessage = defaultMessage,
                    badRequestMessage = badRequestMessage,
                    notFoundMessage = notFoundMessage,
                    conflictMessage = conflictMessage
                )
            )
        }

        return response.body() ?: throw RepositoryException("Empty response from server")
    }

    private fun requireSuccess(
        response: Response<ResponseBody>,
        defaultMessage: String,
        badRequestMessage: String? = null,
        notFoundMessage: String? = null,
        conflictMessage: String? = null
    ) {
        if (!response.isSuccessful) {
            throw RepositoryException(
                httpMessage(
                    code = response.code(),
                    defaultMessage = defaultMessage,
                    badRequestMessage = badRequestMessage,
                    notFoundMessage = notFoundMessage,
                    conflictMessage = conflictMessage
                )
            )
        }
    }

    private fun httpMessage(
        code: Int,
        defaultMessage: String,
        badRequestMessage: String?,
        notFoundMessage: String?,
        conflictMessage: String?
    ): String {
        return when (code) {
            400 -> badRequestMessage ?: defaultMessage
            403 -> "Admin access required"
            404 -> notFoundMessage ?: defaultMessage
            409 -> conflictMessage ?: defaultMessage
            else -> defaultMessage
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }

            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex == -1) {
                null
            } else {
                cursor.getString(columnIndex)
            }
        }
    }

    private fun String.toPlainTextRequestBody(): RequestBody {
        return toRequestBody("text/plain".toMediaTypeOrNull())
    }
}

private class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType?
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long {
        return runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { assetFileDescriptor ->
                if (assetFileDescriptor.length >= 0) {
                    assetFileDescriptor.length
                } else {
                    -1L
                }
            } ?: -1L
        }.getOrDefault(-1L)
    }

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open selected file")

        inputStream.use { stream ->
            sink.writeAll(stream.source())
        }
    }
}
