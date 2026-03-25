package com.project.auto_aid.settings

import com.project.auto_aid.data.network.dto.AuthResponse
import com.project.auto_aid.data.network.dto.AuthUserDto
import com.project.auto_aid.data.network.dto.MeResponse
import com.project.auto_aid.model.UserProfile

fun AuthResponse.toUserProfile(): UserProfile = user.toUserProfile()

fun MeResponse.toUserProfile(): UserProfile = user.toUserProfile()

fun AuthUserDto.toUserProfile(): UserProfile {
    return UserProfile(
        id = resolvedId(),
        fullName = name?.trim().orEmpty().ifBlank { "Unknown User" },
        email = email?.trim().orEmpty(),
        phone = phone?.trim().orEmpty().ifBlank { "Not Provided" },
        verificationStatus = verificationStatus?.trim()?.lowercase()
            ?: "not_verified"
    )
}