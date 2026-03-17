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
    val state: StateFlow<AuthUiState> = _state

    private var cooldownJob: Job? = null

    fun login(email: String, password: String) {
        if (_state.value.loading) return
        if (_state.value.cooldownSeconds > 0) return

        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Enter email and password")
            return
        }

        _state.value = _state.value.copy(loading = true, error = "", data = null)

        viewModelScope.launch {
            try {
                val auth = repo.login(email, password)
                _state.value = _state.value.copy(loading = false, data = auth)

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
                        error = "Login failed (${e.code()})"
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var left = seconds
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

    fun logout() {
        cooldownJob?.cancel()
        repo.logout()
        _state.value = AuthUiState()
    }
}