package com.project.auto_aid.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.viewmodel.AiAssistViewModel
import com.project.auto_aid.viewmodel.AiAssistViewModelFactory
import com.project.auto_aid.viewmodel.ChatMessage
import com.project.auto_aid.viewmodel.MessageType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val factory = remember { AiAssistViewModelFactory(tokenStore) }
    val vm: AiAssistViewModel = viewModel(factory = factory)

    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    val currentStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val pickedLocationLabel by (
            currentStateHandle?.getStateFlow("picked_location_label", "")
                ?.collectAsState()
                ?: remember { mutableStateOf("") }
            )

    val pickedLocationLat by (
            currentStateHandle?.getStateFlow("picked_location_lat", 0.0)
                ?.collectAsState()
                ?: remember { mutableStateOf(0.0) }
            )

    val pickedLocationLng by (
            currentStateHandle?.getStateFlow("picked_location_lng", 0.0)
                ?.collectAsState()
                ?: remember { mutableStateOf(0.0) }
            )

    val pendingProviderSearch by (
            currentStateHandle?.getStateFlow("pending_provider_search_service", "")
                ?.collectAsState()
                ?: remember { mutableStateOf("") }
            )

    val latestState by rememberUpdatedState(state)

    LaunchedEffect(pickedLocationLabel, pickedLocationLat, pickedLocationLng) {
        if ((pickedLocationLat != 0.0 || pickedLocationLng != 0.0) && pickedLocationLabel.isNotBlank()) {
            vm.setLocation(
                lat = pickedLocationLat,
                lng = pickedLocationLng,
                address = pickedLocationLabel
            )
        }
    }

    LaunchedEffect(state.escalationResult, pickedLocationLat, pickedLocationLng, pickedLocationLabel) {
        val escalation = state.escalationResult ?: return@LaunchedEffect
        if (!escalation.success) return@LaunchedEffect

        val payload = escalation.providerPayload
        val service = payload.recommendedService.trim().lowercase()

        currentStateHandle?.set(
            "ai_problem",
            latestState.analysisResult?.analysis?.issue.orEmpty()
        )
        currentStateHandle?.set(
            "ai_urgency",
            latestState.analysisResult?.analysis?.risk.orEmpty()
        )
        currentStateHandle?.set(
            "ai_note",
            latestState.analysisResult?.analysis?.reason
                ?.ifBlank { latestState.analysisResult?.analysis?.action.orEmpty() }
                .orEmpty()
        )
        currentStateHandle?.set(
            "ai_vehicle_info",
            buildString {
                if (latestState.vehicleType.isNotBlank()) {
                    append("Vehicle: ${latestState.vehicleType}")
                }
                if (latestState.fuelType.isNotBlank()) {
                    if (isNotBlank()) append(" • ")
                    append("Fuel: ${latestState.fuelType}")
                }
            }
        )
        currentStateHandle?.set(
            "ai_summary",
            payload.aiSummary
        )

        val hasLocation = latestState.lat != null &&
                latestState.lng != null &&
                (latestState.lat != 0.0 || latestState.lng != 0.0)

        if (!hasLocation) {
            currentStateHandle?.set("pending_provider_search_service", service)
            vm.clearEscalationResult()
            navController.navigate(Routes.LocationPicker.createRoute())
        } else {
            val safeLabel = URLEncoder.encode(
                latestState.address.ifBlank { "Selected location" },
                StandardCharsets.UTF_8.toString()
            )

            vm.clearEscalationResult()
            navController.navigate(
                Routes.ProviderSelection.createRoute(
                    providerType = service,
                    lat = latestState.lat ?: 0.0,
                    lng = latestState.lng ?: 0.0,
                    pickedLabel = safeLabel
                )
            )
        }
    }

    LaunchedEffect(pendingProviderSearch, pickedLocationLat, pickedLocationLng, pickedLocationLabel) {
        if (
            pendingProviderSearch.isNotBlank() &&
            (pickedLocationLat != 0.0 || pickedLocationLng != 0.0)
        ) {
            val safeLabel = URLEncoder.encode(
                pickedLocationLabel.ifBlank { "Selected location" },
                StandardCharsets.UTF_8.toString()
            )

            currentStateHandle?.set("pending_provider_search_service", "")

            navController.navigate(
                Routes.ProviderSelection.createRoute(
                    providerType = pendingProviderSearch,
                    lat = pickedLocationLat,
                    lng = pickedLocationLng,
                    pickedLabel = safeLabel
                )
            )
        }
    }

    LaunchedEffect(state.chatMessages.size, state.isLoading) {
        val itemCount = state.chatMessages.size + if (state.isLoading) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Auto Aid AI")
                        Text(
                            text = "Smart roadside assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = vm::resetChat) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset chat"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (state.errorMessage != null) {
                        ErrorMessageBar(
                            message = state.errorMessage ?: "",
                            onDismiss = vm::clearError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ChatInputBar(
                        value = state.inputText,
                        onValueChange = vm::updateInputText,
                        onSend = vm::sendMessage,
                        isLoading = state.isLoading
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SafetyFlagsSection(
                    injury = state.injury,
                    fire = state.fire,
                    fuelLeak = state.fuelLeak,
                    unsafeLocation = state.unsafeLocation,
                    onInjuryChanged = vm::setInjury,
                    onFireChanged = vm::setFire,
                    onFuelLeakChanged = vm::setFuelLeak,
                    onUnsafeLocationChanged = vm::setUnsafeLocation
                )
            }

            item {
                VehicleQuickInfoSection(
                    vehicleType = state.vehicleType,
                    fuelType = state.fuelType,
                    address = state.address,
                    onPickLocation = {
                        navController.navigate(Routes.LocationPicker.createRoute())
                    }
                )
            }

            if (
                state.analysisResult != null &&
                (state.selfSolveAvailable || state.selectableProviderTypes.isNotEmpty())
            ) {
                item {
                    AiDecisionCard(
                        issue = state.analysisResult?.analysis?.issue.orEmpty(),
                        risk = state.analysisResult?.analysis?.risk.orEmpty(),
                        reason = state.analysisResult?.analysis?.reason.orEmpty(),
                        confidence = state.analysisResult?.analysis?.confidence ?: 0.0,
                        selfSolveAvailable = state.selfSolveAvailable,
                        selfSolveSteps = state.selfSolveSteps,
                        primaryProvider = state.primaryProviderType,
                        selectedProvider = state.selectedProviderType,
                        options = state.selectableProviderTypes,
                        onChooseProvider = vm::chooseProviderType,
                        onContinueProvider = {
                            vm.triggerProviderSearch(state.selectedProviderType)
                        }
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                AiIntroCard()
            }

            itemsIndexed(
                items = state.chatMessages,
                key = { _, item -> item.id }
            ) { _, message ->
                when (message.type) {
                    MessageType.NORMAL -> ChatBubble(message = message)
                    MessageType.SYSTEM -> InlineSystemMessage(
                        message = message,
                        onSearchProviders = {
                            vm.triggerProviderSearch(state.selectedProviderType)
                        }
                    )
                }
            }

            if (state.isLoading) {
                item {
                    TypingBubble()
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AiIntroCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Describe the problem naturally",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "I can guide self-fix steps, rank the right service, and still let you choose the provider type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VehicleQuickInfoSection(
    vehicleType: String,
    fuelType: String,
    address: String,
    onPickLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Current context",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (vehicleType.isNotBlank()) "Vehicle: $vehicleType" else "Vehicle: not set",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (fuelType.isNotBlank()) "Fuel: $fuelType" else "Fuel: not set",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (address.isNotBlank()) "Location: $address" else "Location: not selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onPickLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (address.isNotBlank()) "Update location"
                    else "Choose location"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiDecisionCard(
    issue: String,
    risk: String,
    reason: String,
    confidence: Double,
    selfSolveAvailable: Boolean,
    selfSolveSteps: List<String>,
    primaryProvider: String?,
    selectedProvider: String?,
    options: List<String>,
    onChooseProvider: (String) -> Unit,
    onContinueProvider: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "AI triage result",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (issue.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detected issue: $issue",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (risk.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Risk: $risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (confidence > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confidence: ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selfSolveAvailable && selfSolveSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Try these steps first",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                selfSolveSteps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            if (options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Choose provider type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (!primaryProvider.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI recommends: $primaryProvider",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        val chosen = selectedProvider.equals(option, ignoreCase = true)

                        Button(
                            onClick = { onChooseProvider(option) }
                        ) {
                            Text(
                                if (chosen) "Selected: $option" else option
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onContinueProvider,
                    enabled = !selectedProvider.isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with selected provider")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.isUser) {
            Card(
                modifier = Modifier.fillMaxWidth(0.82f),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 6.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Card(
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineSystemMessage(
    message: ChatMessage,
    onSearchProviders: () -> Unit
) {
    val showSearchButton = message.text.equals(
        "Search for available service providers",
        ignoreCase = true
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentWidth(Alignment.Start),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (showSearchButton) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSearchProviders,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search for available service providers")
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Card(
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thinking...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Message Auto Aid AI...")
            },
            minLines = 1,
            maxLines = 6,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = !isLoading && value.isNotBlank(),
            modifier = Modifier
                .background(
                    color = if (!isLoading && value.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                )
                .size(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorMessageBar(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SafetyFlagsSection(
    injury: Boolean,
    fire: Boolean,
    fuelLeak: Boolean,
    unsafeLocation: Boolean,
    onInjuryChanged: (Boolean) -> Unit,
    onFireChanged: (Boolean) -> Unit,
    onFuelLeakChanged: (Boolean) -> Unit,
    onUnsafeLocationChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "Safety details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Turn on only what applies.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SafetyChip(
                    label = "Injury",
                    checked = injury,
                    onCheckedChange = onInjuryChanged
                )
                SafetyChip(
                    label = "Fire or smoke",
                    checked = fire,
                    onCheckedChange = onFireChanged
                )
                SafetyChip(
                    label = "Fuel leakage",
                    checked = fuelLeak,
                    onCheckedChange = onFuelLeakChanged
                )
                SafetyChip(
                    label = "Unsafe location",
                    checked = unsafeLocation,
                    onCheckedChange = onUnsafeLocationChanged
                )
            }
        }
    }
}

@Composable
private fun SafetyChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (checked) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}