package com.example.mybabyapp.ui.photos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.PhotoAlbum
import com.example.mybabyapp.data.model.PhotoSummary
import com.example.mybabyapp.data.repository.PhotosRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun PhotosScreen(
    photosRepository: PhotosRepository,
    okHttpClient: OkHttpClient,
    onBack: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PhotosViewModel = viewModel(
        factory = PhotosViewModel.factory(photosRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PhotosContent(
        uiState = uiState,
        photosRepository = photosRepository,
        okHttpClient = okHttpClient,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onSelectAlbum = viewModel::selectAlbum,
        onOpenPhoto = onOpenPhoto,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotosContent(
    uiState: PhotosUiState,
    photosRepository: PhotosRepository,
    okHttpClient: OkHttpClient,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectAlbum: (Int?) -> Unit,
    onOpenPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.photos_title))
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
            uiState.isLoading && uiState.albums.isEmpty() && uiState.photos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null && uiState.albums.isEmpty() && uiState.photos.isEmpty() -> {
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
                    AlbumFilterRow(
                        albums = uiState.albums,
                        selectedAlbumId = uiState.selectedAlbumId,
                        onSelectAlbum = onSelectAlbum
                    )

                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    uiState.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (uiState.photos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.photos_empty),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        PhotoGrid(
                            photos = uiState.photos,
                            photosRepository = photosRepository,
                            okHttpClient = okHttpClient,
                            onOpenPhoto = onOpenPhoto,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumFilterRow(
    albums: List<PhotoAlbum>,
    selectedAlbumId: Int?,
    onSelectAlbum: (Int?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedAlbumId == null,
                onClick = { onSelectAlbum(null) },
                label = {
                    Text(text = stringResource(R.string.photos_all_albums))
                }
            )
        }

        items(
            items = albums,
            key = { album -> album.id }
        ) { album ->
            FilterChip(
                selected = selectedAlbumId == album.id,
                onClick = { onSelectAlbum(album.id) },
                label = {
                    Text(text = album.name)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<PhotoSummary>,
    photosRepository: PhotosRepository,
    okHttpClient: OkHttpClient,
    onOpenPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = photos,
            key = { photo -> photo.id }
        ) { photo ->
            PhotoGridItem(
                photo = photo,
                streamUrl = photosRepository.streamUrl(photo.id),
                okHttpClient = okHttpClient,
                onOpenPhoto = onOpenPhoto
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoSummary,
    streamUrl: String,
    okHttpClient: OkHttpClient,
    onOpenPhoto: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPhoto(photo.id) }
    ) {
        Column {
            AuthenticatedPhotoImage(
                streamUrl = streamUrl,
                okHttpClient = okHttpClient,
                contentDescription = photo.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                photo.album?.name?.let { albumName ->
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun AuthenticatedPhotoImage(
    streamUrl: String,
    okHttpClient: OkHttpClient,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val imageState by produceState(
        initialValue = PhotoImageState(isLoading = true),
        key1 = streamUrl
    ) {
        value = PhotoImageState(isLoading = true)
        value = try {
            val imageBytes = withContext(Dispatchers.IO) {
                fetchPhotoBytes(
                    okHttpClient = okHttpClient,
                    streamUrl = streamUrl
                )
            }
            PhotoImageState(imageBytes = imageBytes)
        } catch (exception: Exception) {
            PhotoImageState(errorMessage = photoLoadErrorMessage(exception))
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp)
                )
            }

            imageState.imageBytes != null -> {
                AsyncImage(
                    model = imageState.imageBytes,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            else -> {
                Text(
                    text = imageState.errorMessage ?: stringResource(R.string.photo_unavailable),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class PhotoImageState(
    val isLoading: Boolean = false,
    val imageBytes: ByteArray? = null,
    val errorMessage: String? = null
)

private suspend fun fetchPhotoBytes(
    okHttpClient: OkHttpClient,
    streamUrl: String
): ByteArray {
    val request = Request.Builder()
        .url(streamUrl)
        .build()

    return okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Unable to load photo")
        }

        response.body?.bytes() ?: throw IOException("Empty response from server")
    }
}

private fun photoLoadErrorMessage(exception: Throwable): String {
    return when (exception) {
        is IOException -> "Unable to load photo"
        else -> "Photo is unavailable"
    }
}
