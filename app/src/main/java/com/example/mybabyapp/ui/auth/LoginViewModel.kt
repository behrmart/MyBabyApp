package com.example.mybabyapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybabyapp.data.repository.AuthRepository
import com.example.mybabyapp.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                username = value,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        mutableUiState.update { currentState ->
            currentState.copy(
                password = value,
                errorMessage = null
            )
        }
    }

    fun submit() {
        val currentState = uiState.value
        val username = currentState.username.trim()
        val password = currentState.password

        if (currentState.isSubmitting) {
            return
        }

        if (username.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "Username is required") }
            return
        }

        if (password.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "Password is required") }
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            when (val result = authRepository.login(username = username, password = password)) {
                LoginResult.Success -> {
                    mutableUiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            password = ""
                        )
                    }
                }

                is LoginResult.Failure -> {
                    mutableUiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                        return LoginViewModel(authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
