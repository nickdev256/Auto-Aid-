package com.project.auto_aid.data.network

import com.project.auto_aid.data.network.dto.AuthResponse
import com.project.auto_aid.data.network.dto.AvailabilityResponse
import com.project.auto_aid.data.network.dto.CreatePaymentBody
import com.project.auto_aid.data.network.dto.CreatePayoutRequestBody
import com.project.auto_aid.data.network.dto.CreateRequestBody
import com.project.auto_aid.data.network.dto.ForgotPasswordRequest
import com.project.auto_aid.data.network.dto.LocationResponse
import com.project.auto_aid.data.network.dto.LoginRequest
import com.project.auto_aid.data.network.dto.MeResponse
import com.project.auto_aid.data.network.dto.MessageResponse
import com.project.auto_aid.data.network.dto.PaymentResponse
import com.project.auto_aid.data.network.dto.PayoutInfoDto
import com.project.auto_aid.data.network.dto.PayoutInfoResponse
import com.project.auto_aid.data.network.dto.PayoutRequestDto
import com.project.auto_aid.data.network.dto.ProviderBucketsResponse
import com.project.auto_aid.data.network.dto.ProviderDto
import com.project.auto_aid.data.network.dto.ProviderLiteDto
import com.project.auto_aid.data.network.dto.ProviderWalletDto
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.data.network.dto.ResendOtpRequest
import com.project.auto_aid.data.network.dto.SetRequestPriceBody
import com.project.auto_aid.data.network.dto.SetRequestPriceResponse
import com.project.auto_aid.data.network.dto.SignupInitResponse
import com.project.auto_aid.data.network.dto.SignupRequest
import com.project.auto_aid.data.network.dto.UpdateAvailabilityBody
import com.project.auto_aid.data.network.dto.UpdatePayoutInfoBody
import com.project.auto_aid.data.network.dto.UpdateProfileRequest
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import com.project.auto_aid.data.network.dto.UploadResponse
import com.project.auto_aid.data.network.dto.VerifyOtpRequest
import com.project.auto_aid.data.network.dto.VoiceUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): Response<SignupInitResponse>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): Response<AuthResponse>

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(@Body body: ResendOtpRequest): Response<MessageResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<MessageResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<MeResponse>

    @GET("api/providers/me")
    suspend fun getProviderMe(): Response<ProviderDto>

    @PATCH("api/providers/me")
    suspend fun updateProviderProfile(
        @Body body: UpdateProfileRequest
    ): Response<ProviderDto>

    @PATCH("api/providers/availability")
    suspend fun updateProviderAvailability(
        @Body body: UpdateAvailabilityBody
    ): Response<AvailabilityResponse>

    @POST("api/requests")
    suspend fun createRequest(@Body body: CreateRequestBody): Response<RequestDto>

    @GET("api/requests/my")
    suspend fun getMyRequests(): Response<List<RequestDto>>

    @GET("api/requests/provider")
    suspend fun getProviderBuckets(
        @Query("providerType") providerType: String
    ): Response<ProviderBucketsResponse>

    @POST("api/requests/{id}/assign")
    suspend fun assignRequest(
        @Path("id") requestId: String
    ): Response<RequestDto>

    @POST("api/requests/{id}/decline")
    suspend fun declineRequest(
        @Path("id") requestId: String
    ): Response<MessageResponse>

    @PATCH("api/requests/{id}/status")
    suspend fun updateRequestStatus(
        @Path("id") requestId: String,
        @Body body: UpdateStatusBody
    ): Response<RequestDto>

    @PATCH("api/requests/{id}/set-price")
    suspend fun setRequestPrice(
        @Path("id") requestId: String,
        @Body body: SetRequestPriceBody
    ): Response<SetRequestPriceResponse>

    @GET("api/requests/{id}/quote")
    suspend fun getRequestQuote(
        @Path("id") requestId: String
    ): Response<RequestQuoteDto>

    @GET("api/requests/{id}")
    suspend fun getRequestById(
        @Path("id") id: String
    ): Response<RequestDto>

    @GET("api/requests/{id}/location")
    suspend fun getUserLocation(
        @Path("id") requestId: String
    ): Response<LocationResponse>

    @POST("api/requests/{id}/provider-complete")
    suspend fun markProviderComplete(
        @Path("id") requestId: String
    ): Response<MessageResponse>

    @POST("api/requests/{id}/user-confirm-complete")
    suspend fun confirmUserComplete(
        @Path("id") requestId: String
    ): Response<MessageResponse>

    @GET("api/providers/available")
    suspend fun getAvailableProviders(
        @Query("providerType") providerType: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("onlineOnly") onlineOnly: Boolean = true
    ): Response<List<ProviderLiteDto>>

    @POST("api/payments")
    suspend fun makePayment(
        @Body body: CreatePaymentBody
    ): Response<PaymentResponse>

    @GET("api/providers/payout-info")
    suspend fun getPayoutInfo(): Response<PayoutInfoDto>

    @PATCH("api/providers/payout-info")
    suspend fun updatePayoutInfo(
        @Body body: UpdatePayoutInfoBody
    ): Response<PayoutInfoResponse>

    @GET("api/providers/wallet")
    suspend fun getProviderWallet(): Response<ProviderWalletDto>

    @POST("api/providers/payout-requests")
    suspend fun createPayoutRequest(
        @Body body: CreatePayoutRequestBody
    ): Response<PayoutRequestDto>

    @GET("api/providers/payout-requests")
    suspend fun getProviderPayoutRequests(): Response<List<PayoutRequestDto>>

    @GET("api/providers/admin/payout-requests")
    suspend fun getAdminPayoutRequests(): Response<List<PayoutRequestDto>>

    @PATCH("api/providers/admin/payout-requests/{id}/approve")
    suspend fun approvePayoutRequest(
        @Path("id") payoutId: String
    ): Response<PayoutRequestDto>

    @PATCH("api/providers/admin/payout-requests/{id}/reject")
    suspend fun rejectPayoutRequest(
        @Path("id") payoutId: String
    ): Response<PayoutRequestDto>

    @PATCH("api/providers/admin/payout-requests/{id}/paid")
    suspend fun markPayoutRequestPaid(
        @Path("id") payoutId: String
    ): Response<PayoutRequestDto>

    @GET("api/ping")
    suspend fun ping(): Response<Map<String, Any>>

    @Multipart
    @POST("api/uploads/profile-image")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @Multipart
    @POST("api/upload/voice")
    suspend fun uploadVoice(
        @Part audio: MultipartBody.Part
    ): Response<VoiceUploadResponse>
}