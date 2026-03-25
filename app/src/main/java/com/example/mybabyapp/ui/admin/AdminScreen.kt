package com.example.mybabyapp.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybabyapp.R
import com.example.mybabyapp.data.model.PhotoSummary
import com.example.mybabyapp.data.model.VideoSummary
import com.example.mybabyapp.data.repository.AdminRepository
import com.example.mybabyapp.data.repository.PhotosRepository
import com.example.mybabyapp.data.repository.VideosRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminRepository: AdminRepository,
    photosRepository: PhotosRepository,
    videosRepository: VideosRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.factory(
            adminRepository = adminRepository,
            photosRepository = photosRepository,
            videosRepository = videosRepository
        )
    )
    val uiState = adminViewModel.uiState.collectAsStateWithLifecycle().value
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        adminViewModel.onVideoFileSelected(uri)
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        adminViewModel.onPhotoFileSelected(uri)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.admin_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = adminViewModel::refresh) {
                        Text(text = stringResource(R.string.retry))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading &&
            uiState.users.isEmpty() &&
            uiState.albums.isEmpty() &&
            uiState.videos.isEmpty() &&
            uiState.photos.isEmpty()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isLoading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                uiState.errorMessage?.let { errorMessage ->
                    item {
                        FeedbackText(
                            message = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.actionMessage?.let { actionMessage ->
                    item {
                        FeedbackText(
                            message = actionMessage,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                item {
                    UserManagementSection(
                        uiState = uiState,
                        onSelectUser = adminViewModel::selectUser,
                        onPasswordChanged = adminViewModel::onPasswordChanged,
                        onSubmitPasswordChange = adminViewModel::submitPasswordChange
                    )
                }
                item {
                    AlbumCreationSection(
                        uiState = uiState,
                        onAlbumNameChanged = adminViewModel::onAlbumNameChanged,
                        onAlbumDescriptionChanged = adminViewModel::onAlbumDescriptionChanged,
                        onCreateAlbum = adminViewModel::createAlbum
                    )
                }
                item {
                    MediaUploadSection(
                        uiState = uiState,
                        onVideoTitleChanged = adminViewModel::onVideoTitleChanged,
                        onVideoDescriptionChanged = adminViewModel::onVideoDescriptionChanged,
                        onPickFile = {
                            videoPickerLauncher.launch("*/*")
                        },
                        onUpload = adminViewModel::uploadVideo
                    )
                }
                item {
                    PhotoUploadSection(
                        uiState = uiState,
                        onSelectAlbum = adminViewModel::selectPhotoAlbum,
                        onPhotoTitleChanged = adminViewModel::onPhotoTitleChanged,
                        onPickPhoto = {
                            photoPickerLauncher.launch("image/*")
                        },
                        onUpload = adminViewModel::uploadPhoto
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.admin_video_list_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.videos.isEmpty()) {
                    item {
                        EmptySectionCard(
                            message = stringResource(R.string.admin_videos_empty)
                        )
                    }
                } else {
                    items(
                        items = uiState.videos,
                        key = { video -> video.id }
                    ) { video ->
                        VideoAdminItem(
                            video = video,
                            isDeleting = uiState.deletingVideoIds.contains(video.id),
                            onDelete = {
                                adminViewModel.deleteVideo(video.id)
                            }
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.admin_photo_list_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.photos.isEmpty()) {
                    item {
                        EmptySectionCard(
                            message = stringResource(R.string.admin_photos_empty)
                        )
                    }
                } else {
                    items(
                        items = uiState.photos,
                        key = { photo -> photo.id }
                    ) { photo ->
                        PhotoAdminItem(
                            photo = photo,
                            isDeleting = uiState.deletingPhotoIds.contains(photo.id),
                            onDelete = {
                                adminViewModel.deletePhoto(photo.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserManagementSection(
    uiState: AdminUiState,
    onSelectUser: (Int) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitPasswordChange: () -> Unit
) {
    val selectedUser = uiState.users.firstOrNull { user -> user.id == uiState.selectedUserId }

    AdminSectionCard(title = stringResource(R.string.admin_users_title)) {
        if (uiState.users.isEmpty()) {
            Text(
                text = stringResource(R.string.admin_users_empty),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = uiState.users,
                    key = { user -> user.id }
                ) { user ->
                    FilterChip(
                        selected = user.id == uiState.selectedUserId,
                        onClick = {
                            onSelectUser(user.id)
                        },
                        label = {
                            Text(text = user.username)
                        }
                    )
                }
            }
            Text(
                text = selectedUser?.let { user ->
                    "${user.username} (${user.role.name})"
                } ?: stringResource(R.string.admin_no_user_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = uiState.passwordDraft,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.password_label))
            },
            enabled = !uiState.isChangingPassword
        )

        uiState.passwordErrorMessage?.let { errorMessage ->
            FeedbackText(
                message = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSubmitPasswordChange,
            enabled = !uiState.isChangingPassword && selectedUser != null
        ) {
            Text(
                text = if (uiState.isChangingPassword) {
                    stringResource(R.string.admin_changing_password)
                } else {
                    stringResource(R.string.admin_change_password)
                }
            )
        }
    }
}

@Composable
private fun AlbumCreationSection(
    uiState: AdminUiState,
    onAlbumNameChanged: (String) -> Unit,
    onAlbumDescriptionChanged: (String) -> Unit,
    onCreateAlbum: () -> Unit
) {
    AdminSectionCard(title = stringResource(R.string.admin_albums_title)) {
        OutlinedTextField(
            value = uiState.albumNameDraft,
            onValueChange = onAlbumNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.admin_album_name_label))
            },
            enabled = !uiState.isCreatingAlbum
        )
        OutlinedTextField(
            value = uiState.albumDescriptionDraft,
            onValueChange = onAlbumDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.admin_album_description_label))
            },
            enabled = !uiState.isCreatingAlbum
        )

        uiState.albumErrorMessage?.let { errorMessage ->
            FeedbackText(
                message = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onCreateAlbum,
            enabled = !uiState.isCreatingAlbum
        ) {
            Text(
                text = if (uiState.isCreatingAlbum) {
                    stringResource(R.string.admin_creating_album)
                } else {
                    stringResource(R.string.admin_create_album)
                }
            )
        }
    }
}

@Composable
private fun MediaUploadSection(
    uiState: AdminUiState,
    onVideoTitleChanged: (String) -> Unit,
    onVideoDescriptionChanged: (String) -> Unit,
    onPickFile: () -> Unit,
    onUpload: () -> Unit
) {
    AdminSectionCard(title = stringResource(R.string.admin_videos_upload_title)) {
        OutlinedTextField(
            value = uiState.videoTitleDraft,
            onValueChange = onVideoTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.admin_video_title_label))
            },
            enabled = !uiState.isUploadingVideo
        )
        OutlinedTextField(
            value = uiState.videoDescriptionDraft,
            onValueChange = onVideoDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.admin_video_description_label))
            },
            enabled = !uiState.isUploadingVideo
        )
        Button(
            onClick = onPickFile,
            enabled = !uiState.isUploadingVideo
        ) {
            Text(text = stringResource(R.string.admin_pick_media_file))
        }
        Text(
            text = uiState.selectedVideoName?.let { fileName ->
                stringResource(R.string.admin_selected_file, fileName)
            } ?: stringResource(R.string.admin_no_file_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        uiState.videoUploadErrorMessage?.let { errorMessage ->
            FeedbackText(
                message = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onUpload,
            enabled = !uiState.isUploadingVideo
        ) {
            Text(
                text = if (uiState.isUploadingVideo) {
                    stringResource(R.string.admin_uploading_media)
                } else {
                    stringResource(R.string.admin_upload_media)
                }
            )
        }
    }
}

@Composable
private fun PhotoUploadSection(
    uiState: AdminUiState,
    onSelectAlbum: (Int) -> Unit,
    onPhotoTitleChanged: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onUpload: () -> Unit
) {
    val selectedAlbum = uiState.albums.firstOrNull { album -> album.id == uiState.selectedPhotoAlbumId }

    AdminSectionCard(title = stringResource(R.string.admin_photos_upload_title)) {
        if (uiState.albums.isEmpty()) {
            Text(
                text = stringResource(R.string.admin_albums_empty),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = uiState.albums,
                    key = { album -> album.id }
                ) { album ->
                    FilterChip(
                        selected = album.id == uiState.selectedPhotoAlbumId,
                        onClick = {
                            onSelectAlbum(album.id)
                        },
                        label = {
                            Text(text = album.name)
                        }
                    )
                }
            }
            Text(
                text = selectedAlbum?.name ?: stringResource(R.string.admin_no_album_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = uiState.photoTitleDraft,
            onValueChange = onPhotoTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.admin_photo_title_label))
            },
            enabled = !uiState.isUploadingPhoto
        )
        Button(
            onClick = onPickPhoto,
            enabled = !uiState.isUploadingPhoto && uiState.albums.isNotEmpty()
        ) {
            Text(text = stringResource(R.string.admin_pick_photo_file))
        }
        Text(
            text = uiState.selectedPhotoName?.let { fileName ->
                stringResource(R.string.admin_selected_file, fileName)
            } ?: stringResource(R.string.admin_no_file_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        uiState.photoUploadErrorMessage?.let { errorMessage ->
            FeedbackText(
                message = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onUpload,
            enabled = !uiState.isUploadingPhoto && uiState.albums.isNotEmpty()
        ) {
            Text(
                text = if (uiState.isUploadingPhoto) {
                    stringResource(R.string.admin_uploading_photo)
                } else {
                    stringResource(R.string.admin_upload_photo)
                }
            )
        }
    }
}

@Composable
private fun VideoAdminItem(
    video: VideoSummary,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.media_mime_type, video.mimeType),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.media_views, video.views),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = onDelete,
                enabled = !isDeleting
            ) {
                Text(
                    text = if (isDeleting) {
                        stringResource(R.string.admin_deleting)
                    } else {
                        stringResource(R.string.admin_delete)
                    }
                )
            }
        }
    }
}

@Composable
private fun PhotoAdminItem(
    photo: PhotoSummary,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = photo.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.media_mime_type, photo.mimeType),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.admin_album_label,
                    photo.album?.name ?: stringResource(R.string.admin_album_unassigned)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = onDelete,
                enabled = !isDeleting
            ) {
                Text(
                    text = if (isDeleting) {
                        stringResource(R.string.admin_deleting)
                    } else {
                        stringResource(R.string.admin_delete)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(message: String) {
    ElevatedCard {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AdminSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        )
    }
}

@Composable
private fun FeedbackText(
    message: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = message,
        color = color,
        style = MaterialTheme.typography.bodyMedium
    )
}
