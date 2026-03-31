package com.project.auto_aid.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.network.ApiService
import com.project.auto_aid.data.network.dto.MessageResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

data class UserVerificationUiState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val message: String = "",
    val error: String? = null
)

class UserVerificationViewModel(
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserVerificationUiState())
    val uiState: StateFlow<UserVerificationUiState> = _uiState.asStateFlow()

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(
            isSuccess = false,
            message = "",
            error = null
        )
    }

    fun submitUserVerification(
        contentResolver: ContentResolver,
        documentUri: Uri?,
        profileImageUri: Uri?,
        selectedDocumentType: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmitting = true,
                isSuccess = false,
                message = "",
                error = null
            )

            try {
                if (documentUri == null) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Please select a verification document"
                    )
                    return@launch
                }

                val normalizedType = normalizeDocumentType(selectedDocumentType)
                if (normalizedType == null) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Invalid document type. Use National ID, Passport, or Driver's License"
                    )
                    return@launch
                }

                val documentFile = uriToTempFile(contentResolver, documentUri, "verification_document")
                if (documentFile == null || !documentFile.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Failed to read selected verification document"
                    )
                    return@launch
                }

                val documentTypeBody = normalizedType
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                val documentMime = contentResolver.getType(documentUri) ?: guessMimeFromName(documentFile.name)
                val documentRequestBody = documentFile.asRequestBody(
                    (documentMime ?: "application/octet-stream").toMediaTypeOrNull()
                )

                val verificationDocumentPart = MultipartBody.Part.createFormData(
                    "verificationDocument",
                    documentFile.name,
                    documentRequestBody
                )

                val profileImagePart = profileImageUri?.let { uri ->
                    val imageFile = uriToTempFile(contentResolver, uri, "profile_image")
                    if (imageFile != null && imageFile.exists()) {
                        val imageMime = contentResolver.getType(uri) ?: guessMimeFromName(imageFile.name) ?: "image/*"
                        val imageRequestBody = imageFile.asRequestBody(imageMime.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData(
                            "profileImage",
                            imageFile.name,
                            imageRequestBody
                        )
                    } else {
                        null
                    }
                }

                Log.d("USER_VERIFY", "Submitting multipart verification")
                Log.d("USER_VERIFY", "documentType=$normalizedType")
                Log.d("USER_VERIFY", "documentFile=${documentFile.name}, size=${documentFile.length()}")
                Log.d("USER_VERIFY", "profileImageIncluded=${profileImagePart != null}")

                val response = api.submitUserVerification(
                    documentType = documentTypeBody,
                    verificationDocument = verificationDocumentPart,
                    profileImage = profileImagePart
                )

                val errorBody = response.errorBody()?.string()

                if (response.isSuccessful) {
                    val body: MessageResponse? = response.body()
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        message = body?.message ?: "Verification submitted successfully",
                        error = null
                    )
                    Log.d("USER_VERIFY", "Success: ${body?.message}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isSuccess = false,
                        error = errorBody ?: "Verification submission failed (${response.code()})"
                    )
                    Log.e("USER_VERIFY", "Failed: code=${response.code()} body=$errorBody")
                }
            } catch (e: Exception) {
                Log.e("USER_VERIFY", "Exception", e)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    isSuccess = false,
                    error = e.message ?: "Unexpected error occurred"
                )
            }
        }
    }

    private fun normalizeDocumentType(input: String): String? {
        return when (input.trim().lowercase()) {
            "national id", "national_id", "id", "nationalid" -> "national_id"
            "passport" -> "passport"
            "driver's license", "drivers license", "drivers_license", "driving permit" -> "drivers_license"
            else -> null
        }
    }

    private fun uriToTempFile(
        contentResolver: ContentResolver,
        uri: Uri,
        fallbackName: String
    ): File? {
        return try {
            val fileName = queryFileName(contentResolver, uri) ?: fallbackName
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val suffix = safeName.substringAfterLast('.', "")
            val prefix = safeName.substringBeforeLast('.', safeName).take(20).ifBlank { fallbackName }

            val tempFile = if (suffix.isNotBlank()) {
                File.createTempFile(prefix, ".$suffix")
            } else {
                File.createTempFile(prefix, ".tmp")
            }

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e("USER_VERIFY", "uriToTempFile failed", e)
            null
        }
    }

    private fun queryFileName(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun guessMimeFromName(fileName: String): String? {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".pdf") -> "application/pdf"
            else -> null
        }
    }
}