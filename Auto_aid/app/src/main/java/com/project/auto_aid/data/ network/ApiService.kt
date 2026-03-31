package com.project.auto_aid.data.network

import com.project.auto_aid.data.network.dto.AiAnalyzeRequest
import com.project.auto_aid.data.network.dto.AiAnalyzeResponse
import com.project.auto_aid.data.network.dto.AiEscalateRequest
import com.project.auto_aid.data.network.dto.AiEscalateResponse
import com.project.auto_aid.data.network.dto.AuthResponse
import com.project.auto_aid.data.network.dto.AvailabilityResponse
import com.project.auto_aid.data.network.dto.CreatePaymentBody
import com.project.auto_aid.data.network.dto.CreateRequestBody
import com.project.auto_aid.data.network.dto.ForgotPasswordRequest
import com.project.auto_aid.data.network.dto.GetMyProviderVerificationResponse
import com.project.auto_aid.data.network.dto.GetMyUserVerificationResponse
import com.project.auto_aid.data.network.dto.LocationResponse
import com.project.auto_aid.data.network.dto.LoginRequest
import com.project.auto_aid.data.network.dto.MeResponse
import com.project.auto_aid.data.network.dto.MessageResponse
import com.project.auto_aid.data.network.dto.NavigationRouteRequest
import com.project.auto_aid.data.network.dto.PaymentHistoryDto
import com.project.auto_aid.data.network.dto.PaymentResponse
import com.project.auto_aid.data.network.dto.ProviderBucketsResponse
import com.project.auto_aid.data.network.dto.ProviderDto
import com.project.auto_aid.data.network.dto.ProviderLiteDto
import com.project.auto_aid.data.network.dto.ReferralSummaryDto
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.data.network.dto.ResendOtpRequest
import com.project.auto_aid.data.network.dto.RouteResponseDto
import com.project.auto_aid.data.network.dto.SetRequestPriceBody
import com.project.auto_aid.data.network.dto.SetRequestPriceResponse
import com.project.auto_aid.data.network.dto.SignupInitResponse
import com.project.auto_aid.data.network.dto.SignupRequest
import com.project.auto_aid.data.network.dto.StatusUpdateResponse
import com.project.auto_aid.data.network.dto.UpdateAvailabilityBody
import com.project.auto_aid.data.network.dto.UpdateProfileRequest
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import com.project.auto_aid.data.network.dto.UploadResponse
import com.project.auto_aid.data.network.dto.VerifyOtpRequest
import com.project.auto_aid.data.network.dto.VoiceUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    /* ---------- Auth ---------- */

    @POST("api/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signup(
        @Body body: SignupRequest
    ): Response<SignupInitResponse>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(
        @Body body: VerifyOtpRequest
    ): Response<AuthResponse>

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(
        @Body body: ResendOtpRequest
    ): Response<MessageResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body body: ForgotPasswordRequest
    ): Response<MessageResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<MeResponse>

    /* ---------- Provider Profile ---------- */

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

    @GET("api/providers/available")
    suspend fun getAvailableProviders(
        @Query("providerType") providerType: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("onlineOnly") onlineOnly: Boolean = true,
        @Query("limit") limit: Int = 6
    ): Response<List<ProviderLiteDto>>

    /* ---------- Requests ---------- */

    @POST("api/requests")
    suspend fun createRequest(
        @Body body: CreateRequestBody
    ): Response<RequestDto>

    @GET("api/requests/my")
    suspend fun getMyRequests(
        @Query("limit") limit: Int = 10,
        @Query("page") page: Int = 1
    ): Response<List<RequestDto>>

    @GET("api/requests/provider")
    suspend fun getProviderBuckets(
        @Query("providerType") providerType: String,
        @Query("limit") limit: Int = 20
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
    ): Response<StatusUpdateResponse>

    @PATCH("api/requests/{id}/set-price")
    suspend fun setRequestPrice(
        @Path("id") requestId: String,
        @Body body: SetRequestPriceBody
    ): Response<SetRequestPriceResponse>

    @PATCH("api/requests/{id}/accept-quotation")
    suspend fun acceptQuotation(
        @Path("id") requestId: String
    ): Response<MessageResponse>

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

    @GET("api/requests/{id}/full")
    suspend fun getFullRequest(
        @Path("id") requestId: String
    ): Response<RequestDto>

    @POST("api/requests/{id}/provider-complete")
    suspend fun markProviderComplete(
        @Path("id") requestId: String
    ): Response<StatusUpdateResponse>

    @POST("api/requests/{id}/user-confirm-complete")
    suspend fun confirmUserComplete(
        @Path("id") requestId: String
    ): Response<StatusUpdateResponse>

    /* ---------- Payments ---------- */

    @POST("api/payments")
    suspend fun createPayment(
        @Body body: CreatePaymentBody
    ): Response<PaymentResponse>

    @GET("api/payments/history")
    suspend fun getPaymentHistory(
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): Response<List<PaymentHistoryDto>>

    /* ---------- Uploads ---------- */

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

    /* ---------- Provider Verification ---------- */

    @Multipart
    @POST("api/provider-verification/submit")
    suspend fun submitProviderVerification(
        @Part workLicenseDocument: MultipartBody.Part? = null,
        @Part businessRegistrationDocument: MultipartBody.Part? = null,
        @Part nationalIdFront: MultipartBody.Part? = null,
        @Part nationalIdBack: MultipartBody.Part? = null,
        @Part profileImage: MultipartBody.Part? = null,
        @Part("businessName") businessName: RequestBody? = null,
        @Part("phone") phone: RequestBody? = null,
        @Part("businessType") businessType: RequestBody? = null
    ): Response<MessageResponse>

    @GET("api/provider-verification/me")
    suspend fun getMyProviderVerification(): Response<GetMyProviderVerificationResponse>

    /* ---------- User Verification ---------- */

    @Multipart
    @POST("api/user-verification/submit")
    suspend fun submitUserVerification(
        @Part("documentType") documentType: RequestBody,
        @Part verificationDocument: MultipartBody.Part,
        @Part profileImage: MultipartBody.Part? = null
    ): Response<MessageResponse>

    @GET("api/user-verification/me")
    suspend fun getMyUserVerification(): Response<GetMyUserVerificationResponse>

    /* ---------- Utility ---------- */

    @GET("api/ping")
    suspend fun ping(): Response<Map<String, Any>>

    @POST("api/ai/analyze")
    suspend fun analyzeProblem(
        @Body body: AiAnalyzeRequest
    ): Response<AiAnalyzeResponse>

    @POST("api/ai/escalate")
    suspend fun escalateAfterSelfSolveFailure(
        @Body body: AiEscalateRequest
    ): Response<AiEscalateResponse>

    @GET("api/referrals/me")
    suspend fun getMyReferralSummary(): ReferralSummaryDto

    @POST("api/navigation/route")
    suspend fun getNavigationRoute(
        @Body body: NavigationRouteRequest
    ): Response<RouteResponseDto>
}