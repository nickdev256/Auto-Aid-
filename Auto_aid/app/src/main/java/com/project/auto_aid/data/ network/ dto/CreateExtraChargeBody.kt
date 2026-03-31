package com.project.auto_aid.data.network.dto

data class CreateExtraChargeBody(
    val requestId: String,
    val amount: Double,
    val reason: String,
    val note: String = ""
)