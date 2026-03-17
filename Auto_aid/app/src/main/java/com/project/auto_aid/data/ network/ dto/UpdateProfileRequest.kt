package com.project.auto_aid.data.network.dto

data class UpdateProfileRequest(
    val name: String,
    val phone: String,
    val profileImageUrl: String? = null,
    val isOnline: Boolean? = null
)