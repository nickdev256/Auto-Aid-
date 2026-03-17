package com.project.auto_aid.data.network.dto

data class UpdateAvailabilityBody(
    val isAvailable: Boolean,
    val lat: Double? = null,
    val lng: Double? = null
)