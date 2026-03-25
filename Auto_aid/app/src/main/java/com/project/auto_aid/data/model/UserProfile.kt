package com.project.auto_aid.data.model

data class UserProfile(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val verificationStatus: String = "not_verified"
)