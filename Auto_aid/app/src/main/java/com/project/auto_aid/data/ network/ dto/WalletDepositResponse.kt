package com.project.auto_aid.data.network.dto

data class WalletDepositResponse(
    val ok: Boolean? = null,
    val message: String? = null,
    val status: String? = null,
    val amount: Double? = null,
    val reference: String? = null,
    val phoneNumber: String? = null,
    val balance: Double? = null
)