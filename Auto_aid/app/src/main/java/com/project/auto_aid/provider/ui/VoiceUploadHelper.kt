package com.project.auto_aid.provider.ui

import com.project.auto_aid.data.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

suspend fun uploadVoiceFile(file: File, api: ApiService): String {
    if (!file.exists()) {
        throw Exception("Recorded audio file does not exist")
    }

    if (file.length() <= 0L) {
        throw Exception("Recorded audio file is empty")
    }

    val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)

    val response = api.uploadVoice(body)

    if (!response.isSuccessful) {
        val backendError = runCatching { response.errorBody()?.string() }.getOrNull()
        throw Exception(backendError ?: "Voice upload failed (HTTP ${response.code()})")
    }

    val payload = response.body()
        ?: throw Exception("Voice upload failed: empty server response")

    val audioUrl = payload.audioUrl.trim()

    if (audioUrl.isBlank()) {
        throw Exception("Voice upload failed: server returned empty audio URL")
    }

    return audioUrl
}