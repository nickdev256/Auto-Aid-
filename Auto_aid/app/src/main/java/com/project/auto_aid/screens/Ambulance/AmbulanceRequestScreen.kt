package com.project.auto_aid.screens.ambulance

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
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.CreateRequestBody
import com.project.auto_aid.data.network.dto.LocationBody
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbulanceRequestScreen(
    navController: NavHostController,
    providerId: String? = null,
    userLat: Double = 0.3476,
    userLng: Double = 32.5825
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }

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

    // 🔥 AI SUPPORT
    val prev = navController.previousBackStackEntry
    val aiState = prev?.savedStateHandle ?: savedStateHandle

    val aiProblem = aiState?.get<String>("ai_problem").orEmpty()
    val aiUrgency = aiState?.get<String>("ai_urgency").orEmpty()
    val aiNote = aiState?.get<String>("ai_note").orEmpty()
    val selectedProviderId = providerId ?: aiState?.get<String>("selected_provider_id")

    var emergencyType by remember { mutableStateOf("Medical Emergency") }
    var patientCondition by remember { mutableStateOf(aiProblem) }
    var notes by remember { mutableStateOf(aiNote) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Ambulance") },
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
                AssistChip(onClick = {}, label = { Text("Broadcast to all ambulance providers") })
            }

            if (pickedLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text("Location: $pickedLabel") })
            }

            // 🔥 AI SUMMARY
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

            Text("Emergency Type", fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = emergencyType == "Accident",
                    onClick = { emergencyType = "Accident" },
                    label = { Text("Accident") }
                )
                FilterChip(
                    selected = emergencyType == "Medical Emergency",
                    onClick = { emergencyType = "Medical Emergency" },
                    label = { Text("Medical") }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = emergencyType == "Labour / Pregnancy",
                    onClick = { emergencyType = "Labour / Pregnancy" },
                    label = { Text("Labour") }
                )
                FilterChip(
                    selected = emergencyType == "Breathing Problem",
                    onClick = { emergencyType = "Breathing Problem" },
                    label = { Text("Breathing") }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = emergencyType == "Unconscious Patient",
                    onClick = { emergencyType = "Unconscious Patient" },
                    label = { Text("Unconscious") }
                )
                FilterChip(
                    selected = emergencyType == "Other",
                    onClick = { emergencyType = "Other" },
                    label = { Text("Other") }
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = patientCondition,
                onValueChange = { patientCondition = it },
                label = { Text("Patient Condition") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Your current location will be used to dispatch the nearest ambulance quickly.",
                style = MaterialTheme.typography.bodySmall
            )

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    if (submitting) return@Button

                    val token = tokenStore.getToken()
                    if (token.isNullOrBlank()) {
                        error = "Login first"
                        return@Button
                    }

                    if (patientCondition.isBlank()) {
                        error = "Describe patient condition"
                        return@Button
                    }

                    submitting = true

                    scope.launch {
                        try {
                            val response = api.createRequest(
                                CreateRequestBody(
                                    service = "ambulance",
                                    providerType = "ambulance",
                                    vehicleInfo = emergencyType,
                                    problem = buildString {
                                        append(patientCondition)
                                        if (notes.isNotBlank()) {
                                            append(" | Notes: $notes")
                                        }
                                        if (aiUrgency.isNotBlank()) {
                                            append(" | Urgency: $aiUrgency")
                                        }
                                    },
                                    towType = emergencyType,
                                    userLocation = LocationBody(finalLat, finalLng),
                                    targetProviderId = selectedProviderId
                                )
                            )

                            val data: RequestDto = response.body()!!
                            val rid = data._id ?: data.id ?: ""

                            tokenStore.saveLastAmbulanceRequestId(rid)

                            // 🔥 CLEAR AI STATE
                            aiState?.remove<String>("ai_problem")
                            aiState?.remove<String>("ai_urgency")
                            aiState?.remove<String>("ai_note")
                            aiState?.remove<String>("selected_provider_id")

                            navController.navigate(Routes.AmbulanceActiveScreen.createRoute(rid))

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
                else Text("Request Ambulance")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}