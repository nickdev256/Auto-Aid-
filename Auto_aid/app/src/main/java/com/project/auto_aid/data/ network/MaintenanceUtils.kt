package com.project.auto_aid.data.network

import com.google.gson.Gson
import com.project.auto_aid.data.network.dto.MaintenanceResponse

object MaintenanceUtils {
    private val gson = Gson()

    fun parseMaintenanceMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        return try {
            val parsed = gson.fromJson(raw, MaintenanceResponse::class.java)
            if (parsed?.maintenanceMode == true) {
                parsed.message ?: "AutoAid is currently under maintenance. Please try again later."
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}