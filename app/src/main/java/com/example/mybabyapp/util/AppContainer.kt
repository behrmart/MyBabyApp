package com.example.mybabyapp.util

import android.content.Context
import com.example.mybabyapp.BuildConfig
import com.example.mybabyapp.data.api.NetworkModule
import com.example.mybabyapp.data.auth.DataStoreSessionStore
import com.example.mybabyapp.data.auth.SessionStore
import com.example.mybabyapp.data.repository.AuthRepository
import com.example.mybabyapp.data.repository.PhotosRepository
import com.example.mybabyapp.data.repository.ServerMediaRepository
import com.example.mybabyapp.data.repository.VideosRepository
import okhttp3.OkHttpClient

class AppContainer private constructor(
    appContext: Context
) {
    val sessionStore: SessionStore = DataStoreSessionStore(appContext)

    val okHttpClient: OkHttpClient = NetworkModule.createOkHttpClient(sessionStore)

    val authRepository: AuthRepository = AuthRepository(
        authApi = NetworkModule.createAuthApi(
            baseUrl = BuildConfig.BASE_URL,
            okHttpClient = okHttpClient
        ),
        sessionStore = sessionStore
    )

    val videosRepository: VideosRepository = VideosRepository(
        videosApi = NetworkModule.createVideosApi(
            baseUrl = BuildConfig.BASE_URL,
            okHttpClient = okHttpClient
        ),
        baseUrl = BuildConfig.BASE_URL
    )

    val photosRepository: PhotosRepository = PhotosRepository(
        photosApi = NetworkModule.createPhotosApi(
            baseUrl = BuildConfig.BASE_URL,
            okHttpClient = okHttpClient
        ),
        baseUrl = BuildConfig.BASE_URL
    )

    val serverMediaRepository: ServerMediaRepository = ServerMediaRepository(
        serverMediaApi = NetworkModule.createServerMediaApi(
            baseUrl = BuildConfig.BASE_URL,
            okHttpClient = okHttpClient
        ),
        baseUrl = BuildConfig.BASE_URL
    )

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun from(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { container ->
                    instance = container
                }
            }
        }
    }
}
