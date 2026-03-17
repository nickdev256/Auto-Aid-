package com.project.auto_aid.screens.towing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

data class ProviderLite(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val providerType: String = "",
    val rating: Double = 0.0,
    val profileImageUrl: String = "",
    val isOnline: Boolean = false
)

private fun ProviderLiteDto.toUi(): ProviderLite {
    return ProviderLite(
        id = id ?: "",
        name = name ?: "Unknown Provider",
        phone = phone ?: "No phone",
        providerType = businessType ?: "towing",
        rating = rating ?: 0.0,
        profileImageUrl = profileImageUrl ?: "",
        isOnline = isOnline ?: false
    )
}

@Composable
fun AvailableProvidersScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }

    var loading by remember { mutableStateOf(true) }
    var providers by remember { mutableStateOf<List<ProviderLite>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null

        runCatching {
            val response = api.getAvailableProviders(
                providerType = "towing",
                lat = null,
                lng = null,
                onlineOnly = true
            )

            if (!response.isSuccessful) {
                throw Exception("Failed to load towing providers: HTTP ${response.code()}")
            }

            response.body()
                .orEmpty()
                .map { it.toUi() }
                .filter { it.providerType.equals("towing", ignoreCase = true) || it.providerType.isBlank() }
        }.onSuccess { list ->
            providers = list
        }.onFailure { e ->
            providers = emptyList()
            error = e.message ?: "Failed to load providers"
        }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Available Towing Providers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a provider to send your request.",
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
                    Text(text = error ?: "Error")
                }
            }

            providers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No towing providers online right now.")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = providers,
                        key = { it.id }
                    ) { provider ->
                        ProviderCard(
                            provider = provider,
                            onClick = {
                                if (provider.id.isNotBlank()) {
                                    navController.navigate(
                                        Routes.TowingRequestScreen.createRoute(providerId = provider.id)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = provider.name.ifBlank { "Unknown Provider" },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "⭐ ${String.format(Locale.US, "%.1f", provider.rating)}"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Phone: ${provider.phone.ifBlank { "No phone" }}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (provider.isOnline) "Online • Tap to request" else "Offline",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}