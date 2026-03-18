package com.project.auto_aid.screens.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.project.auto_aid.components.LocationSelectionKeys
import com.project.auto_aid.components.reverseGeocodeLabel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun openLocationSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    navController: NavHostController,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var pickedLat by remember { mutableDoubleStateOf(0.0) }
    var pickedLng by remember { mutableDoubleStateOf(0.0) }
    var label by remember { mutableStateOf("") }
    var loadingLabel by remember { mutableStateOf(false) }
    var loadingStartLocation by remember { mutableStateOf(true) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var cameraReady by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 16f)
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

    suspend fun loadLabelForCurrentPoint() {
        loadingLabel = true
        label = runCatching {
            reverseGeocodeLabel(context, pickedLat, pickedLng)
        }.getOrElse {
            "${"%.5f".format(pickedLat)}, ${"%.5f".format(pickedLng)}"
        }
        loadingLabel = false
    }

    suspend fun moveToStartLocation() {
        loadingStartLocation = true
        locationError = null

        try {
            val startLatLng = if (initialLat != 0.0 || initialLng != 0.0) {
                LatLng(initialLat, initialLng)
            } else {
                if (!hasLocationPermission()) {
                    locationError = "Location permission is required."
                    null
                } else {
                    val lastLocation = runCatching { fusedClient.lastLocation.await() }.getOrNull()
                    val currentLocation = lastLocation ?: runCatching {
                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            CancellationTokenSource().token
                        ).await()
                    }.getOrNull()

                    if (currentLocation != null) {
                        LatLng(currentLocation.latitude, currentLocation.longitude)
                    } else {
                        locationError = "Could not get your current location. Turn on GPS and try again."
                        null
                    }
                }
            }

            if (startLatLng != null) {
                pickedLat = startLatLng.latitude
                pickedLng = startLatLng.longitude
                cameraPositionState.position = CameraPosition.fromLatLngZoom(startLatLng, 16f)
                cameraReady = true
                loadLabelForCurrentPoint()
            } else {
                cameraReady = false
            }
        } catch (_: Throwable) {
            cameraReady = false
            locationError = "Failed to get your current location."
        } finally {
            loadingStartLocation = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fine || coarse) {
            scope.launch {
                moveToStartLocation()
            }
        } else {
            loadingStartLocation = false
            cameraReady = false
            locationError = "Location permission denied."
        }
    }

    LaunchedEffect(initialLat, initialLng) {
        if (initialLat != 0.0 || initialLng != 0.0) {
            moveToStartLocation()
        } else {
            if (hasLocationPermission()) {
                moveToStartLocation()
            } else {
                loadingStartLocation = false
                cameraReady = false
                locationError = "Location permission is required."
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving, cameraReady) {
        if (cameraReady && !cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            pickedLat = center.latitude
            pickedLng = center.longitude
            loadLabelForCurrentPoint()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Location") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!cameraReady) return@IconButton

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(LocationSelectionKeys.PICKED_LOCATION_LABEL, label)

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(LocationSelectionKeys.PICKED_LOCATION_LAT, pickedLat)

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(LocationSelectionKeys.PICKED_LOCATION_LNG, pickedLng)

                            navController.popBackStack()
                        },
                        enabled = cameraReady
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loadingStartLocation -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !cameraReady -> {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = locationError ?: "Location is required to continue.",
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Please turn on your GPS and allow location access.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { openLocationSettings(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Turn On Location")
                            }
                        }
                    }
                }

                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission()
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = hasLocationPermission(),
                            zoomControlsEnabled = false
                        )
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp)
                    )

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (loadingLabel) {
                                    "Getting address..."
                                } else {
                                    label.ifBlank { "Move map to choose a place" }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Lat: ${"%.5f".format(pickedLat)}   Lng: ${"%.5f".format(pickedLng)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}