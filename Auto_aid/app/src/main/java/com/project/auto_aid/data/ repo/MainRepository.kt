package com.project.auto_aid.data.repository

import com.project.auto_aid.data.network.ApiService
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.data.network.dto.SetRequestPriceBody
import com.project.auto_aid.data.network.dto.StatusUpdateResponse
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import retrofit2.Response

class RequestRepository(
    private val api: ApiService
) {

    private fun <T> requireBody(response: Response<T>, fallbackMessage: String): T {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) return body
        }

        val errorText = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        throw Exception(errorText ?: fallbackMessage)
    }

    suspend fun assignRequest(requestId: String): RequestDto {
        val response = api.assignRequest(requestId)
        return requireBody(response, "Failed to assign request")
    }

    suspend fun updateRequestStatus(
        requestId: String,
        status: String
    ): StatusUpdateResponse {
        val response = api.updateRequestStatus(
            requestId,
            UpdateStatusBody(status)
        )
        return requireBody(response, "Failed to update request status")
    }

    suspend fun setRequestPrice(
        requestId: String,
        providerAmount: Double
    ): StatusUpdateResponse {
        val response = api.setRequestPrice(
            requestId,
            SetRequestPriceBody(providerAmount)
        )

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                return StatusUpdateResponse(
                    ok = true,
                    message = body.message,
                    request = null
                )
            }
        }

        val errorText = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        throw Exception(errorText ?: "Failed to set request price")
    }

    suspend fun getRequestQuote(requestId: String): RequestQuoteDto {
        val response = api.getRequestQuote(requestId)
        return requireBody(response, "Failed to get request quote")
    }

    suspend fun providerCompleteRequest(requestId: String): StatusUpdateResponse {
        val response = api.markProviderComplete(requestId)
        return requireBody(response, "Failed to mark provider complete")
    }

    suspend fun userConfirmCompleteRequest(requestId: String): StatusUpdateResponse {
        val response = api.confirmUserComplete(requestId)
        return requireBody(response, "Failed to confirm completion")
    }

    suspend fun getRequestById(requestId: String): RequestDto {
        val response = api.getRequestById(requestId)
        return requireBody(response, "Failed to get request")
    }
}