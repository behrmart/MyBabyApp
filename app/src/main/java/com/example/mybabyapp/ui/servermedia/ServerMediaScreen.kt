package com.example.mybabyapp.ui.servermedia

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.ServerMediaFilter
import com.example.mybabyapp.data.model.ServerMediaItem
import com.example.mybabyapp.data.model.matchesFilter
import com.example.mybabyapp.data.repository.ServerMediaRepository
import com.example.mybabyapp.ui.components.VideoLockerScreenScaffold
import com.example.mybabyapp.ui.components.VideoLockerSectionCard

@Composable
fun ServerMediaScreen(
    serverMediaRepository: ServerMediaRepository,
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ServerMediaViewModel = viewModel(
        factory = ServerMediaViewModel.factory(serverMediaRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredItems = uiState.items.filter { item ->
        item.matchesFilter(uiState.selectedFilter)
    }

    ServerMediaContent(
        uiState = uiState,
        filteredItems = filteredItems,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onSelectFilter = viewModel::selectFilter,
        onOpenMedia = onOpenMedia,
        modifier = modifier
    )
}

@Composable
private fun ServerMediaContent(
    uiState: ServerMediaUiState,
    filteredItems: List<ServerMediaItem>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectFilter: (ServerMediaFilter) -> Unit,
    onOpenMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    VideoLockerScreenScaffold(
        title = stringResource(R.string.server_media_title),
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

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ServerMediaFilterRow(
                        selectedFilter = uiState.selectedFilter,
                        onSelectFilter = onSelectFilter
                    )

                    uiState.errorMessage?.let { errorMessage ->
                        VideoLockerSectionCard(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    when {
                        uiState.items.isEmpty() -> {
                            ServerMediaEmptyState(
                                message = stringResource(R.string.server_media_empty)
                            )
                        }

                        filteredItems.isEmpty() -> {
                            ServerMediaEmptyState(
                                message = stringResource(R.string.server_media_filtered_empty)
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = filteredItems,
                                    key = { item -> item.id }
                                ) { item ->
                                    ServerMediaListItem(
                                        item = item,
                                        onOpenMedia = onOpenMedia
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerMediaFilterRow(
    selectedFilter: ServerMediaFilter,
    onSelectFilter: (ServerMediaFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ServerMediaFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectFilter(filter) },
                label = {
                    Text(text = stringResource(filter.labelRes()))
                }
            )
        }
    }
}

@Composable
private fun ServerMediaEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        VideoLockerSectionCard {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ServerMediaListItem(
    item: ServerMediaItem,
    onOpenMedia: (String) -> Unit
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
                text = item.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.server_media_path, item.relativePath),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.server_media_type, item.type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.media_mime_type, item.mimeType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.server_media_size, item.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.server_media_modified_at, item.modifiedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@StringRes
private fun ServerMediaFilter.labelRes(): Int {
    return when (this) {
        ServerMediaFilter.ALL -> R.string.server_media_filter_all
        ServerMediaFilter.VIDEO -> R.string.server_media_filter_video
        ServerMediaFilter.IMAGE -> R.string.server_media_filter_image
        ServerMediaFilter.AUDIO -> R.string.server_media_filter_audio
    }
}
