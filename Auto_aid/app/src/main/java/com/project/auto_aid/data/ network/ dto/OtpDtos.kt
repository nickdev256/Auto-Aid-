package com.project.auto_aid.data.network.dto

// ✅ Signup step 1 response: { message, email }
data class SignupInitResponse(
    val message: String? = null,
    val email: String? = null
)

// ✅ Verify OTP request
data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

// ✅ Resend OTP request
data class ResendOtpRequest(
    val email: String
)


