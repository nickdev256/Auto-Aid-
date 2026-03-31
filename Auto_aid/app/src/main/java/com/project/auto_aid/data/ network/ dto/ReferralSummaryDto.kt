package com.project.auto_aid.data.network.dto

data class ReferralUserDto(
    val _id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null
)

data class ReferralItemDto(
    val _id: String? = null,
    val referredUser: ReferralUserDto? = null,
    val referralCode: String? = null,
    val status: String? = null,
    val friendDiscountAmount: Double? = 0.0,
    val referrerRewardAmount: Double? = 0.0,
    val qualifyingRequestId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val rewardedAt: String? = null
)

data class ReferralSummaryDto(
    val ok: Boolean? = null,
    val referralCode: String? = null,
    val nextReferralDiscountAmount: Double? = 0.0,
    val totalReferrals: Int? = 0,
    val rewardedCount: Int? = 0,
    val referrals: List<ReferralItemDto>? = emptyList()
)