package com.project.auto_aid.data.network.dto

data class SetRequestPriceBody(
    val providerAmount: Double
)
data class RequestQuoteDto(
    val providerAmount: Double? = 0.0,
    val systemFee: Double? = 0.0,
    val totalAmount: Double? = 0.0,
    val pricingStatus: String? = "",
    val paymentStatus: String? = "",
    val paymentConfirmedByProvider: Boolean? = false
) {
    val priceSetByProvider: Boolean
        get() = (providerAmount ?: 0.0) > 0.0 ||
                (totalAmount ?: 0.0) > 0.0
}

data class SetRequestPriceResponse(
    val message: String? = null,
    val request: RequestQuoteDto? = null
)