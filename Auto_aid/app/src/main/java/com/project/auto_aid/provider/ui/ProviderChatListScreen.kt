package com.project.auto_aid.provider.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.provider.ProviderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderChatListScreen(navController: NavHostController) {
    val vm: ProviderViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        if (vm.ongoing.isEmpty() && vm.pending.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No active chats yet. Accept a job to start chatting.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // show ongoing first, then pending (optional)
                item {
                    Text("Active Jobs", fontWeight = FontWeight.Bold)
                }

                items(vm.ongoing, key = { it.id }) { r ->
                    ChatListCard(
                        title = "Request ${r.id.takeLast(6)}",
                        subtitle = "${r.service.uppercase()} • ${r.problem.ifBlank { "Job" }}",
                        onOpen = { navController.navigate(Routes.ProviderChatThread.createRoute(r.id)) }
                    )
                }

                if (vm.pending.isNotEmpty()) {
                    item { Spacer(Modifier.height(6.dp)) }
                    item { Text("Pending (optional)", fontWeight = FontWeight.Bold) }
                    items(vm.pending, key = { it.id }) { r ->
                        ChatListCard(
                            title = "Request ${r.id.takeLast(6)}",
                            subtitle = "Pending • ${r.problem.ifBlank { "Request" }}",
                            onOpen = { navController.navigate(Routes.ProviderChatThread.createRoute(r.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListCard(
    title: String,
    subtitle: String,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}