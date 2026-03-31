package com.project.auto_aid.screens.towing

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
fun TowingRequestScreen(
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

    // 🔥 AI + Direct Flow Support
    val prev = navController.previousBackStackEntry
    val aiState = prev?.savedStateHandle ?: savedStateHandle

    val aiProblem = aiState?.get<String>("ai_problem").orEmpty()
    val aiUrgency = aiState?.get<String>("ai_urgency").orEmpty()
    val aiNote = aiState?.get<String>("ai_note").orEmpty()
    val aiVehicleInfo = aiState?.get<String>("ai_vehicle_info").orEmpty()
    val selectedProviderId = providerId ?: aiState?.get<String>("selected_provider_id")

    var vehicleInfo by remember { mutableStateOf(aiVehicleInfo) }
    var problem by remember { mutableStateOf(aiProblem) }
    var towType by remember { mutableStateOf("Standard") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoRefreshKey by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) error = "Camera permission denied."
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            error = "Photo cancelled."
            photoUri = null
        } else {
            error = null
            photoRefreshKey++
        }
    }

    fun createImageUri(): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "req_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Towing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
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

            if (selectedProviderId != null) {
                AssistChip(onClick = {}, label = { Text("Target provider selected") })
            } else {
                AssistChip(onClick = {}, label = { Text("Broadcast to all providers") })
            }

            if (pickedLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text("Location: $pickedLabel") })
            }

            // 🔥 AI Summary
            if (aiProblem.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("AutoAid AI Suggestion", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(aiProblem)

                        if (aiUrgency.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Urgency: $aiUrgency")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = vehicleInfo,
                onValueChange = { vehicleInfo = it },
                label = { Text("Vehicle Info") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = problem,
                onValueChange = { problem = it },
                label = { Text("Problem") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text("Tow Type", fontWeight = FontWeight.Bold)

            Row {
                FilterChip(
                    selected = towType == "Standard",
                    onClick = { towType = "Standard" },
                    label = { Text("Standard") }
                )
                Spacer(Modifier.width(10.dp))
                FilterChip(
                    selected = towType == "Flatbed",
                    onClick = { towType = "Flatbed" },
                    label = { Text("Flatbed") }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val token = tokenStore.getToken()
                    if (token.isNullOrBlank()) {
                        error = "Login first"
                        return@Button
                    }

                    if (vehicleInfo.isBlank() || problem.isBlank()) {
                        error = "Fill all fields"
                        return@Button
                    }

                    if (photoUri == null) {
                        error = "Take photo"
                        return@Button
                    }

                    submitting = true

                    scope.launch {
                        try {
                            val response = api.createRequest(
                                CreateRequestBody(
                                    service = "towing",
                                    providerType = "towing",
                                    vehicleInfo = vehicleInfo,
                                    problem = buildString {
                                        append(problem)
                                        if (aiUrgency.isNotBlank()) {
                                            append("\nUrgency: $aiUrgency")
                                        }
                                    },
                                    towType = towType,
                                    userLocation = LocationBody(finalLat, finalLng),
                                    targetProviderId = selectedProviderId
                                )
                            )

                            val data: RequestDto = response.body()!!

                            val rid = data._id ?: data.id ?: ""

                            tokenStore.saveLastTowingRequestId(rid)

                            // 🔥 CLEAR AI
                            aiState?.remove<String>("ai_problem")
                            aiState?.remove<String>("ai_urgency")
                            aiState?.remove<String>("selected_provider_id")

                            navController.navigate(Routes.TowingActiveScreen.createRoute(rid))

                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            submitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) CircularProgressIndicator()
                else Text("Send Request")
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}