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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.project.auto_aid.components.LocationSelectionKeys
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ProviderLiteDto
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val providers = remember { mutableStateListOf<ProviderLiteDto>() }

    LaunchedEffect(providerType, userLat, userLng) {
        isLoading = true
        errorMessage = ""
        providers.clear()

        if (userLat == 0.0 && userLng == 0.0) {
            errorMessage = "User location is missing. Please select a valid location."
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val response = withContext(Dispatchers.IO) {
                api.getAvailableProviders(
                    providerType = providerType,
                    lat = userLat,
                    lng = userLng,
                    onlineOnly = false
                )
            }

            if (response.isSuccessful) {
                providers.addAll(response.body().orEmpty())
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

    fun continueToRequest(provider: ProviderLiteDto) {
        val providerId = provider.resolvedId()
        if (providerId.isBlank()) {
            errorMessage = "Selected provider ID is missing."
            return
        }

        navController.currentBackStackEntry?.savedStateHandle?.apply {
            set(LocationSelectionKeys.PICKED_LOCATION_LABEL, pickedLocationLabel)
            set(LocationSelectionKeys.PICKED_LOCATION_LAT, userLat)
            set(LocationSelectionKeys.PICKED_LOCATION_LNG, userLng)

            set("selected_provider_id", providerId)
            set("selected_provider_name", provider.name.orEmpty())
            set("selected_provider_business_name", provider.businessName.orEmpty())
            set("selected_provider_type", providerType)
        }

        when (providerType.lowercase()) {
            "garage" -> {
                navController.navigate(
                    Routes.GarageRequestScreen.createRoute(providerId)
                )
            }
            "fuel" -> {
                navController.navigate(
                    Routes.FuelRequestScreen.createRoute(providerId)
                )
            }
            "towing" -> {
                navController.navigate(
                    Routes.TowingRequestScreen.createRoute(providerId)
                )
            }
            "ambulance" -> {
                navController.navigate(
                    Routes.AmbulanceRequestScreen.createRoute(providerId)
                )
            }
            else -> {
                errorMessage = "Unknown service type: $providerType"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${serviceDisplayName(providerType)} Providers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            if (pickedLocationLabel.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = pickedLocationLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lat: $userLat, Lng: $userLng",
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
                        Text(
                            text = errorMessage,
                            color = Color.Red
                        )
                    }
                }

                providers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ${serviceDisplayName(providerType)} providers found nearby.",
                            color = Color.Gray
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Choose a provider to continue to the request form",
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
                                onChoose = {
                                    continueToRequest(provider)
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
    onChoose: () -> Unit
) {
    val isOnline = provider.isOnline == true
    val isAvailable = provider.isAvailable == true
    val canChoose = isAvailable

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
                    text = String.format("%.1f", provider.rating ?: 0.0)
                )

                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = provider.distanceKm?.let { "$it km away" } ?: "Distance unknown"
                )

                StatusBadge(
                    text = if (isAvailable) "Available" else "Busy",
                    bg = if (isAvailable) Color(0xFFDBEAFE) else Color(0xFFFEE2E2),
                    fg = if (isAvailable) Color(0xFF1D4ED8) else Color(0xFFB91C1C)
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
                Text(
                    text = if (!isAvailable) "Provider Busy" else "Continue"
                )
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