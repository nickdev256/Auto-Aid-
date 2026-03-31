package com.project.auto_aid.screens.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationPickerScreen(
    navController: NavController,
    initialLat: Double,
    initialLng: Double
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val hasInitialLocation = initialLat != 0.0 && initialLng != 0.0

    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            if (hasInitialLocation) LatLng(initialLat, initialLng) else LatLng(0.0, 0.0),
            if (hasInitialLocation) 16f else 5f
        )
    }

    var pickedLat by remember { mutableDoubleStateOf(if (hasInitialLocation) initialLat else 0.0) }
    var pickedLng by remember { mutableDoubleStateOf(if (hasInitialLocation) initialLng else 0.0) }
    var pickedLabel by remember {
        mutableStateOf(
            if (hasInitialLocation) "Loading place..." else "Move map or use current location"
        )
    }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var resolveJob by remember { mutableStateOf<Job?>(null) }

    fun formatCoords(lat: Double, lng: Double): String {
        return "Lat: ${"%.6f".format(lat)}, Lng: ${"%.6f".format(lng)}"
    }

    fun buildReadableAddress(address: Address?): String? {
        if (address == null) return null

        val fullLine = address.getAddressLine(0)?.trim()
        if (!fullLine.isNullOrBlank()) return fullLine

        val candidates = linkedSetOf<String>()

        address.featureName?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        address.thoroughfare?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        address.subLocality?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        address.locality?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        address.subAdminArea?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        address.adminArea?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        address.countryName?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }

        return candidates.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results: List<Address> = geocoder.getFromLocation(lat, lng, 1).orEmpty()
                buildReadableAddress(results.firstOrNull()) ?: formatCoords(lat, lng)
            } catch (_: Exception) {
                formatCoords(lat, lng)
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    fun resolvePickedAddress(lat: Double, lng: Double) {
        resolveJob?.cancel()
        resolveJob = scope.launch {
            isResolvingAddress = true
            delay(250)
            pickedLabel = reverseGeocode(lat, lng)
            isResolvingAddress = false
        }
    }

    fun moveToCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    pickedLat = location.latitude
                    pickedLng = location.longitude

                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(latLng, 17f),
                            durationMs = 700
                        )
                    }

                    resolvePickedAddress(location.latitude, location.longitude)
                } else {
                    Toast.makeText(
                        context,
                        "Current location not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    "Failed to get current location",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            moveToCurrentLocation()
        } else {
            Toast.makeText(
                context,
                "Location permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestCurrentLocation() {
        if (hasLocationPermission()) {
            moveToCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        when {
            hasInitialLocation -> {
                val initialTarget = LatLng(initialLat, initialLng)
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(initialTarget, 16f)
                )
                resolvePickedAddress(initialLat, initialLng)
            }

            hasLocationPermission() -> {
                moveToCurrentLocation()
            }

            else -> {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target

            if (target.latitude != 0.0 || target.longitude != 0.0) {
                pickedLat = target.latitude
                pickedLng = target.longitude
                resolvePickedAddress(target.latitude, target.longitude)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                if (pickedLat != 0.0 || pickedLng != 0.0) {
                    Marker(
                        state = MarkerState(position = LatLng(pickedLat, pickedLng)),
                        title = "Picked Location",
                        snippet = pickedLabel
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                CenterAlignedTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { Text("Pick location") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (pickedLat == 0.0 && pickedLng == 0.0) {
                                    Toast.makeText(
                                        context,
                                        "Please select a valid location",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@IconButton
                                }

                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("picked_location_lat", pickedLat)

                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("picked_location_lng", pickedLng)

                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("picked_location_label", pickedLabel)

                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm location"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    tonalElevation = 3.dp,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Selected location",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = pickedLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (isResolvingAddress) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator()
                }
            }

            FloatingActionButton(
                onClick = { requestCurrentLocation() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Use current location"
                )
            }
        }
    }
}