package com.project.auto_aid.screens.fuel

data class FuelVendor(
    val name: String = "",
    val phone: String = "",
    val stationName: String = "",
    val fuelType: String = "",
    val rating: Double = 0.0
)

data class FuelRequest(
    val id: String = "",
    val status: FuelStatus = FuelStatus.REQUEST_SENT,
    val vendor: FuelVendor? = null,
    val fuelType: String = "Petrol",
    val quantity: String = "",
    val paymentMethod: String = "Cash"
)