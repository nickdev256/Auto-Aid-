package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProviderStatsRow(pending: Int, active: Int, completed: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard("Pending", pending)
        StatCard("Active", active)
        StatCard("Completed", completed)
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge)
            Text(label)
        }
    }
}