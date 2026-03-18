package com.example.mybabyapp.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.model.MediaCategory
import com.example.mybabyapp.data.model.VideoSummary
import com.example.mybabyapp.data.model.matchesCategory
import com.example.mybabyapp.data.repository.RepositoryException
import com.example.mybabyapp.data.repository.VideosRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaListUiState(
    val isLoading: Boolean = true,
    val items: List<VideoSummary> = emptyList(),
    val errorMessage: String? = null
)

class MediaListViewModel(
    private val category: MediaCategory,
    private val videosRepository: VideosRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MediaListUiState())
    val uiState: StateFlow<MediaListUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val items = videosRepository.getMediaCatalog()
                    .filter { item -> item.matchesCategory(category) }

                mutableUiState.value = MediaListUiState(
                    isLoading = false,
                    items = items
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = mediaErrorMessage(exception, "Unable to load media")
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            videosRepository: VideosRepository,
            category: MediaCategory
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MediaListViewModel::class.java)) {
                        return MediaListViewModel(
                            category = category,
                            videosRepository = videosRepository
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

internal fun mediaErrorMessage(
    exception: Throwable,
    fallback: String
): String {
    return when (exception) {
        is RepositoryException -> exception.message ?: fallback
        is IOException -> "Unable to reach server"
        else -> fallback
    }
}
