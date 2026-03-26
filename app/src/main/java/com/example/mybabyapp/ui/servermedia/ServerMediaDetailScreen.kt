package com.example.mybabyapp.ui.servermedia

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.ServerMediaItem
import com.example.mybabyapp.data.model.isAudioType
import com.example.mybabyapp.data.model.isImageType
import com.example.mybabyapp.data.model.isVideoType
import com.example.mybabyapp.data.repository.ServerMediaRepository
import com.example.mybabyapp.ui.components.VideoLockerMediaSurface
import com.example.mybabyapp.ui.components.VideoLockerScreenScaffold
import com.example.mybabyapp.ui.components.VideoLockerSectionCard
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun ServerMediaDetailScreen(
    mediaId: String,
    serverMediaRepository: ServerMediaRepository,
    okHttpClient: OkHttpClient,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ServerMediaDetailViewModel = viewModel(
        factory = ServerMediaDetailViewModel.factory(
            serverMediaRepository = serverMediaRepository,
            mediaId = mediaId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val item = uiState.item
    val errorMessage = uiState.errorMessage

    VideoLockerScreenScaffold(
        title = item?.name ?: stringResource(R.string.server_media_detail_title),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        actions = {
            TextButton(onClick = viewModel::refresh) {
                Text(text = stringResource(R.string.retry))
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && item == null -> {
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

            errorMessage != null && item == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoLockerSectionCard {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = viewModel::refresh) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            }

            item != null -> {
                ServerMediaDetailContent(
                    item = item,
                    streamUrl = serverMediaRepository.streamUrl(item.id),
                    okHttpClient = okHttpClient,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ServerMediaDetailContent(
    item: ServerMediaItem,
    streamUrl: String,
    okHttpClient: OkHttpClient,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            VideoLockerMediaSurface(
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    item.isVideoType() -> {
                        AuthenticatedServerMediaPlayer(
                            streamUrl = streamUrl,
                            okHttpClient = okHttpClient,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                    }

                    item.isAudioType() -> {
                        AuthenticatedServerMediaPlayer(
                            streamUrl = streamUrl,
                            okHttpClient = okHttpClient,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp)
                        )
                    }

                    item.isImageType() -> {
                        AuthenticatedServerMediaImage(
                            title = item.name,
                            mimeType = item.mimeType,
                            streamUrl = streamUrl,
                            okHttpClient = okHttpClient,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp)
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.server_media_unsupported),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            VideoLockerSectionCard {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.server_media_path, item.relativePath),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.server_media_type, item.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.media_mime_type, item.mimeType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.server_media_size, item.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.server_media_modified_at, item.modifiedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedServerMediaImage(
    title: String,
    mimeType: String,
    streamUrl: String,
    okHttpClient: OkHttpClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val imageState by produceState<ServerMediaImageState>(
        initialValue = ServerMediaImageState.Loading,
        key1 = streamUrl,
        key2 = okHttpClient
    ) {
        value = try {
            val imageBytes = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(streamUrl)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unable to load image")
                    }

                    response.body?.bytes() ?: throw IOException("Empty image response")
                }
            }

            ServerMediaImageState.Success(imageBytes)
        } catch (_: Exception) {
            ServerMediaImageState.Error
        }
    }

    when (val currentState = imageState) {
        ServerMediaImageState.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ServerMediaImageState.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.server_media_image_unavailable),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        is ServerMediaImageState.Success -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentState.bytes)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = title,
                contentScale = if (mimeType == "image/gif") ContentScale.Fit else ContentScale.Fit,
                modifier = modifier
            )
        }
    }
}

private sealed interface ServerMediaImageState {
    data object Loading : ServerMediaImageState
    data object Error : ServerMediaImageState
    data class Success(val bytes: ByteArray) : ServerMediaImageState
}

@Composable
private fun AuthenticatedServerMediaPlayer(
    streamUrl: String,
    okHttpClient: OkHttpClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaSourceFactory = remember(context, okHttpClient) {
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            OkHttpDataSource.Factory(okHttpClient)
        )
        DefaultMediaSourceFactory(dataSourceFactory)
    }
    val exoPlayer = remember(context, mediaSourceFactory, streamUrl) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = false
            }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()

        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}
