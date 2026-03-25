package com.example.mybabyapp.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mybabyapp.R
import com.example.mybabyapp.data.auth.SessionStore
import com.example.mybabyapp.data.model.SessionState
import com.example.mybabyapp.data.model.UserRole
import com.example.mybabyapp.data.model.UserSession
import com.example.mybabyapp.data.repository.AdminRepository
import com.example.mybabyapp.data.repository.AuthRepository
import com.example.mybabyapp.data.repository.PhotosRepository
import com.example.mybabyapp.data.repository.ServerMediaRepository
import com.example.mybabyapp.data.repository.VideosRepository
import com.example.mybabyapp.ui.admin.AdminScreen
import com.example.mybabyapp.ui.auth.LoginScreen
import com.example.mybabyapp.ui.auth.LoginViewModel
import com.example.mybabyapp.ui.gifs.GifsScreen
import com.example.mybabyapp.ui.photos.PhotoViewerScreen
import com.example.mybabyapp.ui.photos.PhotosScreen
import com.example.mybabyapp.ui.servermedia.ServerMediaDetailScreen
import com.example.mybabyapp.ui.servermedia.ServerMediaScreen
import com.example.mybabyapp.ui.videos.MediaDetailScreen
import com.example.mybabyapp.ui.videos.VideosScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private object AppRoute {
    const val Loading = "loading"
    const val Login = "login"
    const val Home = "home"
    const val Admin = "admin"
    const val Photos = "photos"
    const val ServerMedia = "serverMedia"
    const val Videos = "videos"
    const val Gifs = "gifs"
    const val PhotoViewer = "photoViewer"
    const val PhotoIdArgument = "photoId"
    const val PhotoViewerPattern = "$PhotoViewer/{$PhotoIdArgument}"
    const val ServerMediaDetail = "serverMediaDetail"
    const val ServerMediaIdArgument = "serverMediaId"
    const val ServerMediaDetailPattern = "$ServerMediaDetail/{$ServerMediaIdArgument}"
    const val MediaDetail = "mediaDetail"
    const val MediaIdArgument = "mediaId"
    const val MediaDetailPattern = "$MediaDetail/{$MediaIdArgument}"

    fun photoViewer(photoId: Int): String = "$PhotoViewer/$photoId"
    fun serverMediaDetail(serverMediaId: String): String {
        return "$ServerMediaDetail/${Uri.encode(serverMediaId)}"
    }

    fun mediaDetail(mediaId: Int): String = "$MediaDetail/$mediaId"
}

