package com.project.auto_aid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.AuthResponse
import com.project.auto_aid.data.repo.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val loading: Boolean = false,
    val error: String = "",
    val data: AuthResponse? = null,
    val cooldownSeconds: Int = 0
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenStore = TokenStore(app.applicationContext)
    private val api = RetrofitClient.create(tokenStore)
    private val repo = AuthRepository(api, tokenStore)

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    fun login(email: String, password: String) {
        if (_state.value.loading) return
        if (_state.value.cooldownSeconds > 0) return

        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            _state.value = _state.value.copy(
                error = "Enter email and password"
            )
            return
        }

        _state.value = _state.value.copy(
            loading = true,
            error = "",
            data = null
        )

        viewModelScope.launch {
            try {
                val auth = repo.login(cleanEmail, cleanPassword)

                _state.value = _state.value.copy(
                    loading = false,
                    error = "",
                    data = auth
                )
            } catch (e: HttpException) {
                val retryAfter = e.response()?.headers()?.get("Retry-After")?.toIntOrNull()

                if (e.code() == 429) {
                    val seconds = retryAfter ?: 30
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Too many attempts. Try again in $seconds seconds.",
                        cooldownSeconds = seconds
                    )
                    startCooldown(seconds)
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Login failed (${e.code()})",
                        data = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Login failed",
                    data = null
                )
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var left = seconds
            _state.value = _state.value.copy(cooldownSeconds = left)

            while (left > 0) {
                delay(1000)
                left -= 1
                _state.value = _state.value.copy(cooldownSeconds = left)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = "")
    }

    fun clearAuthData() {
        _state.value = _state.value.copy(data = null)
    }

    fun logout() {
        cooldownJob?.cancel()
        repo.logout()
        _state.value = AuthUiState()
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }
}