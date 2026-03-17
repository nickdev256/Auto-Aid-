package com.project.auto_aid.data.network.dto

import com.google.gson.annotations.SerializedName

data class ProviderLiteDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("_id")
    val mongoId: String? = null,

    val name: String? = null,
    val phone: String? = null,
    val businessName: String? = null,
    val businessType: String? = null,
    val servicesOffered: List<String> = emptyList(),
    val address: String? = null,
    val rating: Double? = 0.0,
    val lat: Double? = null,
    val lng: Double? = null,
    val isAvailable: Boolean? = false,
    val isOnline: Boolean? = false,
    val isApprovedProvider: Boolean? = false,
    val profileImageUrl: String? = null,
    val logoUrl: String? = null,
    val distanceKm: Double? = null
) {
    fun resolvedId(): String {
        return when {
            !id.isNullOrBlank() -> id
            !mongoId.isNullOrBlank() -> mongoId
            else -> ""
        }
    }
}