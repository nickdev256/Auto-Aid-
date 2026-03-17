package com.project.auto_aid.provider.ui

import android.content.Context
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.UpdateProfileRequest

class ProviderProfileRepository(context: Context) {

    private val api = RetrofitClient.create(TokenStore(context))

    suspend fun loadProfile(): Pair<String, String> {

        val res = api.getProviderMe()

        // 🔐 Handle HTTP errors safely
        if (!res.isSuccessful) {
            return Pair("", "")
        }

        val user = res.body() ?: return Pair("", "")

        return Pair(
            user.name ?: "",
            user.phone ?: ""
        )
    }

    suspend fun updateProfile(name: String, phone: String) {

        api.updateProviderProfile(
            UpdateProfileRequest(
                name = name,
                phone = phone
            )
        )
    }
}