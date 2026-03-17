package com.project.auto_aid.data.network

data class PayoutInfoDto(
    val accountName: String?,
    val accountNumber: String?,
    val bankName: String?,
    val mobileMoneyNumber: String?,
    val providerId: String?
)