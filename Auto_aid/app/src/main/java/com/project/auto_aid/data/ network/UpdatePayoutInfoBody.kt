package com.project.auto_aid.data.network

data class UpdatePayoutInfoBody(
    val accountName: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val mobileMoneyNumber: String? = null
)