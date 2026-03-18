package com.example.mybabyapp.ui.videos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.VideoComment
import com.example.mybabyapp.data.model.VideoDetail
import com.example.mybabyapp.data.model.isGifItem
import com.example.mybabyapp.data.model.isVideoItem
import com.example.mybabyapp.data.repository.VideosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = detail?.title ?: stringResource(R.string.media_detail_title))
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
            uiState.isLoading && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = viewModel::refresh) {
                        Text(text = stringResource(R.string.retry))
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            when {
                detail.isVideoItem() -> {
                    AuthenticatedVideoPlayer(
                        streamUrl = streamUrl,
                        okHttpClient = okHttpClient,
                        onPlaybackStarted = onPlaybackStarted
                    )
                }

                detail.isGifItem() -> {
                    GifPreview(
                        title = detail.title,
                        streamUrl = streamUrl,
                        okHttpClient = okHttpClient
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.media_mime_type, detail.mimeType),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.media_created_at, detail.createdAt),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    enabled = !isSubmittingComment
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
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (comments.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_comments),
                    style = MaterialTheme.typography.bodyMedium
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    okHttpClient: OkHttpClient
) {
    val context = LocalContext.current
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        GifPreviewState.Error -> {
            Text(
                text = stringResource(R.string.gif_preview_unavailable),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        is GifPreviewState.Success -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentState.bytes)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
            )
        }
    }
}

@Composable
private fun AuthenticatedVideoPlayer(
    streamUrl: String,
    okHttpClient: OkHttpClient,
    onPlaybackStarted: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
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
