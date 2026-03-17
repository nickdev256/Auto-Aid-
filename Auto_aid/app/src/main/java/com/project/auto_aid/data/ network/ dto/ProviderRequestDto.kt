package com.project.auto_aid.data.network.dto

data class ProviderRequestDto(
    val id: String? = null,
    val status: String? = null,
    val providerType: String? = null,
    val assignedProviderId: String? = null,
    val targetProviderId: String? = null,
    val assignedProviderName: String? = null,
    val assignedProviderPhone: String? = null,
    val assignedProviderRating: Double? = null
) {
    fun resolvedId(): String = id ?: ""
}