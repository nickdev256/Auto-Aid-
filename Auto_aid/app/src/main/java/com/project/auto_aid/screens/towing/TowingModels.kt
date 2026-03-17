package com.project.auto_aid.screens.towing

data class Driver(
    val name: String = "",
    val phone: String = "",
    val truckPlate: String = "",
    val truckType: String = "",
    val rating: Double = 0.0
)

data class TowingRequest(
    val id: String = "",
    val status: TowingStatus = TowingStatus.REQUEST_SENT,
    val driver: Driver? = null,
    val vehicleInfo: String = "",
    val problem: String = "",
    val towType: String = "Standard",
    val userId: String = ""
)