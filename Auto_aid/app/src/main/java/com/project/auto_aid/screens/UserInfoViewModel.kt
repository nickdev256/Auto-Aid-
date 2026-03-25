package com.project.auto_aid.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.project.auto_aid.model.UserProfile

data class UserInfoUiState(
    val user: UserProfile? = null,
    val loading: Boolean = true,
    val error: String = "",
    val editMode: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val editEmail: String = "",
    val verificationStatus: String = "not_verified",
    val verificationStep: Int = 1,
    val selectedDocument: String? = null
)

class UserInfoViewModel : ViewModel() {

    var uiState by mutableStateOf(UserInfoUiState())
        private set

    fun loadUser(user: UserProfile) {
        uiState = uiState.copy(
            user = user,
            loading = false,
            error = "",
            editName = user.fullName,
            editPhone = user.phone,
            editEmail = user.email,
            verificationStatus = user.verificationStatus
        )
    }

    fun setLoading(value: Boolean) {
        uiState = uiState.copy(loading = value)
    }

    fun setError(message: String) {
        uiState = uiState.copy(loading = false, error = message)
    }

    fun clearError() {
        uiState = uiState.copy(error = "")
    }

    fun setEditMode(enabled: Boolean) {
        val user = uiState.user
        uiState = uiState.copy(
            editMode = enabled,
            editName = if (enabled) user?.fullName ?: uiState.editName else uiState.editName,
            editPhone = if (enabled) user?.phone ?: uiState.editPhone else uiState.editPhone,
            editEmail = if (enabled) user?.email ?: uiState.editEmail else uiState.editEmail
        )
    }

    fun updateEditName(value: String) {
        uiState = uiState.copy(editName = value)
    }

    fun updateEditPhone(value: String) {
        uiState = uiState.copy(editPhone = value)
    }

    fun updateEditEmail(value: String) {
        uiState = uiState.copy(editEmail = value)
    }

    fun saveEditedProfile() {
        val currentUser = uiState.user ?: return
        val updatedUser = currentUser.copy(
            fullName = uiState.editName,
            phone = uiState.editPhone,
            email = uiState.editEmail
        )

        uiState = uiState.copy(
            user = updatedUser,
            editMode = false
        )
    }

    fun startVerification() {
        uiState = uiState.copy(
            verificationStep = 1
        )
    }

    fun selectDocument(document: String) {
        uiState = uiState.copy(
            selectedDocument = document,
            verificationStep = 2
        )
    }

    fun goToVerificationStep(step: Int) {
        uiState = uiState.copy(verificationStep = step)
    }

    fun setPendingVerification() {
        uiState = uiState.copy(
            verificationStatus = "pending",
            user = uiState.user?.copy(verificationStatus = "pending"),
            verificationStep = 1
        )
    }
}