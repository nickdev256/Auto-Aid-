package com.project.auto_aid.data.network.dto

data class LocationDto(
    val lat: Double? = 0.0,
    val lng: Double? = 0.0
)

data class RequestDto(
    val _id: String? = null,
    val id: String? = null,

    val status: String? = null,
    val providerType: String? = null,
    val service: String? = null,

    val targetProviderId: String? = null,

    val assignedProviderId: String? = null,
    val assignedProviderName: String? = null,
    val assignedProviderPhone: String? = null,
    val assignedProviderRating: Double? = null,

    val userName: String? = null,
    val userPhone: String? = null,

    val vehicleInfo: String? = null,
    val problem: String? = null,
    val towType: String? = null,
    val note: String? = null,
    val urgency: String? = null,

    val userLocation: LocationDto? = null,
    val createdAt: String? = null,

    // ✅ Payment / escrow / completion fields
    val totalAmount: Double? = null,
    val amount: Double? = null,
    val price: Double? = null,
    val paymentStatus: String? = null,
    val providerCompleted: Boolean? = null,
    val userCompleted: Boolean? = null
) {
    fun resolvedId(): String = _id ?: id ?: ""
}