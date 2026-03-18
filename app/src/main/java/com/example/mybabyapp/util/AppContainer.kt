package com.example.mybabyapp.util

import android.content.Context
import com.example.mybabyapp.BuildConfig
import com.example.mybabyapp.data.api.NetworkModule
import com.example.mybabyapp.data.auth.DataStoreSessionStore
import com.example.mybabyapp.data.auth.SessionStore
import com.example.mybabyapp.data.repository.AuthRepository

class AppContainer private constructor(
    appContext: Context
) {
    val sessionStore: SessionStore = DataStoreSessionStore(appContext)

    val authRepository: AuthRepository = AuthRepository(
        authApi = NetworkModule.createAuthApi(
            baseUrl = BuildConfig.BASE_URL,
            sessionStore = sessionStore
        ),
        sessionStore = sessionStore
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
