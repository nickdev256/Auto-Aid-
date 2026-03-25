package com.project.auto_aid.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.model.UserProfile
import com.project.auto_aid.settings.toUserProfile
import com.project.auto_aid.utils.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

data class IdentityVerificationUiState(
    val user: UserProfile? = null,
    val verificationStatus: String = "not_verified",
    val selectedDocumentType: String = "",
    val isSubmitting: Boolean = false,
    val isLoadingUser: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = ""
)

class IdentityVerificationViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "IdentityVerification"
        private const val DEFAULT_REFRESH_INTERVAL = 5000L
    }

    private val tokenStore = TokenStore(app.applicationContext)
    private val api = RetrofitClient.create(tokenStore)

    private var autoRefreshJob: Job? = null
    private var refreshJob: Job? = null

    var uiState by mutableStateOf(IdentityVerificationUiState())
        private set

    init {
        refreshUser()
    }

    fun loadUser(user: UserProfile) {
        val safeStatus = user.verificationStatus.ifBlank { "not_verified" }

        uiState = uiState.copy(
            user = user,
            verificationStatus = safeStatus,
            errorMessage = ""
        )

        if (safeStatus.equals("pending", ignoreCase = true)) {
            startAutoRefresh()
        } else {
            stopAutoRefresh()
        }
    }

    fun refreshUser(showLoader: Boolean = true) {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            try {
                if (showLoader) {
                    uiState = uiState.copy(
                        isLoadingUser = true,
                        errorMessage = ""
                    )
                }

                Log.d(TAG, "Refreshing user from /api/auth/me")

                val response = api.getMe()

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        val user = body.toUserProfile()
                        val safeStatus = user.verificationStatus.ifBlank { "not_verified" }

                        Log.d(
                            TAG,
                            "User refreshed successfully: id=${user.id}, verificationStatus=$safeStatus"
                        )

                        uiState = uiState.copy(
                            user = user,
                            verificationStatus = safeStatus,
                            isLoadingUser = false,
                            errorMessage = ""
                        )

                        if (safeStatus.equals("pending", ignoreCase = true)) {
                            startAutoRefresh()
                        } else {
                            stopAutoRefresh()
                        }
                    } else {
                        Log.e(TAG, "refreshUser: response body is null")
                        uiState = uiState.copy(
                            isLoadingUser = false,
                            errorMessage = "Empty user response"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()

                    Log.e(TAG, "refreshUser failed: code=${response.code()} body=$errorBody")

                    uiState = uiState.copy(
                        isLoadingUser = false,
                        errorMessage = if (errorBody.isNotBlank()) {
                            errorBody
                        } else {
                            "Failed to load user (${response.code()})"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "refreshUser exception", e)
                uiState = uiState.copy(
                    isLoadingUser = false,
                    errorMessage = e.message ?: "Failed to load user"
                )
            } finally {
                refreshJob = null
            }
        }
    }

    fun startAutoRefresh(intervalMillis: Long = DEFAULT_REFRESH_INTERVAL) {
        if (autoRefreshJob?.isActive == true) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMillis)

                val currentStatus = uiState.verificationStatus.ifBlank { "not_verified" }

                if (currentStatus.equals("pending", ignoreCase = true)) {
                    refreshUser(showLoader = false)
                } else {
                    stopAutoRefresh()
                    break
                }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun selectDocument(type: String) {
        uiState = uiState.copy(
            selectedDocumentType = type.trim(),
            errorMessage = "",
            successMessage = ""
        )
    }

    fun clearDocument() {
        uiState = uiState.copy(
            selectedDocumentType = "",
            errorMessage = "",
            successMessage = ""
        )
    }

    fun clearMessages() {
        uiState = uiState.copy(
            errorMessage = "",
            successMessage = ""
        )
    }

    fun uploadVerification(
        context: Context,
        bitmap: Bitmap?,
        documentType: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cleanType = documentType.trim().lowercase()

                if (bitmap == null) {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Please choose a document image first",
                        successMessage = ""
                    )
                    return@launch
                }

                if (cleanType.isBlank()) {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Please select document type",
                        successMessage = ""
                    )
                    return@launch
                }

                uiState = uiState.copy(
                    isSubmitting = true,
                    errorMessage = "",
                    successMessage = ""
                )

                val filePart = try {
                    FileUtils.bitmapToMultipart(
                        context = context,
                        bitmap = bitmap,
                        fileName = "verification_${System.currentTimeMillis()}.jpg"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "bitmapToMultipart failed", e)
                    null
                }

                if (filePart == null) {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to prepare file for upload",
                        successMessage = ""
                    )
                    return@launch
                }

                var licensePart: MultipartBody.Part? = null
                var businessPart: MultipartBody.Part? = null
                var nationalFrontPart: MultipartBody.Part? = null
                var nationalBackPart: MultipartBody.Part? = null
                var profileImagePart: MultipartBody.Part? = null

                when (cleanType) {
                    "license", "driving_license" -> licensePart = filePart
                    "business", "business_license" -> businessPart = filePart
                    "national_front", "national_id_front", "nationalidfront" -> nationalFrontPart = filePart
                    "national_back", "national_id_back", "nationalidback" -> nationalBackPart = filePart
                    "profile", "profileimage", "profile_image" -> profileImagePart = filePart
                    else -> {
                        uiState = uiState.copy(
                            isSubmitting = false,
                            errorMessage = "Unsupported document type: $documentType",
                            successMessage = ""
                        )
                        return@launch
                    }
                }

                Log.d(TAG, "Submitting provider verification: type=$cleanType")

                val response = api.submitProviderVerification(
                    workLicenseDocument = licensePart,
                    businessRegistrationDocument = businessPart,
                    nationalIdFront = nationalFrontPart,
                    nationalIdBack = nationalBackPart,
                    profileImage = profileImagePart,
                    businessName = null,
                    phone = null
                )

                if (response.isSuccessful) {
                    val message = response.body()?.message?.takeIf { it.isNotBlank() }
                        ?: "Verification submitted successfully"

                    Log.d(TAG, "Verification upload successful: $message")

                    uiState = uiState.copy(
                        isSubmitting = false,
                        successMessage = message,
                        errorMessage = "",
                        selectedDocumentType = cleanType
                    )

                    refreshUser(showLoader = false)
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()

                    Log.e(
                        TAG,
                        "Verification upload failed: code=${response.code()} body=$errorBody"
                    )

                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = if (errorBody.isNotBlank()) {
                            errorBody
                        } else {
                            "Upload failed (${response.code()})"
                        },
                        successMessage = ""
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadVerification exception", e)

                uiState = uiState.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "Upload failed",
                    successMessage = ""
                )
            }
        }
    }

    override fun onCleared() {
        stopAutoRefresh()
        super.onCleared()
    }
}