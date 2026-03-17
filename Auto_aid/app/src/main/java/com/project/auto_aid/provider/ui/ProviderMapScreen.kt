package com.project.auto_aid.provider.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderMapScreen(
    requestId: String,
    pickupLat: Double,
    pickupLng: Double
) {
    val context = LocalContext.current

    var providerLocation by remember { mutableStateOf<LatLng?>(null) }
    var screenError by remember { mutableStateOf<String?>(null) }

    val userLocation = remember(pickupLat, pickupLng) {
        LatLng(pickupLat, pickupLng)
    }

    val fused = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasLocationPermission()) {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        providerLocation = LatLng(loc.latitude, loc.longitude)
                        screenError = null
                    } else {
                        screenError = "Could not get current provider location."
                    }
                }
                .addOnFailureListener {
                    screenError = "Failed to get current provider location."
                }
        } else {
            screenError = "Location permission denied."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        providerLocation = LatLng(loc.latitude, loc.longitude)
                        screenError = null
                    } else {
                        screenError = "Could not get current provider location."
                    }
                }
                .addOnFailureListener {
                    screenError = "Failed to get current provider location."
                }
        }
    }

    fun openGoogleNavigation(dest: LatLng) {
        val uri = Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallbackUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=${dest.latitude},${dest.longitude}&travelmode=driving"
            )
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            context.startActivity(fallbackIntent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider → User Route") }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { openGoogleNavigation(userLocation) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text("Navigate in Google Maps")
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            screenError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission()
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = hasLocationPermission()
                )
            ) {
                providerLocation?.let { provider ->
                    Marker(
                        state = MarkerState(position = provider),
                        title = "You (Provider)"
                    )
                }

                Marker(
                    state = MarkerState(position = userLocation),
                    title = "User Pickup"
                )
            }
        }
    }
}