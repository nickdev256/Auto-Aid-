package com.project.auto_aid.data.network.dto

data class SetRequestPriceBody(
    val providerAmount: Double
)

data class RequestQuoteDto(
    val _id: String? = null,
    val status: String? = null,
    val pricingStatus: String? = null,
    val paymentStatus: String? = null,
    val providerAmount: Double? = 0.0,
    val systemFee: Double? = 0.0,
    val totalAmount: Double? = 0.0,
    val priceSetByProvider: Boolean? = false,
    val priceSetAt: String? = null,
    val paymentMethod: String? = null,
    val paymentReference: String? = null,
    val paidAt: String? = null
)

data class SetRequestPriceResponse(
    val message: String? = null,
    val request: RequestQuoteDto? = null
)