package com.project.auto_aid.data.network.dto

data class GetMyProviderVerificationResponse(
    val provider: ProviderVerificationDtoWrapper
)

data class ProviderVerificationDtoWrapper(
    val fullName: String? = null,
    val phone: String? = null,
    val providerVerification: ProviderVerificationDto? = null
)

data class ProviderVerificationDto(
    val status: String? = null,
    val rejectionReason: String? = null,

    // EXISTING
    val licenseDocumentUrl: String? = null,
    val businessDocumentUrl: String? = null,
    val profileImageUrl: String? = null,

    // 🔥 NEW (VERY IMPORTANT)
    val nationalIdFrontUrl: String? = null,
    val nationalIdBackUrl: String? = null
)