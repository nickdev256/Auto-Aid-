package com.project.auto_aid.screens.tracking

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun LiveTrackingScreen(
    requestId: String,
    userLat: Double,
    userLng: Double
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    val userLocation = remember(userLat, userLng) { LatLng(userLat, userLng) }

    var providerLocation by remember { mutableStateOf<LatLng?>(null) }
    var animatedProviderLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    var connectionText by remember { mutableStateOf("Connecting live tracking...") }
    var distanceText by remember { mutableStateOf("-- km") }
    var etaText by remember { mutableStateOf("--") }
    var statusText by remember { mutableStateOf("Waiting for provider location...") }

    var userPlaceText by remember { mutableStateOf("Loading your location...") }
    var providerPlaceText by remember { mutableStateOf("Waiting for provider location...") }

    var lastResolvedProviderLat by remember { mutableDoubleStateOf(0.0) }
    var lastResolvedProviderLng by remember { mutableDoubleStateOf(0.0) }

    val cameraPositionState = rememberCameraPositionState()
    val userMarkerState = remember(userLat, userLng) { MarkerState(position = userLocation) }
    val providerMarkerState = remember { MarkerState(position = userLocation) }

    suspend fun animateProviderMarker(from: LatLng?, to: LatLng) {
        if (from == null) {
            animatedProviderLocation = to
            providerMarkerState.position = to
            return
        }

        val steps = 24
        for (i in 1..steps) {
            val fraction = i / steps.toFloat()
            val lat = from.latitude + (to.latitude - from.latitude) * fraction
            val lng = from.longitude + (to.longitude - from.longitude) * fraction
            val point = LatLng(lat, lng)

            animatedProviderLocation = point
            providerMarkerState.position = point
            delay(60)
        }
    }

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

                val encodedPolyline = body?.encodedPolyline.orEmpty()
                val distanceMeters = body?.distanceMeters ?: 0
                val duration = body?.duration.orEmpty()

                routePoints = if (encodedPolyline.isNotBlank()) {
                    PolylineUtils.decode(encodedPolyline)
                } else {
                    listOf(origin, destination)
                }

                distanceText = if (distanceMeters > 0) {
                    String.format("%.2f km", distanceMeters / 1000.0)
                } else {
                    String.format("%.2f km", straightDistanceKm(origin, destination))
                }

                etaText = if (duration.isNotBlank()) {
                    formatDuration(duration)
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

    suspend fun fitBothLocations(user: LatLng, provider: LatLng) {
        try {
            val bounds = LatLngBounds.builder()
                .include(user)
                .include(provider)
                .build()

            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 180),
                durationMs = 900
            )
        } catch (_: Exception) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(provider, 15f),
                durationMs = 700
            )
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

        SocketManager.listenRequestRoomError { message ->
            connectionText = "Tracking error: $message"
        }

        SocketManager.listenProviderLocation { _, lat, lng, incomingRequestId ->
            if (incomingRequestId == null || incomingRequestId == requestId) {
                val newProviderLocation = LatLng(lat, lng)
                val previousLocation = animatedProviderLocation

                providerLocation = newProviderLocation
                statusText = "Provider is moving"

                scope.launch {
                    animateProviderMarker(previousLocation, newProviderLocation)
                    refreshRoute(newProviderLocation, userLocation)
                    fitBothLocations(userLocation, newProviderLocation)

                    if (
                        abs(lat - lastResolvedProviderLat) > 0.0005 ||
                        abs(lng - lastResolvedProviderLng) > 0.0005
                    ) {
                        lastResolvedProviderLat = lat
                        lastResolvedProviderLng = lng
                        providerPlaceText = reverseGeocode(context, lat, lng)
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
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                compassEnabled = true,
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            Marker(
                state = userMarkerState,
                title = "You",
                snippet = userPlaceText,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )

            animatedProviderLocation?.let {
                Marker(
                    state = providerMarkerState,
                    title = "Provider",
                    snippet = providerPlaceText
                )
            }

            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    width = 14f,
                    geodesic = true,
                    jointType = JointType.ROUND
                )
            }
        }

        TrackingTopCard(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            connectionText = connectionText,
            distanceText = distanceText,
            etaText = etaText,
            statusText = statusText,
            userPlaceText = userPlaceText,
            providerPlaceText = providerPlaceText
        )
    }
}

@Composable
private fun TrackingTopCard(
    modifier: Modifier = Modifier,
    connectionText: String,
    distanceText: String,
    etaText: String,
    statusText: String,
    userPlaceText: String,
    providerPlaceText: String
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Provider live tracking",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = connectionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "You: $userPlaceText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Provider: $providerPlaceText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    title = "Distance",
                    value = distanceText
                )

                StatChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccessTime,
                    title = "ETA",
                    value = etaText
                )

                StatChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MyLocation,
                    title = "Status",
                    value = statusText
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall
            )
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

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(startLat) * cos(endLat) *
            sin(dLng / 2) * sin(dLng / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}