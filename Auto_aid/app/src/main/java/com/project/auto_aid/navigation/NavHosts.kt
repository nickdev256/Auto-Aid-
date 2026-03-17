package com.project.auto_aid.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/* ---------- Splash & Onboard ---------- */
import com.project.auto_aid.model.OnBoardScreen
import com.project.auto_aid.model.SplashScreen
import com.project.auto_aid.settings.ConsentScreen

/* ---------- Auth ---------- */
import com.project.auto_aid.screens.ForgotPasswordScreen
import com.project.auto_aid.screens.LoginScreen
import com.project.auto_aid.screens.ProviderSelectionScreen
import com.project.auto_aid.screens.ResetPasswordScreen
import com.project.auto_aid.screens.SignupScreen
import com.project.auto_aid.screens.VerifyCodeScreen
import com.project.auto_aid.settings.TermsAndConditionsScreen

/* ---------- Main ---------- */
import com.project.auto_aid.screens.HomeScreen
import com.project.auto_aid.screens.MaintenanceScreen
import com.project.auto_aid.screens.NotificationScreen
import com.project.auto_aid.screens.RequestDetailsScreen

/* ---------- Settings ---------- */
import com.project.auto_aid.settings.AboutUsScreen
import com.project.auto_aid.settings.IdentityVerificationScreen
import com.project.auto_aid.settings.PayoutInformationScreen
import com.project.auto_aid.settings.PrivacyPolicyScreen
import com.project.auto_aid.settings.PromotionScreen
import com.project.auto_aid.settings.SettingsScreen

/* ---------- User Features ---------- */
import com.project.auto_aid.screens.ambulance.AmbulanceActiveScreen
import com.project.auto_aid.screens.ambulance.AmbulanceHistoryScreen
import com.project.auto_aid.screens.ambulance.AmbulanceRequestScreen
import com.project.auto_aid.screens.ambulance.AmbulanceScreen
import com.project.auto_aid.screens.fuel.FuelActiveScreen
import com.project.auto_aid.screens.fuel.FuelHistoryScreen
import com.project.auto_aid.screens.fuel.FuelRequestScreen
import com.project.auto_aid.screens.fuel.FuelScreen
import com.project.auto_aid.screens.garage.AvailableGarageProvidersScreen
import com.project.auto_aid.screens.garage.GarageActiveScreen
import com.project.auto_aid.screens.garage.GarageHistoryScreen
import com.project.auto_aid.screens.garage.GarageRequestScreen
import com.project.auto_aid.screens.garage.GarageScreen
import com.project.auto_aid.screens.location.LocationPickerScreen
import com.project.auto_aid.screens.towing.AvailableProvidersScreen
import com.project.auto_aid.screens.towing.TowingActiveScreen
import com.project.auto_aid.screens.towing.TowingHistoryScreen
import com.project.auto_aid.screens.towing.TowingRequestScreen
import com.project.auto_aid.screens.towing.TowingScreen

