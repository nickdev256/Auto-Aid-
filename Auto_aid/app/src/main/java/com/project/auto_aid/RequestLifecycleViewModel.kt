package com.project.auto_aid.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.data.repository.RequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RequestLifecycleUiState(
    val loading: Boolean = false,
    val request: RequestDto? = null,
    val quote: RequestQuoteDto? = null,
    val message: String? = null,
    val error: String? = null
)

class RequestLifecycleViewModel(
    private val repository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestLifecycleUiState())
    val uiState: StateFlow<RequestLifecycleUiState> = _uiState.asStateFlow()

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun loadRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val request = repository.getRequestById(requestId)
                val quote = runCatching { repository.getRequestQuote(requestId) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    request = request,
                    quote = quote
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load request"
                )
            }
        }
    }

    fun assignRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loading = true, error = null)
                repository.assignRequest(requestId)
                val request = repository.getRequestById(requestId)
                val quote = runCatching { repository.getRequestQuote(requestId) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    request = request,
                    quote = quote,
                    message = "Request accepted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to assign request"
                )
            }
        }
    }

    fun updateStatus(requestId: String, status: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loading = true, error = null)
                val result = repository.updateRequestStatus(requestId, status)
                val quote = runCatching { repository.getRequestQuote(requestId) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    request = result.request ?: repository.getRequestById(requestId),
                    quote = quote,
                    message = result.message ?: "Status updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to update status"
                )
            }
        }
    }

    fun setPrice(requestId: String, providerAmount: Double) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loading = true, error = null)
                val result = repository.setRequestPrice(requestId, providerAmount)
                val quote = repository.getRequestQuote(requestId)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    request = result.request ?: repository.getRequestById(requestId),
                    quote = quote,
                    message = result.message ?: "Quotation sent successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to set quotation"
                )
            }
        }
    }

    fun providerComplete(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loading = true, error = null)
                val result = repository.providerCompleteRequest(requestId)
                val quote = runCatching { repository.getRequestQuote(requestId) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    request = result.request ?: repository.getRequestById(requestId),
                    quote = quote,
                    message = result.message ?: "Job marked done"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to mark job done"
                )
            }
        }
    }

    fun userConfirmComplete(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loading = true, error = null)
                val result = repository.userConfirmCompleteRequest(requestId)
                val quote = runCatching { repository.getRequestQuote(requestId) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    request = result.request ?: repository.getRequestById(requestId),
                    quote = quote,
                    message = result.message ?: "Completion confirmed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to confirm completion"
                )
            }
        }
    }
}