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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.server_media_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null && uiState.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onRetry) {
                        Text(text = stringResource(R.string.retry))
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
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ServerMediaListItem(
    item: ServerMediaItem,
    onOpenMedia: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMedia(item.id) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.media_mime_type, item.mimeType),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.server_media_size, item.size),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.server_media_modified_at, item.modifiedAt),
                style = MaterialTheme.typography.bodySmall
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
