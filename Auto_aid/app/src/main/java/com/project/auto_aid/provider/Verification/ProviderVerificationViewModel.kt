package com.project.auto_aid.provider.verification

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ProviderVerificationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val tokenStore = TokenStore(application.applicationContext)
    private val api = RetrofitClient.create(tokenStore)
    private val context = application.applicationContext

    private val _state = MutableStateFlow(ProviderVerificationUiState())
    val state: StateFlow<ProviderVerificationUiState> = _state.asStateFlow()

    init {
        loadVerification()
    }

    fun onBusinessNameChange(value: String) {
        _state.value = _state.value.copy(businessName = value)
    }

    fun onPhoneChange(value: String) {
        _state.value = _state.value.copy(phone = value)
    }

    fun onBusinessTypeChange(value: String) {
        _state.value = _state.value.copy(businessType = value)
    }

    fun onLicenseSelected(uri: Uri?) {
        _state.value = _state.value.copy(
            selectedLicenseUri = uri,
            hasLicensePhoto = uri != null || _state.value.licenseDocumentUrl.isNotBlank()
        )
    }

    fun onBusinessSelected(uri: Uri?) {
        _state.value = _state.value.copy(
            selectedBusinessUri = uri,
            hasBusinessPhoto = uri != null || _state.value.businessDocumentUrl.isNotBlank()
        )
    }

    fun onNationalFrontSelected(uri: Uri?) {
        _state.value = _state.value.copy(
            selectedNationalIdFrontUri = uri,
            hasNationalIdFrontPhoto = uri != null || _state.value.nationalIdFrontUrl.isNotBlank()
        )
    }

    fun onNationalBackSelected(uri: Uri?) {
        _state.value = _state.value.copy(
            selectedNationalIdBackUri = uri,
            hasNationalIdBackPhoto = uri != null || _state.value.nationalIdBackUrl.isNotBlank()
        )
    }

    fun onProfileImageSelected(uri: Uri?) {
        _state.value = _state.value.copy(
            selectedProfileImageUri = uri,
            hasProfilePhoto = uri != null || _state.value.profileImageUrl.isNotBlank()
        )
    }

    fun clearMessage() {
        _state.value = _state.value.copy(error = "", success = "")
    }

    fun loadVerification() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = "", success = "")

            try {
                val response = api.getMyProviderVerification()

                if (response.isSuccessful) {
                    val provider = response.body()?.provider
                    val verification = provider?.providerVerification
                    val status = verification?.status ?: "not_verified"

                    _state.value = _state.value.copy(
                        loading = false,
                        businessName = provider?.fullName ?: "",
                        phone = provider?.phone ?: "",
                        verificationStatus = status,
                        rejectionReason = verification?.rejectionReason ?: "",
                        licenseDocumentUrl = verification?.licenseDocumentUrl ?: "",
                        businessDocumentUrl = verification?.businessDocumentUrl ?: "",
                        nationalIdFrontUrl = verification?.nationalIdFrontUrl ?: "",
                        nationalIdBackUrl = verification?.nationalIdBackUrl ?: "",
                        profileImageUrl = verification?.profileImageUrl ?: "",
                        hasLicensePhoto = !verification?.licenseDocumentUrl.isNullOrBlank(),
                        hasBusinessPhoto = !verification?.businessDocumentUrl.isNullOrBlank(),
                        hasNationalIdFrontPhoto = !verification?.nationalIdFrontUrl.isNullOrBlank(),
                        hasNationalIdBackPhoto = !verification?.nationalIdBackUrl.isNullOrBlank(),
                        hasProfilePhoto = !verification?.profileImageUrl.isNullOrBlank(),
                        canReceiveJobs = status == "verified",
                        verifiedBadgeText = if (status == "verified") {
                            "Verified Provider"
                        } else {
                            "Verification Required"
                        }
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = response.errorBody()?.string() ?: "Failed to load provider verification"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load provider verification"
                )
            }
        }
    }

    fun uploadLicenseDocument(context: Context, uri: Uri) {
        onLicenseSelected(uri)
        _state.value = _state.value.copy(
            success = "Work license selected successfully",
            error = ""
        )
    }

    fun uploadBusinessDocument(context: Context, uri: Uri) {
        onBusinessSelected(uri)
        _state.value = _state.value.copy(
            success = "Business document selected successfully",
            error = ""
        )
    }

    fun uploadNationalIdFront(context: Context, uri: Uri) {
        onNationalFrontSelected(uri)
        _state.value = _state.value.copy(
            success = "National ID front selected successfully",
            error = ""
        )
    }

    fun uploadNationalIdBack(context: Context, uri: Uri) {
        onNationalBackSelected(uri)
        _state.value = _state.value.copy(
            success = "National ID back selected successfully",
            error = ""
        )
    }

    fun uploadProfileImage(context: Context, uri: Uri) {
        onProfileImageSelected(uri)
        _state.value = _state.value.copy(
            success = "Profile image selected successfully",
            error = ""
        )
    }

    fun submitVerification() {
        viewModelScope.launch {
            val current = _state.value

            if (current.businessName.isBlank()) {
                _state.value = current.copy(error = "Business name is required", success = "")
                return@launch
            }

            if (current.phone.isBlank()) {
                _state.value = current.copy(error = "Phone number is required", success = "")
                return@launch
            }

            if (current.businessType.isBlank()) {
                _state.value = current.copy(error = "Business type is required", success = "")
                return@launch
            }

            if (current.selectedLicenseUri == null && current.licenseDocumentUrl.isBlank()) {
                _state.value = current.copy(error = "Work license is required", success = "")
                return@launch
            }

            if (current.selectedNationalIdFrontUri == null && current.nationalIdFrontUrl.isBlank()) {
                _state.value = current.copy(error = "National ID front is required", success = "")
                return@launch
            }

            if (current.selectedNationalIdBackUri == null && current.nationalIdBackUrl.isBlank()) {
                _state.value = current.copy(error = "National ID back is required", success = "")
                return@launch
            }

            _state.value = current.copy(submitting = true, error = "", success = "")

            try {
                val licensePart = current.selectedLicenseUri?.let {
                    uriToMultipart("workLicenseDocument", it)
                }

                val businessPart = current.selectedBusinessUri?.let {
                    uriToMultipart("businessRegistrationDocument", it)
                }

                val nationalFrontPart = current.selectedNationalIdFrontUri?.let {
                    uriToMultipart("nationalIdFront", it)
                }

                val nationalBackPart = current.selectedNationalIdBackUri?.let {
                    uriToMultipart("nationalIdBack", it)
                }

                val profileImagePart = current.selectedProfileImageUri?.let {
                    uriToMultipart("profileImage", it)
                }

                val businessNameBody =
                    current.businessName.toRequestBody("text/plain".toMediaTypeOrNull())
                val phoneBody =
                    current.phone.toRequestBody("text/plain".toMediaTypeOrNull())
                val businessTypeBody =
                    current.businessType.lowercase().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.submitProviderVerification(
                    workLicenseDocument = licensePart,
                    businessRegistrationDocument = businessPart,
                    nationalIdFront = nationalFrontPart,
                    nationalIdBack = nationalBackPart,
                    profileImage = profileImagePart,
                    businessName = businessNameBody,
                    phone = phoneBody,
                    businessType = businessTypeBody
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        submitting = false,
                        success = response.body()?.message ?: "Verification submitted successfully",
                        error = "",
                        selectedLicenseUri = null,
                        selectedBusinessUri = null,
                        selectedNationalIdFrontUri = null,
                        selectedNationalIdBackUri = null,
                        selectedProfileImageUri = null
                    )
                    loadVerification()
                } else {
                    _state.value = _state.value.copy(
                        submitting = false,
                        error = response.errorBody()?.string() ?: "Failed to submit verification",
                        success = ""
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    submitting = false,
                    error = e.message ?: "Failed to submit verification",
                    success = ""
                )
            }
        }
    }

    fun saveProfile() {
        submitVerification()
    }

    private fun uriToMultipart(partName: String, uri: Uri): MultipartBody.Part {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open selected file")

        val mimeType = resolver.getType(uri) ?: "image/*"
        val extension = when {
            mimeType.contains("png") -> ".png"
            mimeType.contains("pdf") -> ".pdf"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
            else -> ".jpg"
        }

        val fileName = "upload_${System.currentTimeMillis()}$extension"
        val tempFile = File(context.cacheDir, fileName)

        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, tempFile.name, requestBody)
    }
}