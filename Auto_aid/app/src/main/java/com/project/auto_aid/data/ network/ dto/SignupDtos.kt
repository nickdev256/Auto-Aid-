package com.project.auto_aid.data.network.dto

data class SignupRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String, // "user" or "provider"

    // ✅ backend fields
    val businessName: String? = null,
    val businessType: String? = null,          // "towing" | "garage" | "fuel" | "ambulance"
    val servicesOffered: List<String> = emptyList(),
    val subscriptionPlan: String? = null       // "monthly" | "quarterly" | "yearly"
)