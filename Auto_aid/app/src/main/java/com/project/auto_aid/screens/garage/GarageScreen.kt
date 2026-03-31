package com.project.auto_aid.screens.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.navigation.Routes

@Composable
fun GarageScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }

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
            text = "Garage Service",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Fast, reliable mechanical help when you need it.",
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
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(10.dp))
        }

        GarageServiceCard(
            icon = {
                Icon(Icons.Default.Build, contentDescription = null)
            },
            title = "Request Garage Help",
            subtitle = "Choose the closest available mechanic"
        ) {
            error = null

            if (pickedLat == 0.0 && pickedLng == 0.0) {
                error = "Please choose your service location first."
                return@GarageServiceCard
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
                    providerType = "garage",
                    lat = pickedLat,
                    lng = pickedLng,
                    pickedLabel = pickedLabel
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GarageServiceCard(
            icon = {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            },
            title = "Active Requests",
            subtitle = "View ongoing garage requests"
        ) {
            error = null

            val requestId = tokenStore.getLastGarageRequestId()

            if (!requestId.isNullOrBlank()) {
                navController.navigate(
                    Routes.GarageActiveScreen.createRoute(requestId)
                )
            } else {
                error = "No active garage request found."
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GarageServiceCard(
            icon = {
                Icon(Icons.Default.History, contentDescription = null)
            },
            title = "Garage History",
            subtitle = "View past garage requests"
        ) {
            navController.navigate(Routes.GarageHistoryScreen.route)
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
private fun GarageServiceCard(
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
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}