package com.project.auto_aid.provider.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.provider.ProviderViewModel

private object ProviderChatStatuses {
    const val ASSIGNED = "assigned"
    const val DRIVER_ASSIGNED = "driver_assigned"
    const val MECHANIC_ASSIGNED = "mechanic_assigned"
    const val VENDOR_ASSIGNED = "vendor_assigned"
    const val ARRIVED = "arrived"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
    const val PENDING = "pending"
    const val REQUEST_SENT = "request_sent"
}

private fun RequestDto.requestKey(): String {
    return _id ?: id ?: requestId ?: ""
}

private fun RequestDto.displayUserName(): String {
    return userName?.takeIf { it.isNotBlank() } ?: "Customer"
}

private fun RequestDto.displayProblem(): String {
    return when {
        !problem.isNullOrBlank() -> problem ?: ""
        !note.isNullOrBlank() -> note ?: ""
        !vehicleInfo.isNullOrBlank() -> vehicleInfo ?: ""
        !service.isNullOrBlank() -> service ?: ""
        !providerType.isNullOrBlank() -> providerType ?: ""
        else -> "Service request"
    }
}

private fun RequestDto.normalizedStatus(): String {
    return status?.trim()?.lowercase().orEmpty()
}

private fun RequestDto.isChatVisible(): Boolean {
    return normalizedStatus() in setOf(
        ProviderChatStatuses.ASSIGNED,
        ProviderChatStatuses.DRIVER_ASSIGNED,
        ProviderChatStatuses.MECHANIC_ASSIGNED,
        ProviderChatStatuses.VENDOR_ASSIGNED,
        ProviderChatStatuses.ARRIVED,
        ProviderChatStatuses.IN_PROGRESS
    )
}

private fun RequestDto.statusLabel(): String {
    return when (normalizedStatus()) {
        ProviderChatStatuses.PENDING,
        ProviderChatStatuses.REQUEST_SENT -> "Pending"

        ProviderChatStatuses.ASSIGNED,
        ProviderChatStatuses.DRIVER_ASSIGNED,
        ProviderChatStatuses.MECHANIC_ASSIGNED,
        ProviderChatStatuses.VENDOR_ASSIGNED -> "Assigned"

        ProviderChatStatuses.ARRIVED -> "Arrived"
        ProviderChatStatuses.IN_PROGRESS -> "In Progress"
        ProviderChatStatuses.COMPLETED -> "Completed"
        ProviderChatStatuses.CANCELLED -> "Cancelled"
        else -> "Unknown"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderChatListScreen(
    providerViewModel: ProviderViewModel,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by providerViewModel.state.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        providerViewModel.refreshAll()
    }

    val allRequests: List<RequestDto> = remember(
        state.pendingJobs,
        state.ongoingJobs,
        state.completedJobs
    ) {
        state.pendingJobs + state.ongoingJobs + state.completedJobs
    }

    val chatRequests: List<RequestDto> = remember(allRequests, searchQuery) {
        val query = searchQuery.trim().lowercase()

        allRequests
            .filter { request: RequestDto ->
                request.isChatVisible()
            }
            .filter { request: RequestDto ->
                if (query.isBlank()) return@filter true

                request.displayUserName().lowercase().contains(query) ||
                        request.displayProblem().lowercase().contains(query) ||
                        request.requestKey().lowercase().contains(query) ||
                        request.statusLabel().lowercase().contains(query) ||
                        (request.userPhone ?: "").lowercase().contains(query)
            }
            .sortedByDescending { request: RequestDto ->
                request.createdAt ?: ""
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { value: String ->
                    searchQuery = value
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search chats"
                    )
                },
                label = {
                    Text("Search chats")
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                state.loading && allRequests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error.isNotBlank() && allRequests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                chatRequests.isEmpty() -> {
                    EmptyChatState(
                        message = if (searchQuery.isNotBlank()) {
                            "No chats match your search."
                        } else {
                            "No active chats yet."
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = chatRequests,
                            key = { request: RequestDto ->
                                request.requestKey().ifBlank {
                                    "${request.userName.orEmpty()}_${request.createdAt.orEmpty()}"
                                }
                            }
                        ) { request: RequestDto ->
                            ProviderChatListItem(
                                request = request,
                                onClick = {
                                    val requestId = request.requestKey()
                                    if (requestId.isNotBlank()) {
                                        onOpenChat(requestId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderChatListItem(
    request: RequestDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = request.displayUserName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = request.displayProblem(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Open chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Request ID: ${request.requestKey().ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = request.statusLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}