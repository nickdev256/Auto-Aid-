package com.project.auto_aid.viewmodel

import com.project.auto_aid.screens.LiveFeaturedServiceItem
import com.project.auto_aid.screens.RecentItem

data class HomeUiState(
    val userName: String = "User",
    val notificationCount: Int = 0,
    val isProfileLoading: Boolean = false,
    val isProvidersLoading: Boolean = false,
    val featuredTitle: String = "Featured Services",
    val recentItems: List<RecentItem> = emptyList(),
    val featuredServices: List<LiveFeaturedServiceItem> = emptyList(),
    val error: String? = null,
    val referralCode: String = "",
    val nextReferralDiscountAmount: Double = 0.0,
    val rewardedReferralCount: Int = 0,
    val totalReferralCount: Int = 0,
    val isReferralLoading: Boolean = false,
)