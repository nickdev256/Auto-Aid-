package com.project.auto_aid.screens.ambulance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ProviderLiteDto
import com.project.auto_aid.navigation.Routes
import java.util.Locale

private fun ProviderLiteDto.toAmbulanceUi(): AmbulanceProvider {
    return AmbulanceProvider(
        id = id ?: "",
        name = name ?: "Unknown Ambulance Provider",
        phone = phone ?: "No phone",
        providerType = businessType ?: "ambulance",
        rating = rating ?: 0.0,
        isOnline = isOnline ?: false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbulanceProvidersScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }

    val lat = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<Double>("user_lat")

    val lng = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<Double>("user_lng")

    var loading by remember { mutableStateOf(true) }
    var providers by remember { mutableStateOf<List<AmbulanceProvider>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lat, lng) {
        loading = true
        error = null

        runCatching {
            val response = api.getAvailableProviders(
                providerType = "ambulance",
                lat = lat,
                lng = lng,
                onlineOnly = true
            )

            if (!response.isSuccessful) {
                throw Exception("Failed to load ambulance providers: HTTP ${response.code()}")
            }

            response.body()
                .orEmpty()
                .map { it.toAmbulanceUi() }
                .filter {
                    it.providerType.equals("ambulance", ignoreCase = true) ||
                            it.providerType.isBlank()
                }
        }.onSuccess { list ->
            providers = list
        }.onFailure { e ->
            providers = emptyList()
            error = e.message ?: "Failed to load ambulance providers"
        }

        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Ambulance Providers") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = if (lat != null && lng != null) {
                    "Showing ambulance providers near your location"
                } else {
                    "Location not detected — showing general ambulance providers"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(error ?: "Error")
                    }
                }

                providers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No ambulance providers available nearby.")
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = providers,
                            key = { it.id }
                        ) { provider ->
                            AmbulanceProviderCard(
                                provider = provider,
                                onClick = {
                                    if (provider.id.isNotBlank()) {
                                        navController.navigate(
                                            Routes.AmbulanceRequestScreen.createRoute(provider.id)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbulanceProviderCard(
    provider: AmbulanceProvider,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = "Ambulance Provider"
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name.ifBlank { "Unknown Ambulance Provider" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Phone: ${provider.phone.ifBlank { "No phone" }}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "⭐ ${String.format(Locale.US, "%.1f", provider.rating)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = if (provider.isOnline) "Online • Tap to request" else "Offline",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}