package com.project.auto_aid.data.network.dto

data class RouteResponseDto(
    val distanceMeters: Int = 0,
    val duration: String = "0s",
    val encodedPolyline: String = ""
)