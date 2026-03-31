package com.project.auto_aid.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.network.ApiService
import com.project.auto_aid.data.network.SocketManager
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.screens.AppImages
import com.project.auto_aid.screens.LiveFeaturedServiceItem
import com.project.auto_aid.screens.RecentItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class HomeViewModel(
    private val api: ApiService
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    private var serviceOrderState: List<String> =
        listOf("garage", "towing", "fuel", "ambulance")

    private var isRealtimeStarted = false

    fun loadInitialData() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isProfileLoading = true,
                isReferralLoading = true,
                error = null
            )

            try {
                coroutineScope {
                    val meDeferred = async { runCatching { api.getMe() }.getOrNull() }
                    val requestsDeferred = async { runCatching { api.getMyRequests() }.getOrNull() }
                    val referralDeferred = async { runCatching { api.getMyReferralSummary() }.getOrNull() }

                    val meResponse = meDeferred.await()
                    val requestsResponse = requestsDeferred.await()
                    val referralResponse = referralDeferred.await()

                    val userName = if (meResponse?.isSuccessful == true) {
                        meResponse.body()?.user?.name?.trim()
                            .takeIf { !it.isNullOrBlank() } ?: "User"
                    } else {
                        "User"
                    }

                    val requests = if (requestsResponse?.isSuccessful == true) {
                        requestsResponse.body().orEmpty()
                    } else {
                        emptyList()
                    }

                    val sortedRequests =
                        requests.sortedByDescending { parseServerTimeMillis(it.updatedAt ?: it.createdAt) }

                    val recents = sortedRequests.take(10).map(::requestToRecentItem)

                    serviceOrderState = serviceUsageOrderFromRequests(sortedRequests)

                    uiState = uiState.copy(
                        userName = userName,
                        notificationCount = 5,
                        recentItems = recents,
                        featuredTitle = if (recents.isNotEmpty()) {
                            "Featured Based on Your Activity"
                        } else {
                            "Featured Services"
                        },
                        referralCode = referralResponse?.referralCode.orEmpty(),
                        nextReferralDiscountAmount = referralResponse?.nextReferralDiscountAmount ?: 0.0,
                        rewardedReferralCount = referralResponse?.rewardedCount ?: 0,
                        totalReferralCount = referralResponse?.totalReferrals ?: 0,
                        isProfileLoading = false,
                        isReferralLoading = false
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isProfileLoading = false,
                    isReferralLoading = false,
                    error = e.message ?: "Failed to load home data"
                )
            }
        }
    }

    fun loadProviders(lat: Double, lng: Double) {
        viewModelScope.launch {
            uiState = uiState.copy(isProvidersLoading = true, error = null)

            try {
                val grouped = coroutineScope {
                    val garageDeferred =
                        async { runCatching { api.getAvailableProviders("garage", lat, lng) }.getOrNull() }
                    val towingDeferred =
                        async { runCatching { api.getAvailableProviders("towing", lat, lng) }.getOrNull() }
                    val fuelDeferred =
                        async { runCatching { api.getAvailableProviders("fuel", lat, lng) }.getOrNull() }
                    val ambulanceDeferred =
                        async { runCatching { api.getAvailableProviders("ambulance", lat, lng) }.getOrNull() }

                    linkedMapOf(
                        "garage" to garageDeferred.await()?.body().orEmpty(),
                        "towing" to towingDeferred.await()?.body().orEmpty(),
                        "fuel" to fuelDeferred.await()?.body().orEmpty(),
                        "ambulance" to ambulanceDeferred.await()?.body().orEmpty()
                    )
                }

                val mapped = linkedMapOf(
                    "garage" to mutableListOf<LiveFeaturedServiceItem>(),
                    "towing" to mutableListOf<LiveFeaturedServiceItem>(),
                    "fuel" to mutableListOf<LiveFeaturedServiceItem>(),
                    "ambulance" to mutableListOf<LiveFeaturedServiceItem>()
                )

                grouped.forEach { (serviceKey, providers) ->
                    mapped[serviceKey]?.addAll(
                        providers.take(3).map { provider ->
                            LiveFeaturedServiceItem(
                                name = provider.name ?: "${serviceDisplayName(serviceKey)} Provider",
                                subtitle = if (uiState.recentItems.any { normalizeServiceKey(it.service) == serviceKey }) {
                                    "Based on your activity"
                                } else {
                                    "Online provider"
                                },
                                type = provider.businessType ?: serviceDisplayName(serviceKey),
                                rating = provider.rating ?: 0.0,
                                imageRes = featuredImageFor(serviceKey)
                            )
                        }
                    )
                }

                val prioritized = serviceOrderState.flatMap { mapped[it].orEmpty() }

                uiState = uiState.copy(
                    featuredServices = prioritized.ifEmpty {
                        listOf(
                            LiveFeaturedServiceItem(
                                name = "No providers online",
                                subtitle = "Try again later",
                                type = "AutoAid",
                                rating = 0.0,
                                imageRes = AppImages.shell
                            )
                        )
                    },
                    isProvidersLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isProvidersLoading = false,
                    error = e.message ?: "Failed to load providers"
                )
            }
        }
    }

    fun loadReferralSummary() {
        viewModelScope.launch {
            uiState = uiState.copy(isReferralLoading = true)

            try {
                val res = api.getMyReferralSummary()
                uiState = uiState.copy(
                    referralCode = res.referralCode.orEmpty(),
                    nextReferralDiscountAmount = res.nextReferralDiscountAmount ?: 0.0,
                    rewardedReferralCount = res.rewardedCount ?: 0,
                    totalReferralCount = res.totalReferrals ?: 0,
                    isReferralLoading = false
                )
            } catch (_: Exception) {
                uiState = uiState.copy(isReferralLoading = false)
            }
        }
    }

    fun startRealtimeUpdates() {
        if (isRealtimeStarted) return
        isRealtimeStarted = true

        SocketManager.listenRequestUpdated { json ->
            val updatedRequest = jsonToRequestDto(json)
            if (updatedRequest.resolvedId().isNotBlank()) {
                viewModelScope.launch {
                    mergeRecentRequest(updatedRequest)
                }
            }
        }

        SocketManager.listenNewRequestBroadcast { json ->
            val newRequest = jsonToRequestDto(json)
            if (newRequest.resolvedId().isNotBlank()) {
                viewModelScope.launch {
                    mergeRecentRequest(newRequest)
                }
            }
        }
    }

    fun stopRealtimeUpdates() {
        SocketManager.stopListeningRequestUpdated()
        SocketManager.clearTrackingListeners()
        isRealtimeStarted = false
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeUpdates()
    }

    private fun mergeRecentRequest(request: RequestDto) {
        val requestId = request.resolvedId()
        if (requestId.isBlank()) return

        val updatedItem = requestToRecentItem(request)
        val current = uiState.recentItems.toMutableList()

        val index = current.indexOfFirst { it.requestId == requestId }

        if (index >= 0) {
            current[index] = updatedItem
        } else {
            current.add(0, updatedItem)
        }

        val sorted = current
            .distinctBy { it.requestId }
            .sortedByDescending { parseRecentDisplayTime(it.date) }
            .take(10)

        serviceOrderState = serviceUsageOrderFromRecentItems(sorted)

        uiState = uiState.copy(
            recentItems = sorted,
            featuredTitle = if (sorted.isNotEmpty()) {
                "Featured Based on Your Activity"
            } else {
                "Featured Services"
            }
        )
    }

    private fun jsonToRequestDto(json: JSONObject): RequestDto {
        return RequestDto(
            _id = json.optString("_id").takeIf { it.isNotBlank() },
            id = json.optString("id").takeIf { it.isNotBlank() },
            requestId = json.optString("requestId").takeIf { it.isNotBlank() },
            status = json.optString("status").takeIf { it.isNotBlank() },
            providerType = json.optString("providerType").takeIf { it.isNotBlank() },
            service = json.optString("service").takeIf { it.isNotBlank() },
            assignedProviderId = json.optString("assignedProviderId").takeIf { it.isNotBlank() },
            assignedProviderName = json.optString("assignedProviderName").takeIf { it.isNotBlank() },
            assignedProviderPhone = json.optString("assignedProviderPhone").takeIf { it.isNotBlank() },
            userName = json.optString("userName").takeIf { it.isNotBlank() },
            userPhone = json.optString("userPhone").takeIf { it.isNotBlank() },
            vehicleInfo = json.optString("vehicleInfo").takeIf { it.isNotBlank() },
            problem = json.optString("problem").takeIf { it.isNotBlank() },
            towType = json.optString("towType").takeIf { it.isNotBlank() },
            note = json.optString("note").takeIf { it.isNotBlank() },
            urgency = json.optString("urgency").takeIf { it.isNotBlank() },
            createdAt = json.optString("createdAt").takeIf { it.isNotBlank() },
            updatedAt = json.optString("updatedAt").takeIf { it.isNotBlank() }
        )
    }

    private fun normalizeServiceKey(value: String?): String {
        return when (value?.trim()?.lowercase()) {
            "fuel", "fuel delivery" -> "fuel"
            "garage", "garage repair" -> "garage"
            "towing", "tow", "towing service", "towing track" -> "towing"
            "ambulance", "ambulance service" -> "ambulance"
            else -> ""
        }
    }

    private fun serviceDisplayName(service: String?): String {
        return when (normalizeServiceKey(service)) {
            "fuel" -> "Fuel Delivery"
            "garage" -> "Garage"
            "towing" -> "Towing Service"
            "ambulance" -> "Ambulance Service"
            else -> "AutoAid Service"
        }
    }

    private fun featuredImageFor(service: String?): Int {
        return when (normalizeServiceKey(service)) {
            "fuel" -> AppImages.shell
            "garage" -> AppImages.stabex
            "towing" -> AppImages.total
            "ambulance" -> AppImages.rubis
            else -> AppImages.shell
        }
    }

    private fun formatStatus(status: String?): String {
        return when (status?.trim()?.lowercase()) {
            "pending", "request_sent" -> "Pending"
            "assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned" -> "Assigned"
            "driver_on_the_way", "mechanic_on_the_way", "vendor_on_the_way", "ambulance_on_the_way" -> "On Going"
            "arrived" -> "Arrived"
            "in_progress", "delivering", "patient_picked", "vehicle_towed", "repaired", "job_started", "started" -> "On Going"
            "quotation_sent", "quote_sent" -> "Quotation Sent"
            "awaiting_payment" -> "Awaiting Payment"
            "paid", "payment_confirmed" -> "Paid"
            "delivered", "at_hospital", "completed", "job_done" -> "Completed"
            "cancelled" -> "Cancelled"
            else -> status?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "Unknown"
        }
    }

    private fun parseServerDateToDisplay(value: String?): String {
        if (value.isNullOrBlank()) return "Unknown time"

        val inputPatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (pattern in inputPatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault())
                val date = parser.parse(value)
                if (date != null) {
                    val formatter = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
                    return formatter.format(date)
                }
            } catch (_: Exception) {
            }
        }

        return value
    }

    private fun parseServerTimeMillis(value: String?): Long {
        if (value.isNullOrBlank()) return 0L

        val inputPatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (pattern in inputPatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault())
                val date = parser.parse(value)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }

        return 0L
    }

    private fun parseRecentDisplayTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        return try {
            SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).parse(value)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun requestToRecentItem(req: RequestDto): RecentItem {
        return RecentItem(
            requestId = req.resolvedId(),
            service = serviceDisplayName(req.service ?: req.providerType),
            date = parseServerDateToDisplay(req.updatedAt ?: req.createdAt),
            status = formatStatus(req.status),
            icon = when (normalizeServiceKey(req.service ?: req.providerType)) {
                "fuel" -> Icons.Default.LocalGasStation
                "garage" -> Icons.Default.CarRepair
                "towing" -> Icons.Default.LocalShipping
                "ambulance" -> Icons.Default.MedicalServices
                else -> Icons.Default.Build
            }
        )
    }

    private fun serviceUsageOrderFromRequests(requests: List<RequestDto>): List<String> {
        val counts = linkedMapOf(
            "garage" to 0,
            "towing" to 0,
            "fuel" to 0,
            "ambulance" to 0
        )

        requests.forEach { req ->
            val key = normalizeServiceKey(req.service ?: req.providerType)
            if (counts.containsKey(key)) {
                counts[key] = (counts[key] ?: 0) + 1
            }
        }

        val sorted = counts.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val fallback = listOf("garage", "towing", "fuel", "ambulance")
        return (sorted + fallback).distinct()
    }

    private fun serviceUsageOrderFromRecentItems(items: List<RecentItem>): List<String> {
        val counts = linkedMapOf(
            "garage" to 0,
            "towing" to 0,
            "fuel" to 0,
            "ambulance" to 0
        )

        items.forEach { item ->
            val key = normalizeServiceKey(item.service)
            if (counts.containsKey(key)) {
                counts[key] = (counts[key] ?: 0) + 1
            }
        }

        val sorted = counts.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val fallback = listOf("garage", "towing", "fuel", "ambulance")
        return (sorted + fallback).distinct()
    }
}

private fun RequestDto.resolvedId(): String {
    return when {
        !this.requestId.isNullOrBlank() -> this.requestId
        !this.id.isNullOrBlank() -> this.id
        !this._id.isNullOrBlank() -> this._id
        else -> ""
    } ?: ""
}