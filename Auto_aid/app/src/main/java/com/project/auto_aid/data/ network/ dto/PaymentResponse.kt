package com.project.auto_aid.data.network.dto

data class PaymentResponse(
    val ok: Boolean? = null,
    val message: String? = null,
    val status: String? = null,
    val requestId: String? = null,
    val amount: Double? = null,
    val method: String? = null,
    val paymentStatus: String? = null,
    val paymentConfirmedByProvider: Boolean? = false,
    val reference: String? = null,
    val txRef: String? = null
)

