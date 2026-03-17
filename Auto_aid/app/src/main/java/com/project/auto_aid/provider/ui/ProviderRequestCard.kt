package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.auto_aid.provider.model.ProviderRequest

@Composable
fun ProviderRequestCard(
    request: ProviderRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {

            Text("Service: ${request.service.ifBlank { request.providerType }}")
            Spacer(Modifier.height(4.dp))
            Text("Problem: ${request.problem.ifBlank { "No details" }}")
            Spacer(Modifier.height(4.dp))
            Text("Status: ${request.status}")

            Spacer(Modifier.height(10.dp))

            when (request.status.lowercase()) {
                "pending" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onView,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View")
                        }

                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Accept")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Decline")
                    }
                }

                else -> {
                    Button(
                        onClick = onView,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Job")
                    }
                }
            }
        }
    }
}