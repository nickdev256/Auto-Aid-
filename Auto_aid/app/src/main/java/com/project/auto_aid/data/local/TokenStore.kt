package com.project.auto_aid.data.local

import android.content.Context

class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences("autoaid_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_USER_ID = "userId"

        private const val KEY_LAST_TOWING_ID = "last_towing_request_id"
        private const val KEY_LAST_GARAGE_ID = "last_garage_request_id"
        private const val KEY_LAST_FUEL_ID = "last_fuel_request_id"
        private const val KEY_LAST_AMBULANCE_ID = "last_ambulance_request_id"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun saveRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun saveUserId(id: String) {
        prefs.edit().putString(KEY_USER_ID, id).apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun saveSession(token: String, role: String, userId: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    /* =========================
       Towing
       ========================= */
    fun saveLastTowingRequestId(requestId: String) {
        prefs.edit().putString(KEY_LAST_TOWING_ID, requestId).apply()
    }

    fun getLastTowingRequestId(): String? =
        prefs.getString(KEY_LAST_TOWING_ID, null)

    fun clearLastTowingRequestId() {
        prefs.edit().remove(KEY_LAST_TOWING_ID).apply()
    }

    /* =========================
       Garage
       ========================= */
    fun saveLastGarageRequestId(requestId: String) {
        prefs.edit().putString(KEY_LAST_GARAGE_ID, requestId).apply()
    }

    fun getLastGarageRequestId(): String? =
        prefs.getString(KEY_LAST_GARAGE_ID, null)

    fun clearLastGarageRequestId() {
        prefs.edit().remove(KEY_LAST_GARAGE_ID).apply()
    }

    /* =========================
       Fuel
       ========================= */
    fun saveLastFuelRequestId(requestId: String) {
        prefs.edit().putString(KEY_LAST_FUEL_ID, requestId).apply()
    }

    fun getLastFuelRequestId(): String? =
        prefs.getString(KEY_LAST_FUEL_ID, null)

    fun clearLastFuelRequestId() {
        prefs.edit().remove(KEY_LAST_FUEL_ID).apply()
    }

    /* =========================
       Ambulance
       ========================= */
    fun saveLastAmbulanceRequestId(requestId: String) {
        prefs.edit().putString(KEY_LAST_AMBULANCE_ID, requestId).apply()
    }

    fun getLastAmbulanceRequestId(): String? =
        prefs.getString(KEY_LAST_AMBULANCE_ID, null)

    fun clearLastAmbulanceRequestId() {
        prefs.edit().remove(KEY_LAST_AMBULANCE_ID).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}