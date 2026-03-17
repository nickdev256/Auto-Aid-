package com.project.auto_aid.data.network.dto

data class PaymentResponse(
    val message: String? = null,
    val status: String? = null,
    val requestId: String? = null,
    val amount: Double? = null,
    val method: String? = null,
    val reference: String? = null
)