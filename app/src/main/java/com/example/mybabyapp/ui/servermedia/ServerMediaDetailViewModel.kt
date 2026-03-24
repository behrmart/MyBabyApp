package com.example.mybabyapp.ui.servermedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.model.ServerMediaItem
import com.example.mybabyapp.data.repository.ServerMediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerMediaDetailUiState(
    val isLoading: Boolean = true,
    val item: ServerMediaItem? = null,
    val errorMessage: String? = null
)

class ServerMediaDetailViewModel(
    private val serverMediaRepository: ServerMediaRepository,
    private val mediaId: String
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ServerMediaDetailUiState())
    val uiState: StateFlow<ServerMediaDetailUiState> = mutableUiState.asStateFlow()

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
                val item = serverMediaRepository.getItem(mediaId)
                mutableUiState.value = ServerMediaDetailUiState(
                    isLoading = false,
                    item = item
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = serverMediaErrorMessage(
                            exception = exception,
                            fallback = "Unable to load external media"
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            serverMediaRepository: ServerMediaRepository,
            mediaId: String
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ServerMediaDetailViewModel::class.java)) {
                        return ServerMediaDetailViewModel(
                            serverMediaRepository = serverMediaRepository,
                            mediaId = mediaId
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
