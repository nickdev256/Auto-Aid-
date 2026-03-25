package com.project.auto_aid.provider.verification

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.MessageResponse
import com.project.auto_aid.data.network.dto.UpdateProfileRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ProviderVerificationRepository(
    private val context: Context
) {
    private val tokenStore = TokenStore(context)
    private val api = RetrofitClient.create(tokenStore)

    suspend fun loadProviderProfile(): ProviderVerificationProfile {
        return withContext(Dispatchers.IO) {
            val providerResponse = api.getProviderMe()
            val verificationResponse = api.getMyProviderVerification()

            if (!providerResponse.isSuccessful) {
                throw Exception(providerResponse.errorBody()?.string()
                    ?: "Failed to load provider profile")
            }

            if (!verificationResponse.isSuccessful) {
                throw Exception(verificationResponse.errorBody()?.string()
                    ?: "Failed to load verification")
            }

            val verification = verificationResponse.body()?.provider?.providerVerification

            ProviderVerificationProfile(
                businessName = providerResponse.body()?.businessName ?: "",
                phone = providerResponse.body()?.phone ?: "",
                verificationStatus = verification?.status ?: "not_verified",
                rejectionReason = verification?.rejectionReason ?: "",

                licenseDocumentUrl = verification?.licenseDocumentUrl ?: "",
                businessDocumentUrl = verification?.businessDocumentUrl ?: "",
                nationalIdFrontUrl = verification?.nationalIdFrontUrl ?: "",
                nationalIdBackUrl = verification?.nationalIdBackUrl ?: "",
                profileImageUrl = verification?.profileImageUrl ?: ""
            )
        }
    }

    suspend fun submitVerification(
        businessName: String,
        phone: String,
        licenseUri: Uri?,
        businessUri: Uri?,
        nationalIdFrontUri: Uri?,
        nationalIdBackUri: Uri?,
        profileImageUri: Uri?
    ): MessageResponse {
        return withContext(Dispatchers.IO) {

            val licensePart = licenseUri?.let {
                uriToMultipart("workLicenseDocument", it)
            }

            val businessPart = businessUri?.let {
                uriToMultipart("businessRegistrationDocument", it)
            }

            val nationalFrontPart = nationalIdFrontUri?.let {
                uriToMultipart("nationalIdFront", it)
            }

            val nationalBackPart = nationalIdBackUri?.let {
                uriToMultipart("nationalIdBack", it)
            }

            val profilePart = profileImageUri?.let {
                uriToMultipart("profileImage", it)
            }

            val response = api.submitProviderVerification(
                workLicenseDocument = licensePart,
                businessRegistrationDocument = businessPart,
                nationalIdFront = nationalFrontPart,
                nationalIdBack = nationalBackPart,
                profileImage = profilePart,
                businessName = businessName.toPlainPart(),
                phone = phone.toPlainPart()
            )

            if (!response.isSuccessful) {
                throw Exception(response.errorBody()?.string()
                    ?: "Verification failed")
            }

            response.body() ?: MessageResponse(
                message = "Verification submitted successfully"
            )
        }
    }

    private fun uriToMultipart(partName: String, uri: Uri): MultipartBody.Part {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/jpeg"

        val inputStream = resolver.openInputStream(uri)
            ?: throw Exception("Cannot read file")

        val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }

        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            partName,
            file.name,
            requestBody
        )
    }

    private fun String.toPlainPart(): RequestBody {
        return this.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}

data class ProviderVerificationProfile(
    val businessName: String = "",
    val phone: String = "",
    val verificationStatus: String = "not_verified",
    val rejectionReason: String = "",

    val licenseDocumentUrl: String = "",
    val businessDocumentUrl: String = "",
    val nationalIdFrontUrl: String = "",
    val nationalIdBackUrl: String = "",
    val profileImageUrl: String = ""
)