package com.example.mybabyapp.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.model.VideoComment
import com.example.mybabyapp.data.model.VideoDetail
import com.example.mybabyapp.data.repository.VideosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaDetailUiState(
    val isLoading: Boolean = true,
    val detail: VideoDetail? = null,
    val comments: List<VideoComment> = emptyList(),
    val commentDraft: String = "",
    val isSubmittingComment: Boolean = false,
    val errorMessage: String? = null,
    val commentErrorMessage: String? = null
)

class MediaDetailViewModel(
    private val mediaId: Int,
    private val videosRepository: VideosRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MediaDetailUiState())
    val uiState: StateFlow<MediaDetailUiState> = mutableUiState.asStateFlow()

    private var hasRecordedView = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val commentDraft = uiState.value.commentDraft

            mutableUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val detail = videosRepository.getMediaDetail(mediaId)
                val comments = videosRepository.getComments(mediaId)

                mutableUiState.value = MediaDetailUiState(
                    isLoading = false,
                    detail = detail,
                    comments = comments,
                    commentDraft = commentDraft
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = mediaErrorMessage(exception, "Unable to load media details")
                    )
                }
            }
        }
    }

    fun onCommentDraftChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                commentDraft = value,
                commentErrorMessage = null
            )
        }
    }

    fun submitComment() {
        val content = uiState.value.commentDraft.trim()

        if (uiState.value.isSubmittingComment) {
            return
        }

        if (content.isBlank()) {
            mutableUiState.update { currentState ->
                currentState.copy(commentErrorMessage = "Comment is required")
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update { currentState ->
                currentState.copy(
                    isSubmittingComment = true,
                    commentErrorMessage = null
                )
            }

            try {
                videosRepository.addComment(
                    id = mediaId,
                    content = content
                )
                val comments = videosRepository.getComments(mediaId)

                mutableUiState.update { currentState ->
                    currentState.copy(
                        comments = comments,
                        commentDraft = "",
                        isSubmittingComment = false,
                        commentErrorMessage = null
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }

                mutableUiState.update { currentState ->
                    currentState.copy(
                        isSubmittingComment = false,
                        commentErrorMessage = mediaErrorMessage(exception, "Unable to post comment")
                    )
                }
            }
        }
    }

    fun onPlaybackStarted() {
        if (hasRecordedView) {
            return
        }

        hasRecordedView = true

        viewModelScope.launch {
            runCatching {
                videosRepository.incrementView(mediaId)
            }
        }
    }

    fun streamUrl(): String = videosRepository.streamUrl(mediaId)

    companion object {
        fun factory(
            videosRepository: VideosRepository,
            mediaId: Int
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MediaDetailViewModel::class.java)) {
                        return MediaDetailViewModel(
                            mediaId = mediaId,
                            videosRepository = videosRepository
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
