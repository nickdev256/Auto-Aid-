package com.project.auto_aid.data.network.dto

data class PaymentHistoryDto(
    val _id: String? = null,
    val id: String? = null,
    val requestId: String? = null,
    val amount: Double? = 0.0,
    val method: String? = null,
    val paymentStatus: String? = null,
    val reference: String? = null,
    val createdAt: String? = null,
    val serviceName: String? = null,
    val providerName: String? = null,
    val paymentConfirmedByProvider: Boolean? = false
)