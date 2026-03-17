package com.project.auto_aid.data.network.dto

data class ProviderWalletDto(
    val totalEarned: Double? = 0.0,
    val pendingBalance: Double? = 0.0,
    val totalPaidOut: Double? = 0.0,
    val availableBalance: Double? = 0.0
)