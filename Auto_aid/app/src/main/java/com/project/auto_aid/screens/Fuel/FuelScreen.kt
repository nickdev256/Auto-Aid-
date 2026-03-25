package com.project.auto_aid.screens.fuel

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun FuelScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val scope = rememberCoroutineScope()

    var error by remember { mutableStateOf<String?>(null) }

    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        ?: navController.currentBackStackEntry?.savedStateHandle

    val pickedLocationLabelState =
        savedStateHandle?.getStateFlow("picked_location_label", "")?.collectAsState()
    val pickedLocationLatState =
        savedStateHandle?.getStateFlow("picked_location_lat", 0.0)?.collectAsState()
    val pickedLocationLngState =
        savedStateHandle?.getStateFlow("picked_location_lng", 0.0)?.collectAsState()

    val pickedLabel = pickedLocationLabelState?.value.orEmpty()
    val pickedLat = pickedLocationLatState?.value ?: 0.0
    val pickedLng = pickedLocationLngState?.value ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Fuel Delivery",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Fast, reliable fuel delivery when you need it.",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (pickedLabel.isNotBlank()) {
            AssistChip(
                onClick = {},
                label = { Text("Pickup: $pickedLabel") }
            )
        } else {
            AssistChip(
                onClick = {
                    navController.navigate(Routes.LocationPicker.createRoute())
                },
                label = { Text("Choose service location") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        FuelServiceCard(
            icon = {
                Icon(
                    Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Request Fuel",
            subtitle = "Choose the closest available fuel provider"
        ) {
            error = null

            if (pickedLat == 0.0 && pickedLng == 0.0) {
                error = "Please choose your service location first."
                return@FuelServiceCard
            }

            navController.currentBackStackEntry?.savedStateHandle?.set(
                "picked_location_label",
                pickedLabel
            )
            navController.currentBackStackEntry?.savedStateHandle?.set(
                "picked_location_lat",
                pickedLat
            )
            navController.currentBackStackEntry?.savedStateHandle?.set(
                "picked_location_lng",
                pickedLng
            )

            navController.navigate(
                Routes.ProviderSelection.createRoute(
                    providerType = "fuel",
                    lat = pickedLat,
                    lng = pickedLng,
                    pickedLabel = pickedLabel,
                    vehicleInfo = "",
                    problem = "",
                    note = "",
                    urgency = "normal",
                    towType = ""
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FuelServiceCard(
            icon = {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF16A34A)
                )
            },
            title = "Active Request",
            subtitle = "Track your ongoing fuel order"
        ) {
            scope.launch {
                val rid = tokenStore.getLastFuelRequestId()
                if (rid.isNullOrBlank()) {
                    error = "No active fuel request yet. Please request fuel first."
                    return@launch
                }
                error = null
                navController.navigate(Routes.FuelActiveScreen.createRoute(rid))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FuelServiceCard(
            icon = {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF0284C7)
                )
            },
            title = "Fuel History",
            subtitle = "View past fuel requests"
        ) {
            error = null
            navController.navigate(Routes.FuelHistoryScreen.route)
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun FuelServiceCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}