package com.project.auto_aid.data.network

import com.google.gson.Gson
import com.project.auto_aid.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Emulator -> 10.0.2.2
    // Real phone with adb reverse -> 127.0.0.1
    private const val BASE_URL = "http://127.0.0.1:5001/"

    private val gson = Gson()

    fun create(tokenStore: TokenStore): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authAndClientInterceptor = Interceptor { chain ->
            val originalRequest: Request = chain.request()
            val path = originalRequest.url.encodedPath

            val token = try {
                tokenStore.getToken()
            } catch (e: Exception) {
                null
            }

            val requestBuilder = originalRequest.newBuilder()
                .header("X-Client", "android")
                .header("Accept", "application/json")

            val isAuthEndpoint =
                path.contains("/api/auth/login") ||
                        path.contains("/api/auth/signup") ||
                        path.contains("/api/auth/verify-otp") ||
                        path.contains("/api/auth/resend-otp") ||
                        path.contains("/api/auth/forgot-password")

            if (!isAuthEndpoint && !token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            } else {
                requestBuilder.removeHeader("Authorization")
            }

            chain.proceed(requestBuilder.build())
        }

        val maintenanceInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (response.code == 503) {
                response.newBuilder()
                    .message("AutoAid is currently under maintenance. Please try again later.")
                    .build()
            } else {
                response
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authAndClientInterceptor)
            .addInterceptor(maintenanceInterceptor)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}