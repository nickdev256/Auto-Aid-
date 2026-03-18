package com.project.auto_aid.navigation

import android.net.Uri

sealed class Routes(val route: String) {

    /* ---------- Splash & Onboard ---------- */

    object SplashScreen : Routes("splash")
    object OnBoardScreen : Routes("onboard")
    object ConsentScreen : Routes("consent")

    /* ---------- Auth ---------- */

    object LoginScreen : Routes("login")
    object SignupScreen : Routes("signup")
    object ForgotPasswordScreen : Routes("forgot_password")

    object VerifyCodeScreen : Routes("verify_code/{email}") {
        fun createRoute(email: String) = "verify_code/${Uri.encode(email)}"
    }

    object ResetPasswordScreen : Routes("reset_password")

    /* ---------- Maintenance ---------- */

    object MaintenanceScreen : Routes("maintenance?message={message}") {
        fun createRoute(message: String? = null): String {
            return if (message.isNullOrBlank()) {
                "maintenance"
            } else {
                "maintenance?message=${Uri.encode(message)}"
            }
        }
    }

    /* ---------- Main ---------- */

    object HomeScreen : Routes("home")
    object NotificationScreen : Routes("notifications")

    object RequestDetails : Routes("request_details/{requestId}") {
        fun createRoute(requestId: String) = "request_details/${Uri.encode(requestId)}"
    }

    /* ---------- Provider Selection ---------- */

    object ProviderSelection : Routes(
        "provider_selection/{providerType}/{lat}/{lng}?" +
                "pickedLabel={pickedLabel}&vehicleInfo={vehicleInfo}&problem={problem}&note={note}&urgency={urgency}&towType={towType}"
    ) {
        fun createRoute(
            providerType: String,
            lat: Double,
            lng: Double,
            pickedLabel: String = "",
            vehicleInfo: String = "",
            problem: String = "",
            note: String = "",
            urgency: String = "normal",
            towType: String = ""
        ): String {
            return "provider_selection/${Uri.encode(providerType)}/$lat/$lng" +
                    "?pickedLabel=${Uri.encode(pickedLabel)}" +
                    "&vehicleInfo=${Uri.encode(vehicleInfo)}" +
                    "&problem=${Uri.encode(problem)}" +
                    "&note=${Uri.encode(note)}" +
                    "&urgency=${Uri.encode(urgency)}" +
                    "&towType=${Uri.encode(towType)}"
        }
    }

    /* ---------- Create Request Form ---------- */

    object CreateRequestForm : Routes(
        "create_request_form?providerType={providerType}&providerId={providerId}&lat={lat}&lng={lng}&locationLabel={locationLabel}"
    ) {
        fun createRoute(
            providerType: String,
            providerId: String,
            lat: Double,
            lng: Double,
            locationLabel: String = ""
        ): String {
            return "create_request_form" +
                    "?providerType=${Uri.encode(providerType)}" +
                    "&providerId=${Uri.encode(providerId)}" +
                    "&lat=$lat" +
                    "&lng=$lng" +
                    "&locationLabel=${Uri.encode(locationLabel)}"
        }
    }

    /* ---------- Location Picker ---------- */

    object LocationPicker : Routes("location_picker?lat={lat}&lng={lng}") {
        fun createRoute(lat: Double? = null, lng: Double? = null): String {
            return if (lat == null || lng == null) {
                "location_picker"
            } else {
                "location_picker?lat=$lat&lng=$lng"
            }
        }
    }

    /* ---------- Settings ---------- */

    object SettingsScreen : Routes("settings")
    object UserInfoScreen : Routes("user_info")
    object AboutUsScreen : Routes("about_us")
    object PrivacyPolicyScreen : Routes("privacy_policy")
    object PromotionScreen : Routes("promotion")
    object PayoutInformationScreen : Routes("payout_information")

    object TermsAndConditionsScreen : Routes("terms_conditions?fromSignup={fromSignup}") {
        fun createRoute(fromSignup: Boolean = false): String {
            return "terms_conditions?fromSignup=$fromSignup"
        }
    }

    /* ---------- Garage ---------- */

    object GarageScreen : Routes("garage")
    object GarageProvidersScreen : Routes("garage_providers")

