package com.project.auto_aid.data.network.dto

data class MaintenanceResponse(
    val ok: Boolean? = null,
    val maintenanceMode: Boolean? = null,
    val message: String? = null,
    val systemName: String? = null
)