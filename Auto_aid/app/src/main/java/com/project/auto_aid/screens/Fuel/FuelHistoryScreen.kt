package com.project.auto_aid.screens.fuel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun FuelHistoryScreen(navController: NavHostController) {

    val history = listOf(
        "Petrol • 10L • Completed",
        "Diesel • 20L • Cancelled"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        TextButton(onClick = { navController.popBackStack() }) {
            Text("← Back")
        }

        Text("Fuel History", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            Text("No fuel requests yet")
        } else {
            LazyColumn {
                items(history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Text(
                            item,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