/* ---------- Provider ---------- */
import com.project.auto_aid.provider.ui.EditProviderProfileScreen
import com.project.auto_aid.provider.ui.ProviderActiveJobScreen
import com.project.auto_aid.provider.ui.ProviderChatListScreen
import com.project.auto_aid.provider.ui.ProviderChatThreadScreen
import com.project.auto_aid.provider.ui.ProviderDashboardScreen
import com.project.auto_aid.provider.ui.ProviderMapHomeScreen
import com.project.auto_aid.provider.ui.ProviderMapScreen
import com.project.auto_aid.provider.ui.ProviderNotificationsScreen
import com.project.auto_aid.provider.ui.ProviderPayoutInformationScreen
import com.project.auto_aid.provider.ui.ProviderPayoutRequestsScreen
import com.project.auto_aid.provider.ui.ProviderProfileScreen
import com.project.auto_aid.provider.ui.ProviderRequestDetailsScreen
import com.project.auto_aid.provider.ui.ProviderWalletScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SplashScreen.route
    ) {

        /* ---------- Splash & Onboard ---------- */
        composable(Routes.SplashScreen.route) {
            SplashScreen(navController)
        }

        composable(Routes.OnBoardScreen.route) {
            OnBoardScreen(navController)
        }

        composable(Routes.ConsentScreen.route) {
            ConsentScreen(navController)
        }

        /* ---------- Auth ---------- */
        composable(Routes.LoginScreen.route) {
            LoginScreen(navController)
        }

        composable(Routes.SignupScreen.route) {
            SignupScreen(navController)
        }

        composable(Routes.ForgotPasswordScreen.route) {
            ForgotPasswordScreen(navController)
        }

        composable(
            route = Routes.VerifyCodeScreen.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { entry ->
            val email = entry.arguments?.getString("email") ?: ""
            VerifyCodeScreen(
                navController = navController,
                email = email
            )
        }

        composable(Routes.ResetPasswordScreen.route) {
            ResetPasswordScreen(navController)
        }

        /* ---------- Maintenance ---------- */
        composable(
            route = Routes.MaintenanceScreen.route,
            arguments = listOf(
                navArgument("message") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "AutoAid is currently under maintenance. Please try again later."
                }
            )
        ) { entry ->
            val message = entry.arguments?.getString("message")
                ?: "AutoAid is currently under maintenance. Please try again later."

            MaintenanceScreen(
                navController = navController,
                message = message
            )
        }

        /* ---------- Main ---------- */
        composable(Routes.HomeScreen.route) {
            HomeScreen(navController)
        }

        composable(Routes.NotificationScreen.route) {
            NotificationScreen(navController)
        }

        composable(
            route = Routes.RequestDetails.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            RequestDetailsScreen(
                navController = navController,
                requestId = requestId
            )
        }

        /* ---------- Shared Provider Selection ---------- */
        composable(
            route = Routes.ProviderSelection.route,
            arguments = listOf(
                navArgument("providerType") { type = NavType.StringType },
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType },
                navArgument("pickedLabel") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("vehicleInfo") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("problem") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("note") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("urgency") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "normal"
                },
                navArgument("towType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { entry ->
            val providerType = entry.arguments?.getString("providerType") ?: ""
            val lat = entry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = entry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            val pickedLabel = entry.arguments?.getString("pickedLabel").orEmpty()

            ProviderSelectionScreen(
                navController = navController,
                providerType = providerType,
                userLat = lat,
                userLng = lng,
                pickedLocationLabel = pickedLabel
            )
        }

        /* ---------- Location Picker ---------- */
        composable(
            route = Routes.LocationPicker.route,
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "0.0"
                },
                navArgument("lng") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "0.0"
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "android-app://androidx.navigation/location_picker?lat={lat}&lng={lng}"
                }
            )
        ) { entry ->
            val lat = entry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = entry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0

            LocationPickerScreen(
                navController = navController,
                initialLat = lat,
                initialLng = lng
            )
        }

        /* ---------- Settings ---------- */
        composable(Routes.SettingsScreen.route) {
            SettingsScreen(navController)
        }

        composable(Routes.UserInfoScreen.route) {
            IdentityVerificationScreen(navController)
        }

        /* ---------- Garage ---------- */
        composable(Routes.GarageScreen.route) {
            GarageScreen(navController)
        }

        composable(Routes.GarageProvidersScreen.route) {
            AvailableGarageProvidersScreen(navController)
        }

        composable(
            route = Routes.GarageRequestScreen.route,
            arguments = listOf(
                navArgument("providerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId")
            GarageRequestScreen(
                navController = navController,
                providerId = providerId
            )
        }

        composable(
            route = Routes.GarageActiveScreen.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            GarageActiveScreen(
                navController = navController,
                requestId = requestId
            )
        }

        composable(Routes.GarageHistoryScreen.route) {
            GarageHistoryScreen(navController)
        }

        /* ---------- Towing ---------- */
        composable(Routes.TowingScreen.route) {
            TowingScreen(navController)
        }

        composable(Routes.TowingProvidersScreen.route) {
            AvailableProvidersScreen(navController)
        }

        composable(
            route = Routes.TowingRequestScreen.route,
            arguments = listOf(
                navArgument("providerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId")
            TowingRequestScreen(
                navController = navController,
                providerId = providerId
            )
        }

        composable(
            route = Routes.TowingActiveScreen.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            TowingActiveScreen(
                navController = navController,
                requestId = requestId
            )
        }

        composable(Routes.TowingHistoryScreen.route) {
            TowingHistoryScreen(navController)
        }

        /* ---------- Fuel ---------- */
        composable(Routes.FuelScreen.route) {
            FuelScreen(navController)
        }

        composable(Routes.FuelProvidersScreen.route) {
            FuelScreen(navController)
        }

        composable(
            route = Routes.FuelRequestScreen.route,
            arguments = listOf(
                navArgument("providerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId")
            FuelRequestScreen(
                navController = navController,
                providerId = providerId
            )
        }

        composable(
            route = Routes.FuelActiveScreen.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            FuelActiveScreen(
                navController = navController,
                requestId = requestId
            )
        }

        composable(Routes.FuelHistoryScreen.route) {
            FuelHistoryScreen(navController)
        }

        /* ---------- Ambulance ---------- */
        composable(Routes.AmbulanceScreen.route) {
            AmbulanceScreen(navController)
        }

        composable(Routes.AmbulanceProvidersScreen.route) {
            AmbulanceScreen(navController)
        }

        composable(
            route = Routes.AmbulanceRequestScreen.route,
            arguments = listOf(
                navArgument("providerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId")
            AmbulanceRequestScreen(
                navController = navController,
                providerId = providerId
            )
        }

        composable(
            route = Routes.AmbulanceActiveScreen.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            AmbulanceActiveScreen(
                navController = navController,
                requestId = requestId
            )
        }

        composable(Routes.AmbulanceHistoryScreen.route) {
            AmbulanceHistoryScreen(navController)
        }

        /* ---------- Legal ---------- */
        composable(Routes.AboutUsScreen.route) {
            AboutUsScreen(navController)
        }

        composable(Routes.PrivacyPolicyScreen.route) {
            PrivacyPolicyScreen(navController)
        }

        composable(Routes.PromotionScreen.route) {
            PromotionScreen(navController)
        }

        composable(Routes.PayoutInformationScreen.route) {
            PayoutInformationScreen(navController)
        }

        composable(
            route = Routes.TermsAndConditionsScreen.route,
            arguments = listOf(
                navArgument("fromSignup") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val fromSignup = entry.arguments?.getBoolean("fromSignup") ?: false
            TermsAndConditionsScreen(
                navController = navController,
                fromSignup = fromSignup
            )
        }

        /* ---------- Provider ---------- */
        composable(Routes.ProviderDashboard.route) {
            ProviderDashboardScreen(navController)
        }

        composable(Routes.EditProviderProfile.route) {
            EditProviderProfileScreen(navController)
        }

        composable(Routes.ProviderNotifications.route) {
            ProviderNotificationsScreen(navController)
        }

        composable(Routes.ProviderMapHome.route) {
            ProviderMapHomeScreen(navController)
        }

        composable(Routes.ProviderChatList.route) {
            ProviderChatListScreen(navController)
        }

        composable(Routes.ProviderProfile.route) {
            ProviderProfileScreen(navController)
        }

        composable(Routes.ProviderWallet.route) {
            ProviderWalletScreen(navController)
        }

        composable(Routes.ProviderPayoutInfo.route) {
            ProviderPayoutInformationScreen(navController)
        }

        composable(Routes.ProviderPayoutRequests.route) {
            ProviderPayoutRequestsScreen(navController)
        }

        composable(
            route = Routes.ProviderChatThread.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            ProviderChatThreadScreen(
                navController = navController,
                requestId = requestId
            )
        }

        composable(
            route = Routes.ProviderActiveJob.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            ProviderActiveJobScreen(
                requestId = requestId,
                navController = navController
            )
        }

        composable(
            route = Routes.ProviderMapScreen.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType },
                navArgument("pickupLat") {
                    type = NavType.StringType
                    defaultValue = "0.0"
                },
                navArgument("pickupLng") {
                    type = NavType.StringType
                    defaultValue = "0.0"
                }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            val pickupLat = entry.arguments?.getString("pickupLat")?.toDoubleOrNull() ?: 0.0
            val pickupLng = entry.arguments?.getString("pickupLng")?.toDoubleOrNull() ?: 0.0

            ProviderMapScreen(
                requestId = requestId,
                pickupLat = pickupLat,
                pickupLng = pickupLng
            )
        }

        composable(
            route = Routes.ProviderRequestDetails.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId") ?: ""
            ProviderRequestDetailsScreen(
                navController = navController,
                requestId = requestId
            )
        }
    }
}