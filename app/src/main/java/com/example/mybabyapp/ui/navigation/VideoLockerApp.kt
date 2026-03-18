package com.example.mybabyapp.ui.navigation

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
import com.example.mybabyapp.data.model.UserSession
import com.example.mybabyapp.data.repository.AuthRepository
import com.example.mybabyapp.data.repository.VideosRepository
import com.example.mybabyapp.ui.auth.LoginScreen
import com.example.mybabyapp.ui.auth.LoginViewModel
import com.example.mybabyapp.ui.gifs.GifsScreen
import com.example.mybabyapp.ui.videos.MediaDetailScreen
import com.example.mybabyapp.ui.videos.VideosScreen
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private object AppRoute {
    const val Loading = "loading"
    const val Login = "login"
    const val Home = "home"
    const val Videos = "videos"
    const val Gifs = "gifs"
    const val MediaDetail = "mediaDetail"
    const val MediaIdArgument = "mediaId"
    const val MediaDetailPattern = "$MediaDetail/{$MediaIdArgument}"

    fun mediaDetail(mediaId: Int): String = "$MediaDetail/$mediaId"
}

@Composable
fun VideoLockerApp(
    sessionStore: SessionStore,
    authRepository: AuthRepository,
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
        AppRoute.Videos,
        AppRoute.Gifs,
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
                    onOpenVideos = {
                        navController.navigate(AppRoute.Videos)
                    },
                    onOpenGifs = {
                        navController.navigate(AppRoute.Gifs)
                    }
                )
            }
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
            Button(onClick = onOpenVideos) {
                Text(text = stringResource(R.string.open_videos))
            }
            Button(onClick = onOpenGifs) {
                Text(text = stringResource(R.string.open_gifs))
            }
        }
    }
}
