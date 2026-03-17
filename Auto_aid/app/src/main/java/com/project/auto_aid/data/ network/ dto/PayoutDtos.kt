package com.project.auto_aid.data.network.dto

data class PayoutInfoDto(
    val method: String? = null,
    val accountName: String? = null,
    val phoneNumber: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val isVerified: Boolean? = false
)

data class UpdatePayoutInfoBody(
    val method: String,
    val accountName: String,
    val phoneNumber: String = "",
    val bankName: String = "",
    val accountNumber: String = ""
)

data class PayoutInfoResponse(
    val message: String? = null,
    val payoutInfo: PayoutInfoDto? = null
)
