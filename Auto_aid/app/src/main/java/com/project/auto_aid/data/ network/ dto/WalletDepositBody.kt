package com.project.auto_aid.data.network.dto

data class WalletDepositBody(
    val amount: Double,
    val phoneNumber: String,
    val method: String = "airtel_money"
)