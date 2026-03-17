package com.project.auto_aid.data.repo

import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.ApiService
import com.project.auto_aid.data.network.MaintenanceException
import com.project.auto_aid.data.network.MaintenanceUtils
import com.project.auto_aid.data.network.dto.AuthResponse
import com.project.auto_aid.data.network.dto.LoginRequest

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {

    suspend fun login(email: String, password: String): AuthResponse {

        val response = api.login(
            LoginRequest(
                email.trim().lowercase(),
                password.trim()
            )
        )

        if (!response.isSuccessful) {

            val errorText = try {
                response.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            // 🔧 Detect maintenance mode
            val maintenanceMessage =
                MaintenanceUtils.parseMaintenanceMessage(errorText)

            if (response.code() == 503 || maintenanceMessage != null) {
                throw MaintenanceException(
                    maintenanceMessage
                        ?: "AutoAid is currently under maintenance. Please try again later."
                )
            }

            throw Exception(
                errorText ?: "Login failed (${response.code()})"
            )
        }

        val body = response.body()
            ?: throw Exception("Empty server response")

        // ✅ validate required fields
        val role = body.user.role
            ?: throw Exception("Invalid server response: role is null")

        val userId = body.user._id
            ?: throw Exception("Invalid server response: userId is null")

        // ✅ clear old session then save new
        tokenStore.clearAll()

        tokenStore.saveSession(
            token = body.token,
            role = role,
            userId = userId
        )

        return body
    }

    fun logout() {
        tokenStore.clearAll()
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()
    fun getRole(): String? = tokenStore.getRole()
    fun getUserId(): String? = tokenStore.getUserId()
}