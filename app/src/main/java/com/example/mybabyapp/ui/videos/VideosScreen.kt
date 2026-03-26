package com.example.mybabyapp.ui.videos

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.MediaCategory
import com.example.mybabyapp.data.model.VideoSummary
import com.example.mybabyapp.data.repository.VideosRepository
import com.example.mybabyapp.ui.components.VideoLockerScreenScaffold
import com.example.mybabyapp.ui.components.VideoLockerSectionCard

@Composable
fun VideosScreen(
    videosRepository: VideosRepository,
    onBack: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MediaListViewModel = viewModel(
        factory = MediaListViewModel.factory(
            videosRepository = videosRepository,
            category = MediaCategory.VIDEOS
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MediaCatalogScreen(
        titleRes = R.string.videos_title,
        emptyMessageRes = R.string.videos_empty,
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onOpenMedia = onOpenMedia,
        modifier = modifier
    )
}

@Composable
internal fun MediaCatalogScreen(
    @StringRes titleRes: Int,
    @StringRes emptyMessageRes: Int,
    uiState: MediaListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    VideoLockerScreenScaffold(
        title = stringResource(titleRes),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        actions = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoLockerSectionCard {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            uiState.errorMessage != null && uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoLockerSectionCard {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = onRetry) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoLockerSectionCard {
                        Text(
                            text = stringResource(emptyMessageRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    uiState.errorMessage?.let { errorMessage ->
                        item {
                            VideoLockerSectionCard {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(
                        items = uiState.items,
                        key = { item -> item.id }
                    ) { item ->
                        MediaListItem(
                            item = item,
                            onOpenMedia = onOpenMedia
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaListItem(
    item: VideoSummary,
    onOpenMedia: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMedia(item.id) }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.description
                ?.takeIf { description -> description.isNotBlank() }
                ?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            Text(
                text = stringResource(R.string.media_views, item.views),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.media_mime_type, item.mimeType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.media_created_at, item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
