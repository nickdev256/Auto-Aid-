package com.project.auto_aid.screens.ambulance

data class AmbulanceProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val hospitalName: String = "",
    val providerType: String = "",
    val ambulanceType: String = "",
    val rating: Double = 0.0,
    val isOnline: Boolean = false
)

data class AmbulanceRequest(
    val id: String = "",
    val status: AmbulanceStatus = AmbulanceStatus.REQUEST_SENT,
    val provider: AmbulanceProvider? = null,
    val emergencyType: String = "Medical Emergency",
    val patientCondition: String = "",
    val notes: String = ""
)