package com.example.mybabyapp.ui.photos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mybabyapp.R
import com.example.mybabyapp.data.repository.PhotosRepository
import com.example.mybabyapp.ui.components.VideoLockerMediaSurface
import com.example.mybabyapp.ui.components.VideoLockerScreenScaffold
import okhttp3.OkHttpClient

@Composable
fun PhotoViewerScreen(
    photoId: Int,
    photosRepository: PhotosRepository,
    okHttpClient: OkHttpClient,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    VideoLockerScreenScaffold(
        title = stringResource(R.string.photo_viewer_title),
        onBack = onBack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            VideoLockerMediaSurface(
                modifier = Modifier.fillMaxSize()
            ) {
                AuthenticatedPhotoImage(
                    streamUrl = photosRepository.streamUrl(photoId),
                    okHttpClient = okHttpClient,
                    contentDescription = stringResource(R.string.photo_viewer_title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
