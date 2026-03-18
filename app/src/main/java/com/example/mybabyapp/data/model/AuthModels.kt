package com.example.mybabyapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: LoggedInUser
)

@Serializable
data class LoggedInUser(
    val id: Int,
    val username: String,
    val role: UserRole
)

@Serializable
enum class UserRole {
    USER,
    ADMIN
}

data class UserSession(
    val token: String,
    val userId: Int,
    val username: String,
    val role: UserRole
)

sealed interface SessionState {
    data object Loading : SessionState

    data object Unauthenticated : SessionState

    data class Authenticated(val session: UserSession) : SessionState
}
