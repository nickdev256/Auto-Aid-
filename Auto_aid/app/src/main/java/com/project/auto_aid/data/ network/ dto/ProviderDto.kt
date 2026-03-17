package com.project.auto_aid.data.network.dto

data class ProviderDto(
    val _id: String? = null,
    val id: String? = null,

    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,

    val role: String? = null,
    val status: String? = null,

    val providerType: String? = null,
    val businessType: String? = null,
    val businessName: String? = null,

    val rating: Double? = 0.0,
    val profileImageUrl: String? = null,
    val logoUrl: String? = null,

    val isOnline: Boolean? = null
) {
    fun resolvedId(): String = _id ?: id ?: ""

    fun resolvedProviderType(): String =
        (providerType ?: businessType ?: "")
            .trim()
            .lowercase()
}