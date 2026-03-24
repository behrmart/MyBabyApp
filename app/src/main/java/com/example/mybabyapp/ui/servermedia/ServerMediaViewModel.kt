package com.example.mybabyapp.ui.servermedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.model.ServerMediaFilter
import com.example.mybabyapp.data.model.ServerMediaItem
import com.example.mybabyapp.data.repository.RepositoryException
import com.example.mybabyapp.data.repository.ServerMediaRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerMediaUiState(
    val isLoading: Boolean = true,
    val items: List<ServerMediaItem> = emptyList(),
    val selectedFilter: ServerMediaFilter = ServerMediaFilter.ALL,
    val errorMessage: String? = null
)

class ServerMediaViewModel(
    private val serverMediaRepository: ServerMediaRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ServerMediaUiState())
    val uiState: StateFlow<ServerMediaUiState> = mutableUiState.asStateFlow()

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
                val items = serverMediaRepository.getCatalog()

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        items = items
                    )
                }
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

    fun selectFilter(filter: ServerMediaFilter) {
        mutableUiState.update { currentState ->
            currentState.copy(selectedFilter = filter)
        }
    }

    companion object {
        fun factory(
            serverMediaRepository: ServerMediaRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ServerMediaViewModel::class.java)) {
                        return ServerMediaViewModel(
                            serverMediaRepository = serverMediaRepository
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

internal fun serverMediaErrorMessage(
    exception: Throwable,
    fallback: String
): String {
    return when (exception) {
        is RepositoryException -> exception.message ?: fallback
        is IOException -> "Unable to reach server"
        else -> fallback
    }
}
