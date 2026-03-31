package com.project.auto_aid.data.network.dto

data class ExtraChargeDto(
    val _id: String? = null,
    val requestId: String? = null,
    val providerId: String? = null,
    val amount: Double? = 0.0,
    val reason: String? = null,
    val note: String? = null,
    val status: String? = null,
    val paymentMethod: String? = null,
    val paidAt: String? = null,
    val createdAt: String? = null
) {
    val id: String?
        get() = _id
}