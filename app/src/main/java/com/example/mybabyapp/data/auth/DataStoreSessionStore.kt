package com.example.mybabyapp.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mybabyapp.data.model.SessionState
import com.example.mybabyapp.data.model.UserRole
import com.example.mybabyapp.data.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val SessionPreferencesName = "video_locker_session"

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SessionPreferencesName
)

class DataStoreSessionStore(
    appContext: Context
) : SessionStore {
    private val dataStore = appContext.applicationContext.sessionDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.Loading)

    override val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .map(::preferencesToSessionState)
                .collectLatest { state ->
                    mutableSessionState.value = state
                }
        }
    }

    override fun currentSession(): UserSession? {
        return (sessionState.value as? SessionState.Authenticated)?.session
    }

    override suspend fun saveSession(session: UserSession) {
        dataStore.edit { preferences ->
            preferences[tokenKey] = session.token
            preferences[userIdKey] = session.userId
            preferences[usernameKey] = session.username
            preferences[roleKey] = session.role.name
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(userIdKey)
            preferences.remove(usernameKey)
            preferences.remove(roleKey)
        }
    }

    private fun preferencesToSessionState(preferences: Preferences): SessionState {
        val token = preferences[tokenKey]
        val userId = preferences[userIdKey]
        val username = preferences[usernameKey]
        val role = preferences[roleKey]

        if (token.isNullOrBlank() || userId == null || username.isNullOrBlank() || role.isNullOrBlank()) {
            return SessionState.Unauthenticated
        }

        val parsedRole = runCatching { UserRole.valueOf(role) }.getOrNull()
            ?: return SessionState.Unauthenticated

        return SessionState.Authenticated(
            session = UserSession(
                token = token,
                userId = userId,
                username = username,
                role = parsedRole
            )
        )
    }

    private companion object {
        val tokenKey = stringPreferencesKey("token")
        val userIdKey = intPreferencesKey("user_id")
        val usernameKey = stringPreferencesKey("username")
        val roleKey = stringPreferencesKey("role")
    }
}
