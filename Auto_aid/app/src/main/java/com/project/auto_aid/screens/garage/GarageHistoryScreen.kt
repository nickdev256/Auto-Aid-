package com.project.auto_aid.screens.garage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun GarageHistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var requests by remember { mutableStateOf<List<RequestDto>>(emptyList()) }

    fun loadHistory() {
        scope.launch {
            loading = true
            error = null

            try {
                val res = api.getMyRequests()
                if (!res.isSuccessful) {
                    throw Exception("Failed to load requests (HTTP ${res.code()})")
                }

                val all = res.body().orEmpty()

                requests = all.filter { request ->
                    val isGarage = normalizeGarageKey(request.service ?: request.providerType) == "garage"
                    val status = request.status?.trim()?.lowercase().orEmpty()
                    isGarage && status in listOf("completed", "cancelled")
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load garage history"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Garage History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        when {
            loading -> {
                CircularProgressIndicator()
            }

            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { loadHistory() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }

            requests.isEmpty() -> {
                Text(
                    "No garage requests yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(requests, key = { it.resolvedId() }) { request ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(
                                        Routes.RequestDetails.createRoute(request.resolvedId())
                                    )
                                },
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    text = request.problem ?: "Garage request",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Status: ${request.status ?: "-"}")
                                Text("Vehicle: ${request.vehicleInfo ?: "-"}")
                                Text("Provider: ${request.assignedProviderName ?: "-"}")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

private fun normalizeGarageKey(value: String?): String {
    return when (value?.trim()?.lowercase()) {
        "garage", "garage repair" -> "garage"
        else -> ""
    }
}