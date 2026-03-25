package com.project.auto_aid.provider.verification

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProviderVerificationViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repo = ProviderVerificationRepository(app.applicationContext)

    private val _state = MutableStateFlow(ProviderVerificationUiState())
    val state: StateFlow<ProviderVerificationUiState> = _state

    private var pollingJob: Job? = null

    init {
        loadProfile()
        startPolling()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = "")

            try {
                val profile = repo.loadProviderProfile()

                _state.value = _state.value.copy(
                    loading = false,
                    businessName = profile.businessName,
                    phone = profile.phone,
                    verificationStatus = profile.verificationStatus,
                    rejectionReason = profile.rejectionReason,

                    licenseDocumentUrl = profile.licenseDocumentUrl,
                    businessDocumentUrl = profile.businessDocumentUrl,
                    nationalIdFrontUrl = profile.nationalIdFrontUrl,
                    nationalIdBackUrl = profile.nationalIdBackUrl,
                    profileImageUrl = profile.profileImageUrl
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    // TEXT INPUTS
    fun onBusinessNameChange(value: String) {
        _state.value = _state.value.copy(businessName = value)
    }

    fun onPhoneChange(value: String) {
        _state.value = _state.value.copy(phone = value)
    }

    fun onBusinessTypeChange(value: String) {
        _state.value = _state.value.copy(businessType = value)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(error = "", success = "")
    }

    // IMAGE SETTERS
    fun setLicenseUri(uri: Uri?) {
        _state.value = _state.value.copy(selectedLicenseUri = uri)
    }

    fun setBusinessUri(uri: Uri?) {
        _state.value = _state.value.copy(selectedBusinessUri = uri)
    }

    fun setNationalIdFrontUri(uri: Uri?) {
        _state.value = _state.value.copy(selectedNationalIdFrontUri = uri)
    }

    fun setNationalIdBackUri(uri: Uri?) {
        _state.value = _state.value.copy(selectedNationalIdBackUri = uri)
    }

    fun setProfileImageUri(uri: Uri?) {
        _state.value = _state.value.copy(selectedProfileImageUri = uri)
    }

    fun saveProfile() {
        submitVerification()
    }

    fun submitVerification() {
        viewModelScope.launch {
            val current = _state.value

            // REQUIRED CHECKS
            if (current.selectedLicenseUri == null && current.licenseDocumentUrl.isBlank()) {
                _state.value = current.copy(error = "Work license is required")
                return@launch
            }

            if (current.selectedNationalIdFrontUri == null && current.nationalIdFrontUrl.isBlank()) {
                _state.value = current.copy(error = "National ID (front) is required")
                return@launch
            }

            if (current.selectedNationalIdBackUri == null && current.nationalIdBackUrl.isBlank()) {
                _state.value = current.copy(error = "National ID (back) is required")
                return@launch
            }

            _state.value = current.copy(submitting = true, error = "", success = "")

            try {
                Log.d("VerificationVM", "Uploading all verification images")

                val result = repo.submitVerification(
                    businessName = current.businessName,
                    phone = current.phone,

                    licenseUri = current.selectedLicenseUri,
                    businessUri = current.selectedBusinessUri,
                    nationalIdFrontUri = current.selectedNationalIdFrontUri,
                    nationalIdBackUri = current.selectedNationalIdBackUri,
                    profileImageUri = current.selectedProfileImageUri
                )

                _state.value = current.copy(
                    submitting = false,
                    verificationStatus = "pending",
                    success = result.message.ifBlank {
                        "Verification submitted successfully"
                    },

                    selectedLicenseUri = null,
                    selectedBusinessUri = null,
                    selectedNationalIdFrontUri = null,
                    selectedNationalIdBackUri = null,
                    selectedProfileImageUri = null
                )

                loadProfile()

            } catch (e: Exception) {
                Log.e("VerificationVM", "Upload failed", e)

                _state.value = current.copy(
                    submitting = false,
                    error = e.message ?: "Verification failed"
                )
            }
        }
    }

    fun startPolling() {
        if (pollingJob != null) return

        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val profile = repo.loadProviderProfile()

                    _state.value = _state.value.copy(
                        verificationStatus = profile.verificationStatus,
                        rejectionReason = profile.rejectionReason,

                        licenseDocumentUrl = profile.licenseDocumentUrl,
                        businessDocumentUrl = profile.businessDocumentUrl,
                        nationalIdFrontUrl = profile.nationalIdFrontUrl,
                        nationalIdBackUrl = profile.nationalIdBackUrl,
                        profileImageUrl = profile.profileImageUrl
                    )
                } catch (_: Exception) {}

                delay(10000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
