package com.example.mybabyapp.data.auth

import com.example.mybabyapp.data.model.SessionState
import com.example.mybabyapp.data.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface SessionStore {
    val sessionState: StateFlow<SessionState>

    fun currentSession(): UserSession?

    fun currentToken(): String? = currentSession()?.token

    suspend fun saveSession(session: UserSession)

    suspend fun clearSession()
}
