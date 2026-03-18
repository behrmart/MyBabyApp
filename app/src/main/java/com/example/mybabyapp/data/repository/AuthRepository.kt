package com.example.mybabyapp.data.repository

import com.example.mybabyapp.data.api.AuthApi
import com.example.mybabyapp.data.auth.SessionStore
import com.example.mybabyapp.data.model.LoginRequest
import com.example.mybabyapp.data.model.UserSession
import java.io.IOException
import kotlinx.coroutines.CancellationException

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) {
    suspend fun login(
        username: String,
        password: String
    ): LoginResult {
        val response = try {
            authApi.login(
                request = LoginRequest(
                    username = username,
                    password = password
                )
            )
        } catch (exception: IOException) {
            return LoginResult.Failure("Unable to reach server")
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            return LoginResult.Failure("Unable to sign in right now")
        }

        if (!response.isSuccessful) {
            return LoginResult.Failure(
                message = when (response.code()) {
                    401 -> "Invalid credentials"
                    else -> "Unable to sign in right now"
                }
            )
        }

        val responseBody = response.body()
            ?: return LoginResult.Failure("Empty response from server")

        sessionStore.saveSession(
            session = UserSession(
                token = responseBody.token,
                userId = responseBody.user.id,
                username = responseBody.user.username,
                role = responseBody.user.role
            )
        )

        return LoginResult.Success
    }

    suspend fun logout() {
        sessionStore.clearSession()
    }
}

sealed interface LoginResult {
    data object Success : LoginResult

    data class Failure(val message: String) : LoginResult
}
