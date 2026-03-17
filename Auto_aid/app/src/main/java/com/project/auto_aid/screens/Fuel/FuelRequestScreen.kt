package com.project.auto_aid.screens.fuel

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.CreateRequestBody
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.LocationBody
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelRequestScreen(
    navController: NavHostController,
    providerId: String? = null,
    userLat: Double = 0.3476,
    userLng: Double = 32.5825
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }

    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        ?: navController.currentBackStackEntry?.savedStateHandle

    val pickedLocationLabelState =
        savedStateHandle?.getStateFlow("picked_location_label", "")?.collectAsState()
    val pickedLocationLatState =
        savedStateHandle?.getStateFlow("picked_location_lat", userLat)?.collectAsState()
    val pickedLocationLngState =
        savedStateHandle?.getStateFlow("picked_location_lng", userLng)?.collectAsState()

    val pickedLabel = pickedLocationLabelState?.value.orEmpty()
    val finalLat = pickedLocationLatState?.value ?: userLat
    val finalLng = pickedLocationLngState?.value ?: userLng

    var fuelType by remember { mutableStateOf("Petrol") }
    var quantity by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoRefreshKey by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) error = "Camera permission denied. Enable it in Settings."
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            error = "Photo capture cancelled."
            photoUri = null
        } else {
            error = null
            photoRefreshKey++
        }
    }

    fun createImageUri(): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "fuel_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Fuel") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            if (providerId != null) {
                AssistChip(onClick = {}, label = { Text("Target provider selected") })
            } else {
                AssistChip(
                    onClick = {},
                    label = { Text("Broadcast to all online fuel providers") }
                )
            }

            if (pickedLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Location: $pickedLabel") }
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Coordinates: $finalLat, $finalLng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text("Fuel Type", fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = fuelType == "Petrol",
                    onClick = { fuelType = "Petrol" },
                    label = { Text("Petrol") }
                )
                FilterChip(
                    selected = fuelType == "Diesel",
                    onClick = { fuelType = "Diesel" },
                    label = { Text("Diesel") }
                )
                FilterChip(
                    selected = fuelType == "Kerosene",
                    onClick = { fuelType = "Kerosene" },
                    label = { Text("Kerosene") }
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity (Litres)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text("Payment Method", fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = paymentMethod == "Cash",
                    onClick = { paymentMethod = "Cash" },
                    label = { Text("Cash") }
                )
                FilterChip(
                    selected = paymentMethod == "Mobile Money",
                    onClick = { paymentMethod = "Mobile Money" },
                    label = { Text("Mobile Money") }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Dashboard Fuel Gauge Photo", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Take a clear photo from inside the car showing the dashboard fuel gauge on empty or near empty.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Make sure the fuel meter is clearly visible. If possible, turn ignition on so the gauge shows properly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!hasPermission) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    return@OutlinedButton
                                }

                                val uri = createImageUri()
                                photoUri = uri
                                takePictureLauncher.launch(uri)
                            }
                        ) {
                            Text(if (photoUri == null) "Take Dashboard Photo" else "Retake Photo")
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text(if (photoUri != null) "Photo added" else "No photo") }
                        )
                    }

                    if (photoUri != null) {
                        Spacer(Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = "${photoUri}?v=$photoRefreshKey"),
                                contentDescription = "Fuel dashboard preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    if (submitting) return@Button

                    val token = tokenStore.getToken()
                    if (token.isNullOrBlank()) {
                        error = "Please login first."
                        return@Button
                    }

                    if (quantity.isBlank()) {
                        error = "Please enter quantity in litres."
                        return@Button
                    }

                    if (photoUri == null) {
                        error = "Please take a dashboard fuel gauge photo first."
                        return@Button
                    }

                    submitting = true
                    error = null

                    scope.launch {
                        try {
                            val response = api.createRequest(
                                CreateRequestBody(
                                    service = "fuel",
                                    providerType = "fuel",
                                    vehicleInfo = "$fuelType • ${quantity.trim()} L",
                                    problem = "Payment: $paymentMethod",
                                    towType = fuelType,
                                    userLocation = LocationBody(finalLat, finalLng),
                                    targetProviderId = providerId
                                )
                            )

                            if (!response.isSuccessful) {
                                throw Exception("Request failed (HTTP ${response.code()})")
                            }

                            val created: RequestDto =
                                response.body() ?: throw Exception("Empty response body")

                            val rid = created._id ?: created.id ?: ""
                            if (rid.isBlank()) throw Exception("Missing request ID")

                            tokenStore.saveLastFuelRequestId(rid)
                            navController.navigate(Routes.FuelActiveScreen.createRoute(rid))
                        } catch (e: Throwable) {
                            error = e.message ?: "Failed to submit."
                        } finally {
                            submitting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !submitting,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send Request", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}