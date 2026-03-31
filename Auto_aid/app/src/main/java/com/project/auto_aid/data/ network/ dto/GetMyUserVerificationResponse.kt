package com.project.auto_aid.data.network.dto

data class GetMyUserVerificationResponse(
    val message: String? = null,
    val user: UserVerificationDto? = null,
    val verificationStatus: String? = null
)

data class UserVerificationDto(
    val id: String? = null,
    val _id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val status: String? = null,

    val verificationStatus: String? = null,
    val verificationDocumentType: String? = null,
    val verificationDocumentUrl: String? = null,
    val profileImageUrl: String? = null,

    val verificationSubmittedAt: String? = null,
    val verificationReviewedAt: String? = null,
    val verificationRejectionReason: String? = null
)