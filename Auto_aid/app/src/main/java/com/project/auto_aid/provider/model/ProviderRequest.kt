package com.project.auto_aid.provider.model

data class ProviderRequest(
    val id: String = "",
    val status: String = "pending",
    val providerType: String = "",

    val targetProviderId: String? = null,

    val assignedProviderId: String = "",
    val assignedProviderName: String = "",
    val assignedProviderPhone: String = "",
    val assignedProviderRating: Double = 0.0,

    // ✅ ADD BACK (so provider can view details)
    val service: String = "",
    val vehicleInfo: String = "",
    val problem: String = "",
    val towType: String = "",

    val userLocation: Map<String, Double> = emptyMap()
)