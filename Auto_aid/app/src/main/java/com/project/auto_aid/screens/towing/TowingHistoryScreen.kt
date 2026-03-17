package com.project.auto_aid.screens.towing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun TowingHistoryScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Towing History", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Text("No towing orders yet", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back") }
    }
}