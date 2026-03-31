package com.project.auto_aid.screens.ambulance

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

class AmbulanceViewModel(
    private val api: ApiService
) : ViewModel() {

    private val _request: MutableStateFlow<AmbulanceRequest> =
        MutableStateFlow(AmbulanceRequest())

    val request: StateFlow<AmbulanceRequest> = _request

    private var pollingJob: Job? = null

    fun startPollingRequest(requestId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val response = api.getRequestById(requestId)
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code()}")
                    }
                    response.body() ?: throw Exception("Empty body")
                }.onSuccess { dto ->
                    _request.value = dto.toAmbulanceRequest()

                    val s = _request.value.status
                    if (s == AmbulanceStatus.COMPLETED || s == AmbulanceStatus.CANCELLED) {
                        pollingJob?.cancel()
                    }
                }

                delay(4000)
            }
        }
    }

    fun createRequest(
        emergencyType: String,
        patientCondition: String,
        notes: String,
        userLat: Double,
        userLng: Double,
        targetProviderId: String? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val response = api.createRequest(
                    CreateRequestBody(
                        service = "ambulance",
                        providerType = "ambulance",
                        vehicleInfo = emergencyType.trim(),
                        problem = buildString {
                            append(patientCondition.trim())
                            if (notes.isNotBlank()) {
                                append(" | Notes: ")
                                append(notes.trim())
                            }
                        },
                        towType = emergencyType.trim(),
                        userLocation = LocationBody(lat = userLat, lng = userLng),
                        targetProviderId = targetProviderId
                    )
                )

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code()}")
                }

                response.body() ?: throw Exception("Empty body")
            }.onSuccess { created ->
                val id = created.resolvedId()
                if (id.isBlank()) return@onSuccess

                _request.value = created.toAmbulanceRequest()
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

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code()}")
                }

                val body: StatusUpdateResponse =
                    response.body() ?: throw Exception("Empty body")

                body.request ?: throw Exception("Missing request in response")
            }.onSuccess { updatedRequest: RequestDto ->
                _request.value = updatedRequest.toAmbulanceRequest()
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}

private fun RequestDto.toAmbulanceRequest(): AmbulanceRequest {
    val backendStatus = (this.status ?: "pending").uppercase()

    val mapped = when (backendStatus) {
        "PENDING", "REQUEST_SENT" -> AmbulanceStatus.REQUEST_SENT
        "ASSIGNED", "DRIVER_ASSIGNED" -> AmbulanceStatus.DRIVER_ASSIGNED
        "EN_ROUTE", "ON_THE_WAY", "PROVIDER_ON_THE_WAY", "DRIVER_ON_THE_WAY", "AMBULANCE_ON_THE_WAY" ->
            AmbulanceStatus.AMBULANCE_ON_THE_WAY
        "ARRIVED" -> AmbulanceStatus.ARRIVED
        "IN_PROGRESS", "PATIENT_PICKED" -> AmbulanceStatus.PATIENT_PICKED
        "AT_HOSPITAL" -> AmbulanceStatus.AT_HOSPITAL
        "COMPLETED" -> AmbulanceStatus.COMPLETED
        "CANCELLED" -> AmbulanceStatus.CANCELLED
        else -> AmbulanceStatus.REQUEST_SENT
    }

    val provider =
        if (!assignedProviderName.isNullOrBlank() || !assignedProviderPhone.isNullOrBlank()) {
            AmbulanceProvider(
                name = assignedProviderName ?: "",
                phone = assignedProviderPhone ?: "",
                hospitalName = assignedProviderName ?: "",
                ambulanceType = towType ?: "",
                rating = assignedProviderRating ?: 0.0
            )
        } else null

    val rawProblem = this.problem ?: ""
    val parts = rawProblem.split("| Notes: ")
    val condition = parts.getOrNull(0)?.trim().orEmpty()
    val notes = parts.getOrNull(1)?.trim().orEmpty()

    return AmbulanceRequest(
        id = resolvedId(),
        status = mapped,
        provider = provider,
        emergencyType = this.towType ?: this.vehicleInfo ?: "Medical Emergency",
        patientCondition = condition,
        notes = notes
    )
}

private fun RequestDto.resolvedId(): String {
    return this.requestId ?: this._id ?: this.id ?: ""
}