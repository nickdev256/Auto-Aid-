package com.project.auto_aid.data.network.dto

data class CreatePaymentBody(
    val requestId: String,
    val amount: Double,
    val method: String, // airtel_money, cash, wallet
    val phoneNumber: String? = null,
    val reference: String? = null
)