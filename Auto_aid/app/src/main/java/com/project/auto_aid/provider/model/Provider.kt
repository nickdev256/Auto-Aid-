package com.project.auto_aid.provider.model

data class Provider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val providerType: String = "",
    val rating: Double = 0.0,

    // Profile image support
    val profileImageUrl: String = "",

    // Online / Offline persistence
    val isOnline: Boolean = false,

    // Earnings tracking
    val earningsToday: Double = 0.0,
    val earningsWeek: Double = 0.0,

    // Admin verification system
    val isVerified: Boolean = false
)