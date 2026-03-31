package com.project.auto_aid.data.network.dto

data class WalletDepositStatusResponse(
    val ok: Boolean? = null,
    val status: String? = null,
    val amount: Double? = null,
    val reference: String? = null,
    val balance: Double? = null
)