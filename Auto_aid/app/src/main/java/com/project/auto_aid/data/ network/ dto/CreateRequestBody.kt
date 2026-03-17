package com.project.auto_aid.data.network.dto

data class CreateRequestBody(
    val providerType: String,
    val service: String,
    val targetProviderId: String? = null,
    val vehicleInfo: String? = "",
    val problem: String? = "",
    val note: String? = "",
    val urgency: String? = "normal",
    val towType: String? = "",
    val userLocation: LocationBody? = null
)