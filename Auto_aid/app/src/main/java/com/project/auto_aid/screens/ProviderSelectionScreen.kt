package com.project.auto_aid.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ProviderLiteDto
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSelectionScreen(
    navController: NavHostController,
    providerType: String,
    userLat: Double,
    userLng: Double,
    pickedLocationLabel: String = ""
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var retryKey by remember { mutableIntStateOf(0) }
    val providers = remember { mutableStateListOf<ProviderLiteDto>() }

    val previousStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val currentStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val aiProblem = previousStateHandle?.get<String>("ai_problem").orEmpty()
    val aiUrgency = previousStateHandle?.get<String>("ai_urgency").orEmpty()
    val aiNote = previousStateHandle?.get<String>("ai_note").orEmpty()
    val aiVehicleInfo = previousStateHandle?.get<String>("ai_vehicle_info").orEmpty()

    val decodedLocationLabel = remember(pickedLocationLabel) {
        try {
            URLDecoder.decode(pickedLocationLabel, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            pickedLocationLabel
        }
    }

    LaunchedEffect(providerType, retryKey) {
        isLoading = true
        errorMessage = ""
        providers.clear()

        try {
            val response = withContext(Dispatchers.IO) {
                api.getAvailableProviders(
                    providerType = providerType,
                    lat = null,
                    lng = null,
                    onlineOnly = false
                )
            }

            if (response.isSuccessful) {
                val sortedProviders = response.body()
                    .orEmpty()
                    .sortedWith(
                        compareByDescending<ProviderLiteDto> { it.rating ?: 0.0 }
                            .thenBy { it.businessName?.trim().orEmpty().ifBlank { it.name.orEmpty() } }
                    )

                providers.addAll(sortedProviders)
            } else {
                val serverMessage = try {
                    response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                }

                errorMessage = serverMessage ?: "Failed to load providers (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load providers"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${serviceDisplayName(providerType)} Providers")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            if (decodedLocationLabel.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF19ABD9)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pickup / Service Location",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = decodedLocationLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (aiProblem.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AutoAid AI Analysis",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Problem",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = aiProblem,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF374151)
                        )

                        if (aiUrgency.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Risk Level: $aiUrgency",
                                style = MaterialTheme.typography.bodySmall,
                                color = urgencyColor(aiUrgency),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (aiNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Recommended Action",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = aiNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF374151)
                            )
                        }

                        if (aiVehicleInfo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Vehicle Info",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = aiVehicleInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color.Red,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { retryKey++ },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                providers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No registered ${serviceDisplayName(providerType)} providers found.",
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { retryKey++ },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh")
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = if (aiProblem.isNotBlank()) {
                            "Choose a provider to continue with the AI-assisted request"
                        } else {
                            "Choose a provider to continue to the request form"
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = providers,
                            key = { provider ->
                                provider.resolvedId().ifBlank {
                                    provider.name.orEmpty()
                                }
                            }
                        ) { provider ->
                            ProviderSelectionCard(
                                provider = provider,
                                serviceType = providerType,
                                aiUrgency = aiUrgency,
                                onChoose = {
                                    val providerId = provider.resolvedId()

                                    if (providerId.isBlank()) {
                                        errorMessage = "Selected provider ID is missing."
                                        return@ProviderSelectionCard
                                    }

                                    currentStateHandle?.set("ai_problem", aiProblem)
                                    currentStateHandle?.set("ai_urgency", aiUrgency)
                                    currentStateHandle?.set("ai_note", aiNote)
                                    currentStateHandle?.set("ai_vehicle_info", aiVehicleInfo)
                                    currentStateHandle?.set("selected_provider_id", providerId)
                                    currentStateHandle?.set("picked_location_label", decodedLocationLabel)
                                    currentStateHandle?.set("picked_location_lat", userLat)
                                    currentStateHandle?.set("picked_location_lng", userLng)

                                    val route = when (providerType.lowercase()) {
                                        "garage" -> Routes.GarageRequestScreen.createRoute(providerId)
                                        "fuel" -> Routes.FuelRequestScreen.createRoute(providerId)
                                        "towing" -> Routes.TowingRequestScreen.createRoute(providerId)
                                        "ambulance" -> Routes.AmbulanceRequestScreen.createRoute(providerId)
                                        else -> null
                                    }

                                    if (route != null) {
                                        navController.navigate(route)
                                    } else {
                                        errorMessage = "Unknown service type: $providerType"
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSelectionCard(
    provider: ProviderLiteDto,
    serviceType: String,
    aiUrgency: String = "",
    onChoose: () -> Unit
) {
    val isOnline = provider.isOnline == true
    val isAvailable = provider.isAvailable == true
    val canChoose = true
    val showAiRecommended = aiUrgency.equals("high", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFEAF8FD), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = serviceIconFor(serviceType),
                        contentDescription = null,
                        tint = Color(0xFF19ABD9)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.businessName?.takeIf { it.isNotBlank() }
                            ?: provider.name.orEmpty(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Text(
                        text = provider.name?.takeIf { it.isNotBlank() } ?: "Provider",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                StatusBadge(
                    text = if (isOnline) "Online" else "Offline",
                    bg = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
                    fg = if (isOnline) Color(0xFF166534) else Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    icon = Icons.Default.Star,
                    text = String.format(Locale.US, "%.1f", provider.rating ?: 0.0)
                )

                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = provider.distanceKm?.let {
                        String.format(Locale.US, "%.1f km away", it)
                    } ?: "Registered Provider"
                )

                StatusBadge(
                    text = if (isAvailable) "Available" else "Busy",
                    bg = if (isAvailable) Color(0xFFDBEAFE) else Color(0xFFFEE2E2),
                    fg = if (isAvailable) Color(0xFF1D4ED8) else Color(0xFFB91C1C)
                )
            }

            if (showAiRecommended) {
                Spacer(modifier = Modifier.height(10.dp))
                StatusBadge(
                    text = "AI Recommended",
                    bg = Color(0xFFFEF3C7),
                    fg = Color(0xFF92400E)
                )
            }

            if (!provider.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = provider.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onChoose,
                enabled = canChoose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF19ABD9),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFBFDDE7),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Continue")
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF3F4F6), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    bg: Color,
    fg: Color
) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun serviceDisplayName(type: String): String {
    return when (type.lowercase()) {
        "garage" -> "Garage"
        "fuel" -> "Fuel Delivery"
        "towing" -> "Towing"
        "ambulance" -> "Ambulance"
        else -> "Service"
    }
}

private fun serviceIconFor(type: String): ImageVector {
    return when (type.lowercase()) {
        "garage" -> Icons.Default.CarRepair
        "fuel" -> Icons.Default.LocalGasStation
        "towing" -> Icons.Default.LocalShipping
        "ambulance" -> Icons.Default.MedicalServices
        else -> Icons.Default.Person
    }
}

private fun urgencyColor(urgency: String): Color {
    return when (urgency.trim().lowercase()) {
        "high", "critical", "severe" -> Color(0xFFDC2626)
        "medium", "moderate" -> Color(0xFFD97706)
        else -> Color(0xFF1D4ED8)
    }
}