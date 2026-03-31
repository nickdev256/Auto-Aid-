package com.project.auto_aid.settings

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.model.UserProfile
import com.project.auto_aid.screens.IdentityVerificationScreen
import com.project.auto_aid.viewmodel.AuthViewModel
import com.project.auto_aid.viewmodel.IdentityVerificationViewModel

@Composable
fun IdentityVerificationRoute(
    navController: NavHostController
) {
    val application = LocalContext.current.applicationContext as Application

    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(application) as T
            }
        }
    )

    val verificationViewModel: IdentityVerificationViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()

    LaunchedEffect(authState.data) {
        authState.data?.let { authResponse ->
            val user = UserProfile(
                id = authResponse.user?.id
                    ?: authResponse.user?._id
                    ?: "",
                fullName = authResponse.user?.name ?: "",
                email = authResponse.user?.email ?: "",
                phone = authResponse.user?.phone ?: "",
                verificationStatus = authResponse.user?.verificationStatus ?: "not_verified"
            )

            verificationViewModel.loadUser(user)
        }
    }

    IdentityVerificationScreen(
        navController = navController,
        viewModel = verificationViewModel
    )
}