package com.project.auto_aid.provider

import android.content.Context
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import com.project.auto_aid.provider.model.ProviderRequest
import kotlinx.coroutines.*

class ProviderRepository(context: Context) {

    private val tokenStore = TokenStore(context)
    private val api = RetrofitClient.create(tokenStore)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pollingJob: Job? = null
    private var locationJob: Job? = null

    // =====================================================
    // ✅ MAIN LISTENER (NEW BACKEND: pending/ongoing/completed)
    // =====================================================
    fun listenRequests(
        providerType: String,
        providerId: String, // kept for compatibility, backend already knows provider from token
        onUpdate: (List<ProviderRequest>) -> Unit
    ) {
        pollingJob?.cancel()

        val pt = providerType.trim().lowercase()
        if (pt.isBlank()) return

        pollingJob = scope.launch {

            while (isActive) {

                try {
                    // ✅ ONE call only
                    val res = api.getProviderBuckets(pt)

                    if (!res.isSuccessful) {
                        delay(3000)
                        continue
                    }

                    val body = res.body()

                    // Backend returns buckets
                    val pendingDtos = body?.pending.orEmpty()
                    val ongoingDtos = body?.ongoing.orEmpty()
                    val completedDtos = body?.completed.orEmpty()

                    // Merge & dedupe
                    val all = (pendingDtos + ongoingDtos + completedDtos)
                        .distinctBy { it.resolvedId() }

                    val mapped = all.map { dto ->
                        ProviderRequest(
                            id = dto.resolvedId(),
                            status = dto.status?.trim()?.lowercase() ?: "pending",
                            providerType = dto.providerType?.trim()?.lowercase() ?: pt,

                            targetProviderId = dto.targetProviderId?.trim()?.ifBlank { null },

                            assignedProviderId = dto.assignedProviderId ?: "",
                            assignedProviderName = dto.assignedProviderName ?: "",
                            assignedProviderPhone = dto.assignedProviderPhone ?: "",
                            assignedProviderRating = dto.assignedProviderRating ?: 0.0,

                            service = dto.service?.trim()?.lowercase() ?: "",
                            vehicleInfo = dto.vehicleInfo?.trim() ?: "",
                            problem = dto.problem?.trim() ?: "",
                            towType = dto.towType?.trim() ?: "",

                            userLocation = dto.userLocation?.let {
                                mapOf(
                                    "lat" to (it.lat ?: 0.0),
                                    "lng" to (it.lng ?: 0.0)
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

    // =====================================================
    // STOP LISTENING
    // =====================================================
    fun stopListening() {
        pollingJob?.cancel()
        pollingJob = null

        locationJob?.cancel()
        locationJob = null
    }

    // =====================================================
    // ACCEPT REQUEST
    // =====================================================
    suspend fun assignRequest(requestId: String) {
        runCatching { api.assignRequest(requestId) }
    }

    // =====================================================
    // DECLINE REQUEST
    // =====================================================
    suspend fun declineRequest(requestId: String): Boolean {
        return try {
            val res = api.declineRequest(requestId)
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    // =====================================================
    // UPDATE STATUS
    // =====================================================
    suspend fun updateStatus(
        requestId: String,
        status: String
    ) {
        runCatching {
            api.updateRequestStatus(
                requestId,
                UpdateStatusBody(status)
            )
        }
    }

    // =====================================================
    // USER LOCATION LISTENER
    // =====================================================
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
                } catch (_: Exception) {}

                delay(2000)
            }
        }
    }
}