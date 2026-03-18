package com.example.mybabyapp.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthSessionInterceptor(
    private val sessionStore: SessionStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val request = if (originalRequest.url.encodedPath.startsWith("/api/auth/")) {
            originalRequest
        } else {
            val token = sessionStore.currentToken()
            if (token.isNullOrBlank()) {
                originalRequest
            } else {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
        }

        val response = chain.proceed(request)

        if (response.code == 401 && request.header("Authorization")?.startsWith("Bearer ") == true) {
            runBlocking {
                sessionStore.clearSession()
            }
        }

        return response
    }
}
