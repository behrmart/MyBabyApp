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

    fun createAuthApi(
        baseUrl: String,
        sessionStore: SessionStore
    ): AuthApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthSessionInterceptor(sessionStore))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }
}
