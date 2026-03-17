package com.project.auto_aid.screens.ambulance

data class AmbulanceProvider(
    val name: String = "",
    val phone: String = "",
    val hospitalName: String = "",
    val ambulanceType: String = "",
    val rating: Double = 0.0
)

data class AmbulanceRequest(
    val id: String = "",
    val status: AmbulanceStatus = AmbulanceStatus.REQUEST_SENT,
    val provider: AmbulanceProvider? = null,
    val emergencyType: String = "Medical Emergency",
    val patientCondition: String = "",
    val notes: String = ""
)