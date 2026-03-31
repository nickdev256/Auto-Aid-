package com.project.auto_aid.screens.towing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class ProviderLite(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val providerType: String = "",
    val rating: Double = 0.0,
    val isOnline: Boolean = false
)

private fun ProviderLiteDto.toUi(): ProviderLite {
    return ProviderLite(
        id = id ?: "",
        name = name ?: "Unknown Provider",
        phone = phone ?: "No phone",
        providerType = businessType ?: "towing",
        rating = rating ?: 0.0,
        isOnline = isOnline ?: false
    )
}

@Composable
fun AvailableProvidersScreen(navController: NavHostController) {

    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }

    // 🔥 GET LOCATION FROM AI (passed through navigation)
    val lat = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<Double>("user_lat")

    val lng = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<Double>("user_lng")

    var loading by remember { mutableStateOf(true) }
    var providers by remember { mutableStateOf<List<ProviderLite>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lat, lng) {
        loading = true
        error = null

        runCatching {
            val response = api.getAvailableProviders(
                providerType = "towing",
                lat = lat,
                lng = lng,
                onlineOnly = true
            )

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }

            response.body()
                .orEmpty()
                .map { it.toUi() }
                .filter {
                    it.providerType.equals("towing", true) || it.providerType.isBlank()
                }

        }.onSuccess {
            providers = it
        }.onFailure {
            providers = emptyList()
            error = it.message ?: "Failed to load providers"
        }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Nearby Towing Providers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (lat != null && lng != null)
                "Showing providers near your location"
            else
                "Location not detected — showing general providers",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(error ?: "Error")
                }
            }

            providers.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No towing providers available nearby.")
                }
            }

            else -> {
                LazyColumn {
                    items(providers, key = { it.id }) { provider ->
                        ProviderCard(
                            provider = provider,
                            onClick = {
                                if (provider.id.isNotBlank()) {
                                    navController.navigate(
                                        Routes.TowingRequestScreen.createRoute(provider.id)
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

@Composable
private fun ProviderCard(
    provider: ProviderLite,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(provider.name, fontWeight = FontWeight.Bold)
                Text("⭐ ${String.format(Locale.US, "%.1f", provider.rating)}")
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Phone: ${provider.phone}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (provider.isOnline) "Online • Tap to request" else "Offline",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}