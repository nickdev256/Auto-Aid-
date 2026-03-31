package com.project.auto_aid.provider.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.project.auto_aid.data.network.SocketManager

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

    val userLocation = remember { LatLng(pickupLat, pickupLng) }

    val fused = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    // 🔥 START LIVE LOCATION
    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasLocationPermission()) {
            screenError = "Location permission required"
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(2000L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                val latLng = LatLng(loc.latitude, loc.longitude)
                providerLocation = latLng

                // 🔥 SEND TO SERVER
                SocketManager.sendProviderLocation(loc.latitude, loc.longitude)

                // 🔥 MOVE CAMERA
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLng(latLng)
                )
            }
        }

        locationCallback = callback

        fused.requestLocationUpdates(
            request,
            callback,
            context.mainLooper
        )
    }

    fun stopTracking() {
        locationCallback?.let {
            fused.removeLocationUpdates(it)
        }
    }

    // 🔥 START ON LOAD
    LaunchedEffect(Unit) {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startTracking()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopTracking()
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
                "https://www.google.com/maps/dir/?api=1&destination=${dest.latitude},${dest.longitude}"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Provider → User Route") })
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

            screenError?.let {
                Text(
                    text = it,
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

                // 🚗 PROVIDER (LIVE)
                providerLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "You (Provider)"
                    )
                }

                // 👤 USER
                Marker(
                    state = MarkerState(position = userLocation),
                    title = "User Pickup"
                )
            }
        }
    }
}