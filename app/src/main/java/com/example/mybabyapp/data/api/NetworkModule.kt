package com.example.mybabyapp.data.api

import com.example.mybabyapp.data.auth.AuthSessionInterceptor
import com.example.mybabyapp.data.auth.SessionStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createOkHttpClient(sessionStore: SessionStore): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthSessionInterceptor(sessionStore))
            .build()
    }

    private fun createRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    fun createAuthApi(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): AuthApi {
        return createRetrofit(
            baseUrl = baseUrl,
            okHttpClient = okHttpClient
        )
            .create(AuthApi::class.java)
    }

    fun createVideosApi(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): VideosApi {
        return createRetrofit(
            baseUrl = baseUrl,
            okHttpClient = okHttpClient
        )
            .create(VideosApi::class.java)
    }

    fun createPhotosApi(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): PhotosApi {
        return createRetrofit(
            baseUrl = baseUrl,
            okHttpClient = okHttpClient
        )
            .create(PhotosApi::class.java)
    }

    fun createServerMediaApi(
        baseUrl: String,
        okHttpClient: OkHttpClient
    ): ServerMediaApi {
        return createRetrofit(
            baseUrl = baseUrl,
            okHttpClient = okHttpClient
        )
            .create(ServerMediaApi::class.java)
    }
}
