package com.project.auto_aid.data.network.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: AuthUserDto
)

data class AuthUserDto(
    val _id: String? = null,
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val status: String? = null
) {
    fun resolvedId(): String = id ?: _id ?: ""
}