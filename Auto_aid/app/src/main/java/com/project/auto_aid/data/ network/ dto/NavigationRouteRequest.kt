package com.project.auto_aid.data.network.dto

data class NavigationRouteRequest(
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double
)