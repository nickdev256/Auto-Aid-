package com.project.auto_aid.data.network.dto

data class UserWalletDto(
    val balance: Double? = 0.0,
    val totalTopUps: Double? = 0.0,
    val totalSpent: Double? = 0.0,
    val totalRefunded: Double? = 0.0
)