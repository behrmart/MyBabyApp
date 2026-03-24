package com.example.mybabyapp.ui.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.model.PhotoAlbum
import com.example.mybabyapp.data.model.PhotoSummary
import com.example.mybabyapp.data.repository.PhotosRepository
import com.example.mybabyapp.data.repository.RepositoryException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotosUiState(
    val isLoading: Boolean = true,
    val albums: List<PhotoAlbum> = emptyList(),
    val selectedAlbumId: Int? = null,
    val photos: List<PhotoSummary> = emptyList(),
    val errorMessage: String? = null
)

class PhotosViewModel(
    private val photosRepository: PhotosRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        load(
            albumId = mutableUiState.value.selectedAlbumId,
            refreshAlbums = true
        )
    }

    fun selectAlbum(albumId: Int?) {
        if (albumId == mutableUiState.value.selectedAlbumId && mutableUiState.value.photos.isNotEmpty()) {
            return
        }

        load(
            albumId = albumId,
            refreshAlbums = false
        )
    }

    private fun load(
        albumId: Int?,
        refreshAlbums: Boolean
    ) {
        viewModelScope.launch {
            val currentState = mutableUiState.value
            mutableUiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedAlbumId = albumId
                )
            }

            try {
                val albums = if (refreshAlbums || currentState.albums.isEmpty()) {
                    photosRepository.getAlbums()
                } else {
                    currentState.albums
                }

                val photos = photosRepository.getPhotos(albumId = albumId)

                mutableUiState.value = PhotosUiState(
                    isLoading = false,
                    albums = albums,
                    selectedAlbumId = albumId,
                    photos = photos
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = photoErrorMessage(exception, "Unable to load photos")
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            photosRepository: PhotosRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PhotosViewModel::class.java)) {
                        return PhotosViewModel(
                            photosRepository = photosRepository
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

private fun photoErrorMessage(
    exception: Throwable,
    fallback: String
): String {
    return when (exception) {
        is RepositoryException -> exception.message ?: fallback
        is IOException -> "Unable to reach server"
        else -> fallback
    }
}
