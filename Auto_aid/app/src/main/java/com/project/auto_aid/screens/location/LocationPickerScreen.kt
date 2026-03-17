package com.project.auto_aid.screens.location

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

import com.project.auto_aid.components.reverseGeocodeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    navController: NavHostController,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0
) {
    val context = LocalContext.current

    val start = LatLng(initialLat, initialLng)

    var pickedLat by remember { mutableDoubleStateOf(initialLat) }
    var pickedLng by remember { mutableDoubleStateOf(initialLng) }

    var label by remember { mutableStateOf("") }
    var loadingLabel by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, 16f)
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            pickedLat = center.latitude
            pickedLng = center.longitude

            loadingLabel = true
            label = runCatching {
                reverseGeocodeLabel(context, pickedLat, pickedLng)
            }.getOrElse {
                "${"%.5f".format(pickedLat)}, ${"%.5f".format(pickedLng)}"
            }
            loadingLabel = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Location") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("picked_location_label", label)

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("picked_location_lat", pickedLat)

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("picked_location_lng", pickedLng)

                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm")
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
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                )
            )

            // Center pin
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
            )

            // Bottom label card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (loadingLabel) "Getting address..." else label.ifBlank {
                            "Move map to choose a place"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Lat: ${"%.5f".format(pickedLat)}   Lng: ${"%.5f".format(pickedLng)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}