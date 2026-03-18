package com.example.mybabyapp.ui.gifs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.MediaCategory
import com.example.mybabyapp.data.repository.VideosRepository
import com.example.mybabyapp.ui.videos.MediaCatalogScreen
import com.example.mybabyapp.ui.videos.MediaListViewModel

@Composable
fun GifsScreen(
    videosRepository: VideosRepository,
    onBack: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MediaListViewModel = viewModel(
        factory = MediaListViewModel.factory(
            videosRepository = videosRepository,
            category = MediaCategory.GIFS
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MediaCatalogScreen(
        titleRes = R.string.gifs_title,
        emptyMessageRes = R.string.gifs_empty,
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onOpenMedia = onOpenMedia,
        modifier = modifier
    )
}
