package com.project.auto_aid.model

data class UserProfile(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val status: String = "",
    val verificationStatus: String = "not_verified"
)