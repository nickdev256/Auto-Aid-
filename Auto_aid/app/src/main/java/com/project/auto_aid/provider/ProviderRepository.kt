package com.project.auto_aid.provider

import android.content.Context
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import com.project.auto_aid.provider.model.ProviderRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProviderRepository(context: Context) {

    private val tokenStore = TokenStore(context)
    private val api = RetrofitClient.create(tokenStore)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pollingJob: Job? = null
    private var locationJob: Job? = null

    fun listenRequests(
        providerType: String,
        providerId: String,
        onUpdate: (List<ProviderRequest>) -> Unit
    ) {
        pollingJob?.cancel()

        val pt = providerType.trim().lowercase()
        if (pt.isBlank()) return

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val res = api.getProviderBuckets(pt)

                    if (!res.isSuccessful) {
                        delay(3000)
                        continue
                    }

                    val body = res.body()

                    val pendingDtos: List<RequestDto> = body?.pending ?: emptyList()
                    val ongoingDtos: List<RequestDto> = body?.ongoing ?: emptyList()
                    val completedDtos: List<RequestDto> = body?.completed ?: emptyList()

                    val all: List<RequestDto> =
                        (pendingDtos + ongoingDtos + completedDtos)
                            .distinctBy { dto -> dto.resolvedId() }

                    val mapped: List<ProviderRequest> = all.map { dto ->
                        ProviderRequest(
                            id = dto.resolvedId(),
                            status = dto.status?.trim()?.lowercase() ?: "pending",
                            providerType = dto.providerType?.trim()?.lowercase() ?: pt,

                            targetProviderId = dto.targetProviderId?.trim()?.ifBlank { null },

                            assignedProviderId = dto.assignedProviderId ?: "",
                            assignedProviderName = dto.assignedProviderName ?: "",
                            assignedProviderPhone = dto.assignedProviderPhone ?: "",
                            assignedProviderRating = dto.assignedProviderRating ?: 0.0,

                            customerName = dto.userName?.trim().orEmpty(),
                            customerPhone = dto.userPhone?.trim().orEmpty(),

                            service = dto.service?.trim()?.lowercase().orEmpty(),
                            vehicleInfo = dto.vehicleInfo?.trim().orEmpty(),
                            problem = dto.problem?.trim().orEmpty(),
                            towType = dto.towType?.trim().orEmpty(),
                            note = dto.note?.trim().orEmpty(),
                            urgency = dto.urgency?.trim()?.lowercase().orEmpty(),

                            createdAt = dto.createdAt?.trim().orEmpty(),

                            totalAmount = dto.totalAmount ?: 0.0,
                            amount = dto.amount ?: 0.0,
                            price = dto.price ?: 0.0,
                            paymentStatus = dto.paymentStatus?.trim()?.lowercase().orEmpty(),
                            paymentConfirmedByProvider = dto.paymentConfirmedByProvider ?: false,

                            providerCompleted = dto.providerCompleted ?: false,
                            userCompleted = dto.userCompleted ?: false,

                            userLocation = dto.userLocation?.let { location ->
                                mapOf(
                                    "lat" to (location.lat ?: 0.0),
                                    "lng" to (location.lng ?: 0.0)
                                )
                            } ?: mapOf(
                                "lat" to 0.0,
                                "lng" to 0.0
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        onUpdate(mapped)
                    }
                } catch (_: Exception) {
                }

                delay(3000)
            }
        }
    }

    fun stopListening() {
        pollingJob?.cancel()
        pollingJob = null

        locationJob?.cancel()
        locationJob = null
    }

    suspend fun assignRequest(requestId: String): Boolean {
        return try {
            val res = api.assignRequest(requestId)
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun declineRequest(requestId: String): Boolean {
        return try {
            val res = api.declineRequest(requestId)
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateStatus(
        requestId: String,
        status: String
    ): Boolean {
        return try {
            val res = api.updateRequestStatus(
                requestId,
                UpdateStatusBody(status = status)
            )
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    fun listenUserLocation(
        requestId: String,
        onUpdate: (Double, Double) -> Unit
    ) {
        locationJob?.cancel()

        locationJob = scope.launch {
            while (isActive) {
                try {
                    val res = api.getUserLocation(requestId)
                    if (res.isSuccessful) {
                        val body = res.body()
                        if (body != null) {
                            withContext(Dispatchers.Main) {
                                onUpdate(body.lat, body.lng)
                            }
                        }
                    }
                } catch (_: Exception) {
                }

                delay(2000)
            }
        }
    }
}