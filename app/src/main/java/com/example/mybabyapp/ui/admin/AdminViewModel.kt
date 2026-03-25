package com.example.mybabyapp.ui.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.model.AdminUser
import com.example.mybabyapp.data.model.PhotoAlbum
import com.example.mybabyapp.data.model.PhotoSummary
import com.example.mybabyapp.data.model.VideoSummary
import com.example.mybabyapp.data.repository.AdminRepository
import com.example.mybabyapp.data.repository.PhotosRepository
import com.example.mybabyapp.data.repository.RepositoryException
import com.example.mybabyapp.data.repository.VideosRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = true,
    val users: List<AdminUser> = emptyList(),
    val albums: List<PhotoAlbum> = emptyList(),
    val videos: List<VideoSummary> = emptyList(),
    val photos: List<PhotoSummary> = emptyList(),
    val selectedUserId: Int? = null,
    val passwordDraft: String = "",
    val albumNameDraft: String = "",
    val albumDescriptionDraft: String = "",
    val videoTitleDraft: String = "",
    val videoDescriptionDraft: String = "",
    val selectedVideoUri: Uri? = null,
    val selectedVideoName: String? = null,
    val selectedPhotoAlbumId: Int? = null,
    val photoTitleDraft: String = "",
    val selectedPhotoUri: Uri? = null,
    val selectedPhotoName: String? = null,
    val isChangingPassword: Boolean = false,
    val isCreatingAlbum: Boolean = false,
    val isUploadingVideo: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val deletingVideoIds: Set<Int> = emptySet(),
    val deletingPhotoIds: Set<Int> = emptySet(),
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val passwordErrorMessage: String? = null,
    val albumErrorMessage: String? = null,
    val videoUploadErrorMessage: String? = null,
    val photoUploadErrorMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val photosRepository: PhotosRepository,
    private val videosRepository: VideosRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                val dashboardData = coroutineScope {
                    val users = async { adminRepository.getUsers() }
                    val albums = async { adminRepository.getAlbums() }
                    val videos = async { videosRepository.getMediaCatalog() }
                    val photos = async { photosRepository.getPhotos() }

                    AdminDashboardData(
                        users = users.await(),
                        albums = albums.await(),
                        videos = videos.await(),
                        photos = photos.await()
                    )
                }

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        users = dashboardData.users,
                        albums = dashboardData.albums,
                        videos = dashboardData.videos,
                        photos = dashboardData.photos,
                        selectedUserId = currentState.selectedUserId
                            ?.takeIf { selectedId ->
                                dashboardData.users.any { user -> user.id == selectedId }
                            }
                            ?: dashboardData.users.firstOrNull()?.id,
                        selectedPhotoAlbumId = currentState.selectedPhotoAlbumId
                            ?.takeIf { selectedId ->
                                dashboardData.albums.any { album -> album.id == selectedId }
                            }
                            ?: dashboardData.albums.firstOrNull()?.id,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to load admin dashboard"
                        )
                    )
                }
            }
        }
    }

    fun selectUser(userId: Int) {
        mutableUiState.update { currentState ->
            currentState.copy(
                selectedUserId = userId,
                passwordErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                passwordDraft = value,
                passwordErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun submitPasswordChange() {
        val currentState = uiState.value
        if (currentState.isChangingPassword) {
            return
        }

        val userId = currentState.selectedUserId
        if (userId == null) {
            mutableUiState.update { state ->
                state.copy(passwordErrorMessage = "Select a user")
            }
            return
        }

        val password = currentState.passwordDraft.trim()
        if (password.isBlank()) {
            mutableUiState.update { state ->
                state.copy(passwordErrorMessage = "Password is required")
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isChangingPassword = true,
                    passwordErrorMessage = null,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                adminRepository.changeUserPassword(
                    id = userId,
                    password = password
                )

                val username = uiState.value.users.firstOrNull { user -> user.id == userId }?.username
                    ?: "user"

                mutableUiState.update { state ->
                    state.copy(
                        isChangingPassword = false,
                        passwordDraft = "",
                        actionMessage = "Password updated for $username"
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        isChangingPassword = false,
                        passwordErrorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to change password"
                        )
                    )
                }
            }
        }
    }

    fun onAlbumNameChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                albumNameDraft = value,
                albumErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun onAlbumDescriptionChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                albumDescriptionDraft = value,
                albumErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun createAlbum() {
        val currentState = uiState.value
        if (currentState.isCreatingAlbum) {
            return
        }

        val name = currentState.albumNameDraft.trim()
        if (name.isBlank()) {
            mutableUiState.update { state ->
                state.copy(albumErrorMessage = "Album name is required")
            }
            return
        }

        val description = currentState.albumDescriptionDraft.trim().ifBlank { null }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isCreatingAlbum = true,
                    albumErrorMessage = null,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                adminRepository.createAlbum(
                    name = name,
                    description = description
                )
                val albums = adminRepository.getAlbums()
                val createdAlbumId = albums.firstOrNull { album -> album.name == name }?.id

                mutableUiState.update { state ->
                    state.copy(
                        isCreatingAlbum = false,
                        albums = albums,
                        selectedPhotoAlbumId = state.selectedPhotoAlbumId
                            ?.takeIf { selectedId ->
                                albums.any { album -> album.id == selectedId }
                            }
                            ?: createdAlbumId
                            ?: albums.firstOrNull()?.id,
                        albumNameDraft = "",
                        albumDescriptionDraft = "",
                        actionMessage = "Album created"
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        isCreatingAlbum = false,
                        albumErrorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to create album"
                        )
                    )
                }
            }
        }
    }

    fun onVideoTitleChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                videoTitleDraft = value,
                videoUploadErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun onVideoDescriptionChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                videoDescriptionDraft = value,
                videoUploadErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun onVideoFileSelected(uri: Uri?) {
        if (uri == null) {
            return
        }

        mutableUiState.update { currentState ->
            currentState.copy(
                selectedVideoUri = uri,
                selectedVideoName = adminRepository.describeSelectedFile(uri),
                videoUploadErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun uploadVideo() {
        val currentState = uiState.value
        if (currentState.isUploadingVideo) {
            return
        }

        val title = currentState.videoTitleDraft.trim()
        if (title.isBlank()) {
            mutableUiState.update { state ->
                state.copy(videoUploadErrorMessage = "Title is required")
            }
            return
        }

        val fileUri = currentState.selectedVideoUri
        if (fileUri == null) {
            mutableUiState.update { state ->
                state.copy(videoUploadErrorMessage = "Choose a video or GIF file")
            }
            return
        }

        val description = currentState.videoDescriptionDraft.trim().ifBlank { null }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isUploadingVideo = true,
                    videoUploadErrorMessage = null,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                adminRepository.uploadVideo(
                    title = title,
                    description = description,
                    fileUri = fileUri
                )
                val videos = videosRepository.getMediaCatalog()

                mutableUiState.update { state ->
                    state.copy(
                        isUploadingVideo = false,
                        videos = videos,
                        videoTitleDraft = "",
                        videoDescriptionDraft = "",
                        selectedVideoUri = null,
                        selectedVideoName = null,
                        actionMessage = "Media uploaded"
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        isUploadingVideo = false,
                        videoUploadErrorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to upload media"
                        )
                    )
                }
            }
        }
    }

    fun selectPhotoAlbum(albumId: Int) {
        mutableUiState.update { currentState ->
            currentState.copy(
                selectedPhotoAlbumId = albumId,
                photoUploadErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun onPhotoTitleChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                photoTitleDraft = value,
                photoUploadErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun onPhotoFileSelected(uri: Uri?) {
        if (uri == null) {
            return
        }

        mutableUiState.update { currentState ->
            currentState.copy(
                selectedPhotoUri = uri,
                selectedPhotoName = adminRepository.describeSelectedFile(uri),
                photoUploadErrorMessage = null,
                actionMessage = null
            )
        }
    }

    fun uploadPhoto() {
        val currentState = uiState.value
        if (currentState.isUploadingPhoto) {
            return
        }

        val albumId = currentState.selectedPhotoAlbumId
        if (albumId == null) {
            mutableUiState.update { state ->
                state.copy(photoUploadErrorMessage = "Select an album")
            }
            return
        }

        val fileUri = currentState.selectedPhotoUri
        if (fileUri == null) {
            mutableUiState.update { state ->
                state.copy(photoUploadErrorMessage = "Choose a photo")
            }
            return
        }

        val title = currentState.photoTitleDraft.trim().ifBlank { null }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isUploadingPhoto = true,
                    photoUploadErrorMessage = null,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                adminRepository.uploadPhotoToAlbum(
                    albumId = albumId,
                    title = title,
                    fileUri = fileUri
                )

                val refreshedData = coroutineScope {
                    val albums = async { adminRepository.getAlbums() }
                    val photos = async { photosRepository.getPhotos() }

                    RefreshedPhotoData(
                        albums = albums.await(),
                        photos = photos.await()
                    )
                }

                mutableUiState.update { state ->
                    state.copy(
                        isUploadingPhoto = false,
                        albums = refreshedData.albums,
                        photos = refreshedData.photos,
                        selectedPhotoAlbumId = state.selectedPhotoAlbumId
                            ?.takeIf { selectedId ->
                                refreshedData.albums.any { album -> album.id == selectedId }
                            }
                            ?: refreshedData.albums.firstOrNull()?.id,
                        photoTitleDraft = "",
                        selectedPhotoUri = null,
                        selectedPhotoName = null,
                        actionMessage = "Photo uploaded"
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        isUploadingPhoto = false,
                        photoUploadErrorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to upload photo"
                        )
                    )
                }
            }
        }
    }

    fun deleteVideo(id: Int) {
        if (uiState.value.deletingVideoIds.contains(id)) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    deletingVideoIds = state.deletingVideoIds + id,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                adminRepository.deleteVideo(id)
                val videos = videosRepository.getMediaCatalog()

                mutableUiState.update { state ->
                    state.copy(
                        deletingVideoIds = state.deletingVideoIds - id,
                        videos = videos,
                        actionMessage = "Media deleted"
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        deletingVideoIds = state.deletingVideoIds - id,
                        errorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to delete media"
                        )
                    )
                }
            }
        }
    }

    fun deletePhoto(id: Int) {
        if (uiState.value.deletingPhotoIds.contains(id)) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    deletingPhotoIds = state.deletingPhotoIds + id,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            try {
                adminRepository.deletePhoto(id)

                val refreshedData = coroutineScope {
                    val albums = async { adminRepository.getAlbums() }
                    val photos = async { photosRepository.getPhotos() }

                    RefreshedPhotoData(
                        albums = albums.await(),
                        photos = photos.await()
                    )
                }

                mutableUiState.update { state ->
                    state.copy(
                        deletingPhotoIds = state.deletingPhotoIds - id,
                        albums = refreshedData.albums,
                        photos = refreshedData.photos,
                        selectedPhotoAlbumId = state.selectedPhotoAlbumId
                            ?.takeIf { selectedId ->
                                refreshedData.albums.any { album -> album.id == selectedId }
                            }
                            ?: refreshedData.albums.firstOrNull()?.id,
                        actionMessage = "Photo deleted"
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        deletingPhotoIds = state.deletingPhotoIds - id,
                        errorMessage = adminErrorMessage(
                            exception = exception,
                            fallback = "Unable to delete photo"
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            adminRepository: AdminRepository,
            photosRepository: PhotosRepository,
            videosRepository: VideosRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                        return AdminViewModel(
                            adminRepository = adminRepository,
                            photosRepository = photosRepository,
                            videosRepository = videosRepository
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

private data class AdminDashboardData(
    val users: List<AdminUser>,
    val albums: List<PhotoAlbum>,
    val videos: List<VideoSummary>,
    val photos: List<PhotoSummary>
)

private data class RefreshedPhotoData(
    val albums: List<PhotoAlbum>,
    val photos: List<PhotoSummary>
)

private fun adminErrorMessage(
    exception: Throwable,
    fallback: String
): String {
    return when (exception) {
        is RepositoryException -> exception.message ?: fallback
        is IOException -> "Unable to reach server"
        else -> fallback
    }
}
