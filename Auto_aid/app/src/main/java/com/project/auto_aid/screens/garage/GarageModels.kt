package com.project.auto_aid.screens.garage

data class Mechanic(
    val name: String = "",
    val phone: String = "",
    val garageName: String = "",
    val specialty: String = "",
    val rating: Double = 0.0
)

data class GarageRequest(
    val id: String = "",
    val status: GarageStatus = GarageStatus.REQUEST_SENT,
    val mechanic: Mechanic? = null,
    val vehicleInfo: String = "",
    val problem: String = "",
    val serviceType: String = "General Repair"
)