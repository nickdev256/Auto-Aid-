package com.project.auto_aid.provider.verification

import android.net.Uri

data class ProviderVerificationUiState(
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String = "",
    val success: String = "",

    val businessName: String = "",
    val phone: String = "",
    val businessType: String = "",

    val verificationStatus: String = "not_verified",
    val rejectionReason: String = "",

    val licenseDocumentUrl: String = "",
    val businessDocumentUrl: String = "",
    val nationalIdFrontUrl: String = "",
    val nationalIdBackUrl: String = "",
    val profileImageUrl: String = "",

    val selectedLicenseUri: Uri? = null,
    val selectedBusinessUri: Uri? = null,
    val selectedNationalIdFrontUri: Uri? = null,
    val selectedNationalIdBackUri: Uri? = null,
    val selectedProfileImageUri: Uri? = null,

    val canReceiveJobs: Boolean = false,
    val verifiedBadgeText: String = "",

    val hasLicensePhoto: Boolean = false,
    val hasBusinessPhoto: Boolean = false,
    val hasNationalIdFrontPhoto: Boolean = false,
    val hasNationalIdBackPhoto: Boolean = false,
    val hasProfilePhoto: Boolean = false
)