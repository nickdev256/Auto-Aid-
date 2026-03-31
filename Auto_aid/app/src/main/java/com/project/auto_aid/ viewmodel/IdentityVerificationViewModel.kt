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
import com.project.auto_aid.data.model.UserProfile
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.utils.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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

                val response = api.getMyUserVerification()

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.user != null) {
                        val apiUser = body.user

                        val mappedUser = UserProfile(
                            id = apiUser.id ?: apiUser._id ?: "",
                            fullName = apiUser.name ?: "",
                            email = apiUser.email ?: "",
                            phone = apiUser.phone ?: "",
                            verificationStatus = apiUser.verificationStatus
                                ?: body.verificationStatus
                                ?: "not_verified"
                        )

                        val safeStatus = mappedUser.verificationStatus.ifBlank { "not_verified" }

                        uiState = uiState.copy(
                            user = mappedUser,
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
                        uiState = uiState.copy(
                            isLoadingUser = false,
                            user = null,
                            errorMessage = "User verification response is empty"
                        )
                    }
                } else {
                    val errorText = response.errorBody()?.string()
                    Log.e(TAG, "refreshUser failed: code=${response.code()} body=$errorText")

                    uiState = uiState.copy(
                        isLoadingUser = false,
                        user = null,
                        errorMessage = errorText ?: "Failed to load user (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "refreshUser exception", e)
                uiState = uiState.copy(
                    isLoadingUser = false,
                    user = null,
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

    private fun normalizeDocumentType(documentType: String): String {
        return when (documentType.trim().lowercase()) {
            "national id" -> "national_id"
            "national_id" -> "national_id"
            "passport" -> "passport"
            "driver's license" -> "drivers_license"
            "drivers license" -> "drivers_license"
            "drivers_license" -> "drivers_license"
            else -> documentType.trim().lowercase()
        }
    }

    fun uploadVerification(
        context: Context,
        bitmap: Bitmap?,
        documentType: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (bitmap == null) {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Please capture a document image first",
                        successMessage = ""
                    )
                    return@launch
                }

                val cleanType = normalizeDocumentType(documentType)

                if (cleanType.isBlank()) {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Please select document type",
                        successMessage = ""
                    )
                    return@launch
                }

                if (
                    cleanType != "national_id" &&
                    cleanType != "passport" &&
                    cleanType != "drivers_license"
                ) {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Invalid document type selected",
                        successMessage = ""
                    )
                    return@launch
                }

                uiState = uiState.copy(
                    isSubmitting = true,
                    errorMessage = "",
                    successMessage = ""
                )

                val documentPart: MultipartBody.Part = FileUtils.bitmapToMultipart(
                    context = context,
                    bitmap = bitmap,
                    fileName = "user_verification_${System.currentTimeMillis()}.jpg",
                    partName = "verificationDocument"
                ) ?: run {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to prepare file for upload",
                        successMessage = ""
                    )
                    return@launch
                }

                val documentTypeBody = cleanType.toRequestBody("text/plain".toMediaTypeOrNull())

                Log.d(TAG, "Submitting user verification")
                Log.d(TAG, "documentType = $cleanType")
                Log.d(TAG, "selectedDocumentType ui = ${uiState.selectedDocumentType}")
                Log.d(TAG, "documentPart headers = ${documentPart.headers}")

                val response = api.submitUserVerification(
                    documentType = documentTypeBody,
                    verificationDocument = documentPart,
                    profileImage = null
                )

                if (response.isSuccessful) {
                    val message = response.body()?.message?.takeIf { it.isNotBlank() }
                        ?: "Verification submitted successfully"

                    uiState = uiState.copy(
                        isSubmitting = false,
                        successMessage = message,
                        errorMessage = "",
                        selectedDocumentType = documentType.trim()
                    )

                    refreshUser(showLoader = false)
                    onSuccess()
                } else {
                    val errorText = response.errorBody()?.string()
                    Log.e(TAG, "upload failed: code=${response.code()} body=$errorText")

                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = errorText ?: "Upload failed (${response.code()})",
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