package com.project.auto_aid.data.network.dto

data class LocationDto(
    val lat: Double? = 0.0,
    val lng: Double? = 0.0
)

data class RequestDto(
    val _id: String? = null,
    val id: String? = null,
    val requestId: String? = null,

    val status: String? = null,
    val providerType: String? = null,
    val service: String? = null,

    val targetProviderId: String? = null,

    val assignedProviderId: String? = null,
    val assignedTo: String? = null,
    val assignedProviderName: String? = null,
    val assignedProviderPhone: String? = null,
    val assignedProviderRating: Double? = null,

    val userId: String? = null,
    val userName: String? = null,
    val userPhone: String? = null,

    val vehicleInfo: String? = null,
    val problem: String? = null,
    val towType: String? = null,
    val note: String? = null,
    val urgency: String? = null,

    val userLocation: LocationDto? = null,
    val providerLocation: LocationDto? = null,

    val createdAt: String? = null,
    val updatedAt: String? = null,

    val assignedAt: String? = null,
    val tripStartedAt: String? = null,
    val arrivedAt: String? = null,
    val quoteSentAt: String? = null,
    val paymentReadyAt: String? = null,
    val startedAt: String? = null,
    val providerCompletedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,

    val totalAmount: Double? = null,
    val amount: Double? = null,
    val price: Double? = null,
    val providerAmount: Double? = null,
    val quotedAmount: Double? = null,
    val quoteAmount: Double? = null,
    val agreedAmount: Double? = null,
    val finalAmount: Double? = null,
    val systemFee: Double? = null,

    val paymentStatus: String? = null,
    val paymentMethod: String? = null,
    val paymentAmount: Double? = null,
    val paymentPhoneNumber: String? = null,
    val paymentReference: String? = null,
    val paidAt: String? = null,
    val paymentConfirmedByProvider: Boolean? = false,
    val paymentConfirmedAt: String? = null,

    val providerCompleted: Boolean? = false,
    val userCompleted: Boolean? = false,

    val priceSetByProvider: Boolean? = false,
    val pricingStatus: String? = null
) {
    fun resolvedId(): String = requestId ?: _id ?: id ?: ""

    val safeStatus: String
        get() = status?.trim()?.lowercase()?.replace(" ", "_").orEmpty()

    val safePaymentStatus: String
        get() = paymentStatus?.trim()?.lowercase()?.replace(" ", "_").orEmpty()

    val safeService: String
        get() = service ?: providerType ?: ""

    val hasQuote: Boolean
        get() = (priceSetByProvider == true) ||
                (quoteAmount ?: 0.0) > 0.0 ||
                (quotedAmount ?: 0.0) > 0.0 ||
                (totalAmount ?: 0.0) > 0.0 ||
                (agreedAmount ?: 0.0) > 0.0 ||
                (finalAmount ?: 0.0) > 0.0

    val displayTotalAmount: Double
        get() = totalAmount
            ?: quoteAmount
            ?: agreedAmount
            ?: finalAmount
            ?: paymentAmount
            ?: amount
            ?: price
            ?: 0.0

    val displayProviderAmount: Double
        get() = providerAmount
            ?: quotedAmount
            ?: quoteAmount
            ?: 0.0

    // ---------- SIMPLE FLOW FLAGS ----------

    val isPending: Boolean
        get() = safeStatus == "pending"

    val isAccepted: Boolean
        get() = safeStatus == "accepted"

    val isStarted: Boolean
        get() = safeStatus == "started"

    val isArrived: Boolean
        get() = safeStatus == "arrived"

    val isQuotationSent: Boolean
        get() = safeStatus == "quotation_sent"

    val isPaid: Boolean
        get() = safePaymentStatus == "paid"

    val isProviderDone: Boolean
        get() = safeStatus == "provider_done"

    val isCompleted: Boolean
        get() = safeStatus == "completed"

    // ---------- PROVIDER ACTION FLAGS ----------

    val canAcceptRequest: Boolean
        get() = safeStatus == "pending"

    val canStartTrip: Boolean
        get() = safeStatus == "accepted"

    val canMarkArrived: Boolean
        get() = safeStatus == "started"

    val canSendQuotation: Boolean
        get() = safeStatus == "arrived"

    val canProviderComplete: Boolean
        get() = safePaymentStatus == "paid" &&
                safeStatus !in listOf("provider_done", "completed", "cancelled")

    // ---------- USER ACTION FLAGS ----------

    val canViewQuotation: Boolean
        get() = safeStatus in listOf("quotation_sent", "provider_done", "completed") ||
                safePaymentStatus == "paid"

    val canAcceptQuotation: Boolean
        get() = safeStatus == "quotation_sent" && safePaymentStatus != "paid"

    val canPay: Boolean
        get() = safeStatus == "quotation_sent" && safePaymentStatus != "paid"

    val canUserConfirmCompletion: Boolean
        get() = safeStatus == "provider_done"

    // ---------- HELPER LABELS ----------

    val flowStepLabel: String
        get() = when {
            safeStatus == "pending" -> "Pending"
            safeStatus == "accepted" -> "Accepted"
            safeStatus == "started" -> "Started"
            safeStatus == "arrived" -> "Arrived"
            safeStatus == "quotation_sent" && safePaymentStatus != "paid" -> "Quotation Sent"
            safeStatus == "quotation_sent" && safePaymentStatus == "paid" -> "Paid"
            safeStatus == "provider_done" -> "Provider Done"
            safeStatus == "completed" -> "Completed"
            else -> safeStatus.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
}