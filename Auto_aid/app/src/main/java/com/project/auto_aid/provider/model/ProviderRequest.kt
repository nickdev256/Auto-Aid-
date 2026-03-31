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

    // Customer info
    val customerName: String = "",
    val customerPhone: String = "",

    // Request details
    val service: String = "",
    val vehicleInfo: String = "",
    val problem: String = "",
    val towType: String = "",
    val note: String = "",
    val urgency: String = "",

    // Time
    val createdAt: String = "",

    // Money / payment
    val totalAmount: Double = 0.0,
    val amount: Double = 0.0,
    val price: Double = 0.0,
    val paymentStatus: String = "",
    val paymentConfirmedByProvider: Boolean = false,

    // Completion flags
    val providerCompleted: Boolean = false,
    val userCompleted: Boolean = false,

    // Location
    val userLocation: Map<String, Double> = emptyMap()
)