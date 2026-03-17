package com.project.auto_aid.data.network.dto

data class ProviderBucketsResponse(
    val pending: List<RequestDto> = emptyList(),
    val ongoing: List<RequestDto> = emptyList(),
    val completed: List<RequestDto> = emptyList(),

    // your backend returns this as backward compatibility (same as pending)
    val requests: List<RequestDto> = emptyList()
)