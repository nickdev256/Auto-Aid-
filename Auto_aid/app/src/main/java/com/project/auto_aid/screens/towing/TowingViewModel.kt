package com.project.auto_aid.screens.towing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.network.ApiService
import com.project.auto_aid.data.network.dto.CreateRequestBody
import com.project.auto_aid.data.network.dto.LocationBody
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.StatusUpdateResponse
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TowingViewModel(
    private val api: ApiService
) : ViewModel() {

    private val _request = MutableStateFlow(TowingRequest())
    val request: StateFlow<TowingRequest> = _request

    private var pollingJob: Job? = null

    fun startPollingRequest(requestId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val response = api.getRequestById(requestId)
                    if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
                    response.body() ?: throw Exception("Empty body")
                }.onSuccess { dto: RequestDto ->
                    _request.value = dto.toTowingRequest()

                    val s = _request.value.status
                    if (s == TowingStatus.COMPLETED || s == TowingStatus.CANCELLED) {
                        pollingJob?.cancel()
                    }
                }

                delay(4000)
            }
        }
    }

    fun createRequest(
        vehicleInfo: String,
        problem: String,
        towType: String,
        userLat: Double,
        userLng: Double,
        targetProviderId: String? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val response = api.createRequest(
                    CreateRequestBody(
                        service = "towing",
                        providerType = "towing",
                        vehicleInfo = vehicleInfo.trim(),
                        problem = problem.trim(),
                        towType = towType,
                        userLocation = LocationBody(lat = userLat, lng = userLng),
                        targetProviderId = targetProviderId
                    )
                )
                if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
                response.body() ?: throw Exception("Empty body")
            }.onSuccess { created: RequestDto ->
                val id = created.resolvedId()
                if (id.isBlank()) return@onSuccess

                _request.value = created.toTowingRequest()
                startPollingRequest(id)
            }
        }
    }

    fun cancelRequest() {
        val id = _request.value.id
        if (id.isBlank()) return

        viewModelScope.launch {
            runCatching {
                val response = api.updateRequestStatus(
                    id,
                    UpdateStatusBody(status = "cancelled")
                )
                if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")

                val body: StatusUpdateResponse =
                    response.body() ?: throw Exception("Empty body")

                body.request ?: throw Exception("Missing request in response")
            }.onSuccess { updated: RequestDto ->
                _request.value = updated.toTowingRequest()
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}

/* -------------------- Mapper -------------------- */

private fun RequestDto.toTowingRequest(): TowingRequest {
    val backendStatus = (this.status ?: "pending").uppercase()

    val mapped = when (backendStatus) {
        "PENDING", "REQUEST_SENT" -> TowingStatus.REQUEST_SENT
        "ASSIGNED", "DRIVER_ASSIGNED" -> TowingStatus.DRIVER_ASSIGNED
        "EN_ROUTE", "ON_THE_WAY", "PROVIDER_ON_THE_WAY", "DRIVER_ON_THE_WAY" ->
            TowingStatus.DRIVER_ON_THE_WAY
        "ARRIVED" -> TowingStatus.ARRIVED
        "IN_PROGRESS" -> TowingStatus.IN_PROGRESS
        "VEHICLE_TOWED" -> TowingStatus.VEHICLE_TOWED
        "COMPLETED" -> TowingStatus.COMPLETED
        "CANCELLED" -> TowingStatus.CANCELLED
        else -> TowingStatus.REQUEST_SENT
    }

    val driver =
        if (!assignedProviderName.isNullOrBlank() || !assignedProviderPhone.isNullOrBlank()) {
            Driver(
                name = assignedProviderName ?: "",
                phone = assignedProviderPhone ?: "",
                truckPlate = "",
                truckType = "",
                rating = assignedProviderRating ?: 0.0
            )
        } else null

    return TowingRequest(
        id = this.resolvedId(),
        status = mapped,
        driver = driver,
        vehicleInfo = this.vehicleInfo ?: "",
        problem = this.problem ?: "",
        towType = this.towType ?: "Standard"
    )
}

private fun RequestDto.resolvedId(): String {
    return this.requestId ?: this._id ?: this.id ?: ""
}