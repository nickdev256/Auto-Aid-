package com.project.auto_aid.provider

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ProviderDto
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.UpdateAvailabilityBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProviderUiState(
    val loading: Boolean = false,
    val error: String = "",
    val success: String = "",

    val provider: ProviderDto? = null,

    val isOnline: Boolean = false,
    val isAvailable: Boolean = false,

    val verificationStatus: String = "not_verified",
    val rejectionReason: String = "",
    val canReceiveJobs: Boolean = false,

    val pendingJobs: List<RequestDto> = emptyList(),
    val ongoingJobs: List<RequestDto> = emptyList(),
    val completedJobs: List<RequestDto> = emptyList()
)

class ProviderViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val tokenStore = TokenStore(application.applicationContext)
    private val api = RetrofitClient.create(tokenStore)

    private val _state = MutableStateFlow(ProviderUiState())
    val state: StateFlow<ProviderUiState> = _state.asStateFlow()

    init {
        loadProvider()
        loadVerification() // ✅ IMPORTANT
        loadJobs()
    }

    /* ================================
       LOAD PROVIDER PROFILE
    ================================= */

    fun loadProvider() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = "")

            try {
                val response = api.getProviderMe()

                if (response.isSuccessful) {
                    val provider = response.body()

                    _state.value = _state.value.copy(
                        loading = false,
                        provider = provider,
                        isOnline = provider?.isOnline ?: false,
                        isAvailable = provider?.isAvailable ?: false
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Failed to load provider"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load provider"
                )
            }
        }
    }

    /* ================================
       ✅ LOAD VERIFICATION (CORRECT)
    ================================= */

    fun loadVerification() {
        viewModelScope.launch {
            try {
                val response = api.getMyProviderVerification()

                if (response.isSuccessful) {

                    val verification =
                        response.body()?.provider?.providerVerification

                    val status = verification?.status ?: "not_verified"
                    val rejection = verification?.rejectionReason ?: ""

                    _state.value = _state.value.copy(
                        verificationStatus = status,
                        rejectionReason = rejection,
                        canReceiveJobs = status == "verified"
                    )
                }
            } catch (_: Exception) {
                // silent fail
            }
        }
    }

    /* ================================
       LOAD JOBS
    ================================= */

    fun loadJobs() {
        viewModelScope.launch {
            try {
                val providerType = _state.value.provider?.providerType ?: "garage"

                val response = api.getProviderBuckets(providerType)

                if (response.isSuccessful) {
                    val data = response.body()

                    _state.value = _state.value.copy(
                        pendingJobs = data?.pending ?: emptyList(),
                        ongoingJobs = data?.ongoing ?: emptyList(),
                        completedJobs = data?.completed ?: emptyList()
                    )
                }
            } catch (_: Exception) {}
        }
    }

    /* ================================
       TOGGLE AVAILABILITY
    ================================= */

    fun toggleAvailability(isAvailable: Boolean) {
        viewModelScope.launch {
            try {
                val response = api.updateProviderAvailability(
                    UpdateAvailabilityBody(isAvailable = isAvailable)
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isAvailable = isAvailable,
                        success = if (isAvailable)
                            "You are now online"
                        else
                            "You are now offline"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to update availability"
                )
            }
        }
    }

    /* ================================
       ACCEPT JOB
    ================================= */

    fun acceptJob(requestId: String) {
        viewModelScope.launch {
            try {
                val response = api.assignRequest(requestId)

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        success = "Job accepted"
                    )
                    loadJobs()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to accept job"
                )
            }
        }
    }

    /* ================================
       DECLINE JOB
    ================================= */

    fun declineJob(requestId: String) {
        viewModelScope.launch {
            try {
                val response = api.declineRequest(requestId)

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        success = "Job declined"
                    )
                    loadJobs()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to decline job"
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            error = "",
            success = ""
        )
    }

    fun refreshAll() {
        loadProvider()
        loadVerification()
        loadJobs()
    }
}