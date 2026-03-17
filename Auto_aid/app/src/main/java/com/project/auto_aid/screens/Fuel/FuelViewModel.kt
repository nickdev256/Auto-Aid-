package com.project.auto_aid.screens.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.network.ApiService
import com.project.auto_aid.data.network.dto.CreateRequestBody
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import com.project.auto_aid.data.network.dto.LocationBody
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FuelViewModel(
    private val api: ApiService
) : ViewModel() {

    private val _request = MutableStateFlow(FuelRequest())
    val request: StateFlow<FuelRequest> = _request

    private var pollingJob: Job? = null

    fun startPollingRequest(requestId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val response = api.getRequestById(requestId)
                    if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
                    response.body() ?: throw Exception("Empty body")
                }.onSuccess { dto ->
                    _request.value = dto.toFuelRequest()

                    val s = _request.value.status
                    if (s == FuelStatus.COMPLETED || s == FuelStatus.CANCELLED) {
                        pollingJob?.cancel()
                    }
                }

                delay(4000)
            }
        }
    }

    fun createRequest(
        fuelType: String,
        quantity: String,
        paymentMethod: String,
        userLat: Double,
        userLng: Double,
        targetProviderId: String? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val response = api.createRequest(
                    CreateRequestBody(
                        service = "fuel",
                        providerType = "fuel",
                        vehicleInfo = "$fuelType • $quantity L",
                        problem = "Payment: $paymentMethod",
                        towType = fuelType,
                        userLocation = LocationBody(lat = userLat, lng = userLng),
                        targetProviderId = targetProviderId
                    )
                )
                if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
                response.body() ?: throw Exception("Empty body")
            }.onSuccess { created ->
                val id = created.resolvedId()
                if (id.isBlank()) return@onSuccess

                _request.value = created.toFuelRequest()
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
                response.body() ?: throw Exception("Empty body")
            }.onSuccess { updated ->
                _request.value = updated.toFuelRequest()
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}

private fun RequestDto.toFuelRequest(): FuelRequest {
    val backendStatus = (this.status ?: "pending").uppercase()

    val mapped = when (backendStatus) {
        "PENDING", "REQUEST_SENT" -> FuelStatus.REQUEST_SENT
        "ASSIGNED", "VENDOR_ASSIGNED" -> FuelStatus.VENDOR_ASSIGNED
        "DRIVER_ON_THE_WAY", "VENDOR_ON_THE_WAY" -> FuelStatus.VENDOR_ON_THE_WAY
        "ARRIVED" -> FuelStatus.ARRIVED
        "IN_PROGRESS", "DELIVERING" -> FuelStatus.DELIVERING
        "DELIVERED" -> FuelStatus.DELIVERED
        "COMPLETED" -> FuelStatus.COMPLETED
        "CANCELLED" -> FuelStatus.CANCELLED
        else -> FuelStatus.REQUEST_SENT
    }

    val vendor =
        if (!assignedProviderName.isNullOrBlank() || !assignedProviderPhone.isNullOrBlank()) {
            FuelVendor(
                name = assignedProviderName ?: "",
                phone = assignedProviderPhone ?: "",
                stationName = assignedProviderName ?: "",
                fuelType = towType ?: "",
                rating = assignedProviderRating ?: 0.0
            )
        } else null

    return FuelRequest(
        id = this.resolvedId(),
        status = mapped,
        vendor = vendor,
        fuelType = this.towType ?: "Petrol",
        quantity = this.vehicleInfo ?: "",
        paymentMethod = this.problem ?: "Cash"
    )
}