@Composable
fun VideoLockerApp(
    sessionStore: SessionStore,
    adminRepository: AdminRepository,
    authRepository: AuthRepository,
    photosRepository: PhotosRepository,
    serverMediaRepository: ServerMediaRepository,
    videosRepository: VideosRepository,
    okHttpClient: OkHttpClient,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val sessionState by sessionStore.sessionState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val authenticatedRoutes = setOf(
        AppRoute.Home,
        AppRoute.Admin,
        AppRoute.Photos,
        AppRoute.ServerMedia,
        AppRoute.Videos,
        AppRoute.Gifs,
        AppRoute.PhotoViewerPattern,
        AppRoute.ServerMediaDetailPattern,
        AppRoute.MediaDetailPattern
    )

    LaunchedEffect(sessionState, currentRoute) {
        when (sessionState) {
            SessionState.Loading -> {
                if (currentRoute != AppRoute.Loading) {
                    navController.navigate(AppRoute.Loading) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            SessionState.Unauthenticated -> {
                if (currentRoute != AppRoute.Login) {
                    navController.navigate(AppRoute.Login) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            is SessionState.Authenticated -> {
                if (currentRoute !in authenticatedRoutes) {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Loading,
        modifier = modifier
    ) {
        composable(AppRoute.Loading) {
            SessionLoadingScreen()
        }
        composable(AppRoute.Login) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.factory(authRepository)
            )
            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                uiState = uiState,
                onUsernameChanged = loginViewModel::onUsernameChanged,
                onPasswordChanged = loginViewModel::onPasswordChanged,
                onSubmit = loginViewModel::submit
            )
        }
        composable(AppRoute.Home) {
            val session = (sessionState as? SessionState.Authenticated)?.session
            if (session == null) {
                SessionLoadingScreen()
            } else {
                AuthenticatedHomeScreen(
                    session = session,
                    onLogout = authRepository::logout,
                    onOpenAdmin = if (session.role == UserRole.ADMIN) {
                        {
                            navController.navigate(AppRoute.Admin)
                        }
                    } else {
                        null
                    },
                    onOpenPhotos = {
                        navController.navigate(AppRoute.Photos)
                    },
                    onOpenServerMedia = {
                        navController.navigate(AppRoute.ServerMedia)
                    },
                    onOpenVideos = {
                        navController.navigate(AppRoute.Videos)
                    },
                    onOpenGifs = {
                        navController.navigate(AppRoute.Gifs)
                    }
                )
            }
        }
        composable(AppRoute.Admin) {
            val session = (sessionState as? SessionState.Authenticated)?.session
            when {
                session == null -> {
                    SessionLoadingScreen()
                }

                session.role != UserRole.ADMIN -> {
                    AdminAccessDeniedScreen(
                        onNavigateHome = {
                            navController.navigate(AppRoute.Home) {
                                popUpTo(AppRoute.Home) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                else -> {
                    AdminScreen(
                        adminRepository = adminRepository,
                        photosRepository = photosRepository,
                        videosRepository = videosRepository,
                        onBack = navController::navigateUp
                    )
                }
            }
        }
        composable(AppRoute.ServerMedia) {
            ServerMediaScreen(
                serverMediaRepository = serverMediaRepository,
                onBack = navController::navigateUp,
                onOpenMedia = { mediaId ->
                    navController.navigate(AppRoute.serverMediaDetail(mediaId))
                }
            )
        }
        composable(AppRoute.Photos) {
            PhotosScreen(
                photosRepository = photosRepository,
                okHttpClient = okHttpClient,
                onBack = navController::navigateUp,
                onOpenPhoto = { photoId ->
                    navController.navigate(AppRoute.photoViewer(photoId))
                }
            )
        }
        composable(AppRoute.Videos) {
            VideosScreen(
                videosRepository = videosRepository,
                onBack = navController::navigateUp,
                onOpenMedia = { mediaId ->
                    navController.navigate(AppRoute.mediaDetail(mediaId))
                }
            )
        }
        composable(AppRoute.Gifs) {
            GifsScreen(
                videosRepository = videosRepository,
                onBack = navController::navigateUp,
                onOpenMedia = { mediaId ->
                    navController.navigate(AppRoute.mediaDetail(mediaId))
                }
            )
        }
        composable(AppRoute.PhotoViewerPattern) { backStackEntry ->
            val photoId = backStackEntry.arguments
                ?.getString(AppRoute.PhotoIdArgument)
                ?.toIntOrNull()

            if (photoId == null) {
                SessionLoadingScreen()
            } else {
                PhotoViewerScreen(
                    photoId = photoId,
                    photosRepository = photosRepository,
                    okHttpClient = okHttpClient,
                    onBack = navController::navigateUp
                )
            }
        }
        composable(AppRoute.ServerMediaDetailPattern) { backStackEntry ->
            val mediaId = backStackEntry.arguments
                ?.getString(AppRoute.ServerMediaIdArgument)
                ?.let(Uri::decode)

            if (mediaId.isNullOrBlank()) {
                SessionLoadingScreen()
            } else {
                ServerMediaDetailScreen(
                    mediaId = mediaId,
                    serverMediaRepository = serverMediaRepository,
                    okHttpClient = okHttpClient,
                    onBack = navController::navigateUp
                )
            }
        }
        composable(AppRoute.MediaDetailPattern) { backStackEntry ->
            val mediaId = backStackEntry.arguments
                ?.getString(AppRoute.MediaIdArgument)
                ?.toIntOrNull()

            if (mediaId == null) {
                SessionLoadingScreen()
            } else {
                MediaDetailScreen(
                    mediaId = mediaId,
                    videosRepository = videosRepository,
                    okHttpClient = okHttpClient,
                    onBack = navController::navigateUp
                )
            }
        }
    }
}

@Composable
private fun SessionLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.session_loading),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedHomeScreen(
    session: UserSession,
    onLogout: suspend () -> Unit,
    onOpenAdmin: (() -> Unit)?,
    onOpenPhotos: () -> Unit,
    onOpenServerMedia: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenGifs: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.home_title))
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                onLogout()
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_heading),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.signed_in_as, session.username),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.role_label, session.role.name),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.home_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onOpenAdmin != null) {
                Button(onClick = onOpenAdmin) {
                    Text(text = stringResource(R.string.open_admin))
                }
            }
            Button(onClick = onOpenPhotos) {
                Text(text = stringResource(R.string.open_photos))
            }
            Button(onClick = onOpenServerMedia) {
                Text(text = stringResource(R.string.open_server_media))
            }
            Button(onClick = onOpenVideos) {
                Text(text = stringResource(R.string.open_videos))
            }
            Button(onClick = onOpenGifs) {
                Text(text = stringResource(R.string.open_gifs))
            }
        }
    }
}

@Composable
private fun AdminAccessDeniedScreen(
    onNavigateHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1_000)
        onNavigateHome()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_access_denied_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.admin_access_denied_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
