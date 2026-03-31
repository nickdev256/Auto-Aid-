package com.project.auto_aid.data.network.dto

data class PaymentHistoryResponse(
    val ok: Boolean? = null,
    val message: String? = null,
    val status: String? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val data: List<PaymentHistoryDto>? = null,
    val payments: List<PaymentHistoryDto>? = null,
    val history: List<PaymentHistoryDto>? = null,
    val transactions: List<PaymentHistoryDto>? = null
) {
    fun items(): List<PaymentHistoryDto> {
        return data
            ?: payments
            ?: history
            ?: transactions
            ?: emptyList()
    }
}