    object GarageRequestScreen : Routes("garage_request?providerId={providerId}") {
        fun createRoute(providerId: String? = null): String {
            return if (providerId.isNullOrBlank()) {
                "garage_request"
            } else {
                "garage_request?providerId=${Uri.encode(providerId)}"
            }
        }
    }

    object GarageActiveScreen : Routes("garage_active/{requestId}") {
        fun createRoute(requestId: String) = "garage_active/${Uri.encode(requestId)}"
    }

    object GarageHistoryScreen : Routes("garage_history")

    /* ---------- Towing ---------- */

    object TowingScreen : Routes("towing")
    object TowingProvidersScreen : Routes("towing_providers")

    object TowingRequestScreen : Routes("towing_request?providerId={providerId}") {
        fun createRoute(providerId: String? = null): String {
            return if (providerId.isNullOrBlank()) {
                "towing_request"
            } else {
                "towing_request?providerId=${Uri.encode(providerId)}"
            }
        }
    }

    object TowingActiveScreen : Routes("towing_active/{requestId}") {
        fun createRoute(requestId: String) = "towing_active/${Uri.encode(requestId)}"
    }

    object TowingHistoryScreen : Routes("towing_history")

    /* ---------- Fuel ---------- */

    object FuelScreen : Routes("fuel")
    object FuelProvidersScreen : Routes("fuel_providers")

    object FuelRequestScreen : Routes("fuel_request?providerId={providerId}") {
        fun createRoute(providerId: String? = null): String {
            return if (providerId.isNullOrBlank()) {
                "fuel_request"
            } else {
                "fuel_request?providerId=${Uri.encode(providerId)}"
            }
        }
    }

    object FuelActiveScreen : Routes("fuel_active/{requestId}") {
        fun createRoute(requestId: String) = "fuel_active/${Uri.encode(requestId)}"
    }

    object FuelHistoryScreen : Routes("fuel_history")

    /* ---------- Ambulance ---------- */

    object AmbulanceScreen : Routes("ambulance")
    object AmbulanceProvidersScreen : Routes("ambulance_providers")

    object AmbulanceRequestScreen : Routes("ambulance_request?providerId={providerId}") {
        fun createRoute(providerId: String? = null): String {
            return if (providerId.isNullOrBlank()) {
                "ambulance_request"
            } else {
                "ambulance_request?providerId=${Uri.encode(providerId)}"
            }
        }
    }

    object AmbulanceActiveScreen : Routes("ambulance_active/{requestId}") {
        fun createRoute(requestId: String) = "ambulance_active/${Uri.encode(requestId)}"
    }

    object AmbulanceHistoryScreen : Routes("ambulance_history")

    /* ---------- Provider ---------- */

    object ProviderDashboard : Routes("provider_dashboard")
    object EditProviderProfile : Routes("edit_provider_profile")
    object ProviderNotifications : Routes("provider_notifications")
    object ProviderMapHome : Routes("provider_map_home")
    object ProviderChatList : Routes("provider_chat_list")
    object ProviderProfile : Routes("provider_profile")
    object ProviderWallet : Routes("provider_wallet")
    object ProviderPayoutInfo : Routes("provider_payout_info")
    object ProviderPayoutRequests : Routes("provider_payout_requests")

    object ProviderChatThread : Routes("provider_chat_thread/{requestId}") {
        fun createRoute(requestId: String) =
            "provider_chat_thread/${Uri.encode(requestId)}"
    }

    object ProviderActiveJob : Routes("provider_active_job/{requestId}") {
        fun createRoute(requestId: String) =
            "provider_active_job/${Uri.encode(requestId)}"
    }

    /* 🔥 FIXED MAP ROUTE */

    object ProviderMapScreen :
        Routes("provider_map?requestId={requestId}&pickupLat={pickupLat}&pickupLng={pickupLng}") {

        fun createRoute(
            requestId: String,
            pickupLat: Double,
            pickupLng: Double
        ): String {
            return "provider_map" +
                    "?requestId=${Uri.encode(requestId)}" +
                    "&pickupLat=${Uri.encode(pickupLat.toString())}" +
                    "&pickupLng=${Uri.encode(pickupLng.toString())}"
        }
    }

    object ProviderRequestDetails : Routes("provider_request_details/{requestId}") {
        fun createRoute(requestId: String) =
            "provider_request_details/${Uri.encode(requestId)}"
    }
}