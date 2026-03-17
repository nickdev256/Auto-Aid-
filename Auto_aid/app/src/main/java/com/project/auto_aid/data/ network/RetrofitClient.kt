package com.project.auto_aid.data.network

import com.google.gson.Gson
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.dto.MaintenanceResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Real phone + adb reverse tcp:5001 tcp:5001
    private const val BASE_URL = "http://127.0.0.1:5001/"

    private val gson = Gson()

    fun create(tokenStore: TokenStore): ApiService {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authAndClientInterceptor = Interceptor { chain ->
            val original: Request = chain.request()
            val path = original.url.encodedPath
            val token = tokenStore.getToken()

            val builder = original.newBuilder()
                .header("X-Client", "android")
                .header("Accept", "application/json")

            val isAuthEndpoint =
                path.contains("/api/auth/login") ||
                        path.contains("/api/auth/signup") ||
                        path.contains("/api/auth/verify-otp") ||
                        path.contains("/api/auth/resend-otp") ||
                        path.contains("/api/auth/forgot-password")

            if (!isAuthEndpoint && !token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            } else {
                builder.removeHeader("Authorization")
            }

            chain.proceed(builder.build())
        }

        val maintenanceInterceptor = Interceptor { chain ->
            val response: Response = chain.proceed(chain.request())

            val originalBody = response.body
            val contentType = originalBody?.contentType()
            val rawBody = originalBody?.string().orEmpty()

            val maintenance = try {
                if (rawBody.isNotBlank()) {
                    gson.fromJson(rawBody, MaintenanceResponse::class.java)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }

            val isMaintenance =
                response.code == 503 || maintenance?.maintenanceMode == true

            if (isMaintenance) {
                val message = maintenance?.message
                    ?: "AutoAid is currently under maintenance. Please try again later."

                return@Interceptor response.newBuilder()
                    .code(503)
                    .message(message)
                    .body(rawBody.toResponseBody(contentType))
                    .build()
            }

            response.newBuilder()
                .body(rawBody.toResponseBody(contentType))
                .build()
        }

        val okHttp = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authAndClientInterceptor)
            .addInterceptor(maintenanceInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}