package com.example.mybabyapp.ui.videos

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.media3.common.Player
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
import com.example.mybabyapp.data.model.VideoComment
import com.example.mybabyapp.data.model.VideoDetail
import com.example.mybabyapp.data.model.isGifItem
import com.example.mybabyapp.data.model.isVideoItem
import com.example.mybabyapp.data.repository.VideosRepository
import com.example.mybabyapp.ui.components.VideoLockerMediaSurface
import com.example.mybabyapp.ui.components.VideoLockerScreenScaffold
import com.example.mybabyapp.ui.components.VideoLockerSectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun MediaDetailScreen(
    mediaId: Int,
    videosRepository: VideosRepository,
    okHttpClient: OkHttpClient,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MediaDetailViewModel = viewModel(
        factory = MediaDetailViewModel.factory(
            videosRepository = videosRepository,
            mediaId = mediaId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = uiState.detail
    val errorMessage = uiState.errorMessage

    VideoLockerScreenScaffold(
        title = detail?.title ?: stringResource(R.string.media_detail_title),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        actions = {
            TextButton(onClick = viewModel::refresh) {
                Text(text = stringResource(R.string.retry))
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && detail == null -> {
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

            errorMessage != null && detail == null -> {
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

            detail != null -> {
                MediaDetailContent(
                    detail = detail,
                    comments = uiState.comments,
                    commentDraft = uiState.commentDraft,
                    isSubmittingComment = uiState.isSubmittingComment,
                    commentErrorMessage = uiState.commentErrorMessage,
                    streamUrl = viewModel.streamUrl(),
                    okHttpClient = okHttpClient,
                    onPlaybackStarted = viewModel::onPlaybackStarted,
                    onCommentDraftChanged = viewModel::onCommentDraftChanged,
                    onSubmitComment = viewModel::submitComment,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun MediaDetailContent(
    detail: VideoDetail,
    comments: List<VideoComment>,
    commentDraft: String,
    isSubmittingComment: Boolean,
    commentErrorMessage: String?,
    streamUrl: String,
    okHttpClient: OkHttpClient,
    onPlaybackStarted: () -> Unit,
    onCommentDraftChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
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
                    detail.isVideoItem() -> {
                        AuthenticatedVideoPlayer(
                            streamUrl = streamUrl,
                            okHttpClient = okHttpClient,
                            onPlaybackStarted = onPlaybackStarted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                    }

                    detail.isGifItem() -> {
                        LaunchedEffect(detail.id) {
                            onPlaybackStarted()
                        }
                        GifPreview(
                            title = detail.title,
                            streamUrl = streamUrl,
                            okHttpClient = okHttpClient,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp)
                        )
                    }
                }
            }
        }

        item {
            VideoLockerSectionCard {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                detail.description
                    ?.takeIf { description -> description.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                Text(
                    text = stringResource(R.string.media_views, detail.views),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.media_mime_type, detail.mimeType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.media_created_at, detail.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            VideoLockerSectionCard {
                Text(
                    text = stringResource(R.string.add_comment_title),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = onCommentDraftChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(R.string.comment_label))
                    }
                )
                commentErrorMessage?.let { errorMessage ->
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = onSubmitComment,
                    enabled = !isSubmittingComment,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSubmittingComment) {
                            stringResource(R.string.posting_comment)
                        } else {
                            stringResource(R.string.post_comment)
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (comments.isEmpty()) {
            item {
                VideoLockerSectionCard {
                    Text(
                        text = stringResource(R.string.no_comments),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(
                items = comments,
                key = { comment -> comment.id }
            ) { comment ->
                CommentItem(comment = comment)
            }
        }
    }
}

@Composable
private fun CommentItem(comment: VideoComment) {
    VideoLockerSectionCard {
        Text(
            text = comment.username,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.media_created_at, comment.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GifPreview(
    title: String,
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
    val gifState by produceState<GifPreviewState>(
        initialValue = GifPreviewState.Loading,
        key1 = streamUrl,
        key2 = okHttpClient
    ) {
        value = try {
            val gifBytes = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(streamUrl)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Unable to load GIF")
                    }

                    response.body?.bytes() ?: error("Empty GIF response")
                }
            }

            GifPreviewState.Success(gifBytes)
        } catch (_: Exception) {
            GifPreviewState.Error
        }
    }

    when (val currentState = gifState) {
        GifPreviewState.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        GifPreviewState.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.gif_preview_unavailable),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        is GifPreviewState.Success -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentState.bytes)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AuthenticatedVideoPlayer(
    streamUrl: String,
    okHttpClient: OkHttpClient,
    onPlaybackStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)
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
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    currentOnPlaybackStarted()
                }
            }
        }

        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = true
                player = exoPlayer
            }
        },
        modifier = modifier,
        update = { playerView ->
            playerView.player = exoPlayer
        }
    )
}

private sealed interface GifPreviewState {
    data object Loading : GifPreviewState

    data object Error : GifPreviewState

    data class Success(val bytes: ByteArray) : GifPreviewState
}
