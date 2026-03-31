package com.project.auto_aid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.auto_aid.data.local.TokenStore

class AiAssistViewModelFactory(
    private val tokenStore: TokenStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiAssistViewModel::class.java)) {
            return AiAssistViewModel(tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}