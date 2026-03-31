package com.project.auto_aid.screens.tracking

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.SocketManager
import com.project.auto_aid.data.network.dto.NavigationRouteRequest
import com.project.auto_aid.utils.PolylineUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

@Composable
fun LiveTrackingMap(
    requestId: String,
    userLat: Double,
    userLng: Double
) {
    val userLocation = remember(userLat, userLng) { LatLng(userLat, userLng) }
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var providerLocation by remember { mutableStateOf<LatLng?>(null) }
    var animatedProviderLocation by remember { mutableStateOf<LatLng?>(null) }

    var connectionText by remember { mutableStateOf("Connecting live tracking...") }
    var distanceText by remember { mutableStateOf("Distance unavailable") }
    var etaText by remember { mutableStateOf("ETA unavailable") }
    var lastUpdateText by remember { mutableStateOf("Waiting for provider location...") }

    var userPlaceText by remember { mutableStateOf("Loading your location...") }
    var providerPlaceText by remember { mutableStateOf("Waiting for provider location...") }

    var lastResolvedProviderLat by remember { mutableDoubleStateOf(0.0) }
    var lastResolvedProviderLng by remember { mutableDoubleStateOf(0.0) }

    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val cameraPositionState = rememberCameraPositionState()
    val userMarkerState = remember(userLat, userLng) { MarkerState(position = userLocation) }
    val providerMarkerState = remember { MarkerState(position = userLocation) }

    suspend fun refreshRoute(origin: LatLng, destination: LatLng) {
        try {
            val response = api.getNavigationRoute(
                NavigationRouteRequest(
                    originLat = origin.latitude,
                    originLng = origin.longitude,
                    destLat = destination.latitude,
                    destLng = destination.longitude
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                val encoded = body?.encodedPolyline.orEmpty()

                routePoints = if (encoded.isNotBlank()) {
                    PolylineUtils.decode(encoded)
                } else {
                    listOf(origin, destination)
                }

                val distanceKm = (body?.distanceMeters ?: 0) / 1000.0
                distanceText = if (distanceKm > 0) {
                    String.format("%.2f km", distanceKm)
                } else {
                    String.format("%.2f km", straightDistanceKm(origin, destination))
                }

                etaText = if (!body?.duration.isNullOrBlank()) {
                    formatDuration(body?.duration.orEmpty())
                } else {
                    formatDuration(estimateDurationFromDistance(origin, destination))
                }
            } else {
                routePoints = listOf(origin, destination)
                distanceText = String.format("%.2f km", straightDistanceKm(origin, destination))
                etaText = formatDuration(estimateDurationFromDistance(origin, destination))
            }
        } catch (_: Exception) {
            routePoints = listOf(origin, destination)
            distanceText = String.format("%.2f km", straightDistanceKm(origin, destination))
            etaText = formatDuration(estimateDurationFromDistance(origin, destination))
        }
    }

    suspend fun animateProviderMarker(from: LatLng?, to: LatLng) {
        if (from == null) {
            animatedProviderLocation = to
            providerMarkerState.position = to
            return
        }

        val steps = 20
        for (i in 1..steps) {
            val fraction = i / steps.toFloat()
            val lat = from.latitude + (to.latitude - from.latitude) * fraction
            val lng = from.longitude + (to.longitude - from.longitude) * fraction
            val point = LatLng(lat, lng)

            animatedProviderLocation = point
            providerMarkerState.position = point
            delay(80)
        }
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val result: Address? = geocoder.getFromLocation(lat, lng, 1).orEmpty().firstOrNull()

                when {
                    result == null -> formatCoordinates(lat, lng)
                    !result.getAddressLine(0).isNullOrBlank() -> result.getAddressLine(0)
                    !result.featureName.isNullOrBlank() && !result.locality.isNullOrBlank() ->
                        "${result.featureName}, ${result.locality}"
                    !result.subLocality.isNullOrBlank() && !result.locality.isNullOrBlank() ->
                        "${result.subLocality}, ${result.locality}"
                    !result.locality.isNullOrBlank() -> result.locality!!
                    !result.subAdminArea.isNullOrBlank() -> result.subAdminArea!!
                    !result.adminArea.isNullOrBlank() -> result.adminArea!!
                    !result.countryName.isNullOrBlank() -> result.countryName!!
                    else -> formatCoordinates(lat, lng)
                }
            } catch (_: Exception) {
                formatCoordinates(lat, lng)
            }
        }
    }

    LaunchedEffect(userLat, userLng) {
        userMarkerState.position = userLocation
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngZoom(userLocation, 15f)
        )
        userPlaceText = reverseGeocode(context, userLat, userLng)
    }

    LaunchedEffect(requestId) {
        SocketManager.joinRequestRoom(requestId)

        SocketManager.listenRequestRoomJoined { joinedRequestId ->
            if (joinedRequestId == requestId) {
                connectionText = "Live tracking connected"
            }
        }

        SocketManager.listenRequestRoomError { msg ->
            connectionText = "Tracking room error: $msg"
        }

        SocketManager.listenProviderLocation { _, lat, lng, incomingRequestId ->
            if (incomingRequestId == null || incomingRequestId == requestId) {
                val newProviderLocation = LatLng(lat, lng)
                val oldAnimated = animatedProviderLocation

                providerLocation = newProviderLocation
                lastUpdateText = "Provider location updating"

                scope.launch {
                    animateProviderMarker(oldAnimated, newProviderLocation)
                    refreshRoute(newProviderLocation, userLocation)

                    if (
                        abs(lat - lastResolvedProviderLat) > 0.0005 ||
                        abs(lng - lastResolvedProviderLng) > 0.0005
                    ) {
                        lastResolvedProviderLat = lat
                        lastResolvedProviderLng = lng
                        providerPlaceText = reverseGeocode(context, lat, lng)
                    }

                    try {
                        val bounds = LatLngBounds.builder()
                            .include(userLocation)
                            .include(newProviderLocation)
                            .build()

                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 160),
                            900
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SocketManager.clearTrackingListeners()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = userMarkerState,
                title = "You",
                snippet = userPlaceText
            )

            animatedProviderLocation?.let {
                Marker(
                    state = providerMarkerState,
                    title = "Provider",
                    snippet = providerPlaceText
                )
            }

            if (routePoints.isNotEmpty()) {
                Polyline(points = routePoints)
            }
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(connectionText, style = MaterialTheme.typography.bodyMedium)
            Text("You: $userPlaceText", style = MaterialTheme.typography.bodySmall)
            Text("Provider: $providerPlaceText", style = MaterialTheme.typography.bodySmall)
            Text("Distance: $distanceText", style = MaterialTheme.typography.bodyMedium)
            Text("ETA: $etaText", style = MaterialTheme.typography.bodyMedium)
            Text(lastUpdateText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatCoordinates(lat: Double, lng: Double): String {
    return "Lat: ${"%.5f".format(lat)}, Lng: ${"%.5f".format(lng)}"
}

private fun formatDuration(duration: String): String {
    val clean = duration.trim()

    if (clean.endsWith("s")) {
        val seconds = clean.removeSuffix("s").toLongOrNull() ?: return clean
        val minutes = seconds / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            minutes > 0 -> "${minutes} min"
            else -> "${seconds} sec"
        }
    }

    return clean
}

private fun estimateDurationFromDistance(origin: LatLng, destination: LatLng): String {
    val km = straightDistanceKm(origin, destination)
    val averageSpeedKmPerHour = 30.0
    val totalHours = km / averageSpeedKmPerHour
    val totalMinutes = (totalHours * 60).toInt().coerceAtLeast(1)
    return "${totalMinutes * 60}s"
}

private fun straightDistanceKm(start: LatLng, end: LatLng): Double {
    val earthRadiusKm = 6371.0

    val dLat = Math.toRadians(end.latitude - start.latitude)
    val dLng = Math.toRadians(end.longitude - start.longitude)
    val startLat = Math.toRadians(start.latitude)
    val endLat = Math.toRadians(end.latitude)

    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(startLat) * kotlin.math.cos(endLat) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)

    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadiusKm * c
}