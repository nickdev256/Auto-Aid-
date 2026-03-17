package com.project.auto_aid.data.network.dto

data class PayoutRequestDto(
    val _id: String? = null,
    val id: String? = null,
    val providerId: String? = null,
    val amount: Double? = 0.0,
    val method: String? = null,
    val accountName: String? = null,
    val phoneNumber: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val status: String? = null,
    val adminNote: String? = null,
    val paidAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)