package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.provider.ProviderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderRequestDetailsScreen(
    navController: NavHostController,
    requestId: String
) {
    val context = LocalContext.current
    val api = remember(context) { RetrofitClient.create(TokenStore(context)) }
    val vm: ProviderViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var request by remember { mutableStateOf<RequestDto?>(null) }
    var working by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            err = null
            try {
                val res = api.getRequestById(requestId)
                if (!res.isSuccessful) throw Exception("Failed to load request (HTTP ${res.code()})")
                request = res.body()
                if (request == null) throw Exception("Empty request response")
            } catch (e: Throwable) {
                err = e.message ?: "Failed to load request"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(requestId) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) { CircularProgressIndicator() }
            }

            err != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(err ?: "Error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { load() }) { Text("Retry") }
                }
            }

            else -> {
                val r = request!!

                val status = (r.status ?: "pending").trim().lowercase()
                val providerType = (r.providerType ?: "").trim()
                val service = (r.service ?: "").trim()
                val vehicleInfo = (r.vehicleInfo ?: "").trim()
                val problem = (r.problem ?: "").trim()
                val towType = (r.towType ?: "").trim()

                val lat = r.userLocation?.lat ?: 0.0
                val lng = r.userLocation?.lng ?: 0.0
                val hasUserLocation = lat != 0.0 && lng != 0.0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Request ID", fontWeight = FontWeight.Bold)
                            Text(requestId)

                            Spacer(Modifier.height(10.dp))

                            Text("Status", fontWeight = FontWeight.Bold)
                            AssistChip(onClick = {}, label = { Text(status) })

                            Spacer(Modifier.height(10.dp))

                            Text("Type", fontWeight = FontWeight.Bold)
                            Text(if (providerType.isBlank()) "-" else providerType)

                            Spacer(Modifier.height(10.dp))

                            Text("Service", fontWeight = FontWeight.Bold)
                            Text(if (service.isBlank()) "-" else service)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Vehicle Info", fontWeight = FontWeight.Bold)
                            Text(if (vehicleInfo.isBlank()) "-" else vehicleInfo)

                            Spacer(Modifier.height(10.dp))

                            Text("Problem", fontWeight = FontWeight.Bold)
                            Text(if (problem.isBlank()) "-" else problem)

                            if (towType.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text("Tow Type", fontWeight = FontWeight.Bold)
                                Text(towType)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("User Location", fontWeight = FontWeight.Bold)

                            if (hasUserLocation) {
                                Text("Lat: $lat")
                                Text("Lng: $lng")
                            } else {
                                Text(
                                    "No pickup location saved for this request.",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Fix: ensure the user app sends lat/lng when creating the request.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    if (!hasUserLocation) return@OutlinedButton

                                    navController.navigate(
                                        Routes.ProviderMapScreen.createRoute(
                                            requestId = requestId,
                                            pickupLat = lat,
                                            pickupLng = lng
                                        )
                                    )
                                },
                                enabled = hasUserLocation,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Map")
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // ✅ ACTIONS
                    if (status == "pending") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (working) return@OutlinedButton
                                    working = true
                                    scope.launch {
                                        try {
                                            vm.decline(requestId)
                                            navController.popBackStack()
                                        } finally {
                                            working = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Decline") }

                            Button(
                                onClick = {
                                    if (working) return@Button
                                    working = true
                                    scope.launch {
                                        try {
                                            vm.accept(requestId)
                                            navController.navigate(
                                                Routes.ProviderActiveJob.createRoute(requestId)
                                            ) {
                                                popUpTo(Routes.ProviderDashboard.route) { inclusive = false }
                                            }
                                        } finally {
                                            working = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Accept") }
                        }
                    } else {
                        Button(
                            onClick = {
                                navController.navigate(
                                    Routes.ProviderActiveJob.createRoute(requestId)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open Job") }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}