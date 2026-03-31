package com.project.auto_aid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.auto_aid.data.network.ApiService

class HomeViewModelFactory(
    private val api: ApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}