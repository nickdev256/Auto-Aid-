package com.project.auto_aid.provider.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ProviderDto
import com.project.auto_aid.data.network.dto.UpdateAvailabilityBody
import com.project.auto_aid.data.network.dto.UpdateProfileRequest
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.provider.ProviderViewModel
import com.project.auto_aid.provider.model.Provider
import kotlinx.coroutines.launch

@Composable
fun ProviderDashboardScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val scope = rememberCoroutineScope()

    val vm: ProviderViewModel = viewModel()
    val api = remember { RetrofitClient.create(tokenStore) }

    var provider by remember { mutableStateOf<Provider?>(null) }
    var providerId by remember { mutableStateOf("") }
    var providerType by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(1) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var startedListening by remember { mutableStateOf(false) }
    var isDark by remember { mutableStateOf(false) }
    var isOnlineUi by remember { mutableStateOf(true) }
    var availabilityUpdating by remember { mutableStateOf(false) }

    fun logout() {
        scope.launch {
            runCatching { tokenStore.saveToken("") }
            navController.navigate(Routes.LoginScreen.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    suspend fun loadProviderProfile() {
        loading = true
        errorMsg = null

        try {
            val res = api.getProviderMe()

            if (!res.isSuccessful) {
                val err = runCatching { res.errorBody()?.string() }.getOrNull()
                errorMsg = when (res.code()) {
                    401 -> "Session expired. Please login again."
                    403 -> "Access denied."
                    else -> err ?: "Failed to load profile (${res.code()})"
                }
                provider = null

                if (res.code() == 401) {
                    logout()
                }
                return
            }

            val u: ProviderDto? = res.body()
            if (u == null) {
                errorMsg = "Empty profile response"
                provider = null
                return
            }

            if ((u.role ?: "").lowercase() != "provider") {
                errorMsg = "Not a provider account"
                provider = null
                return
            }

            providerId = u.resolvedId()
            providerType = (u.providerType ?: u.businessType ?: "").trim()
            isOnlineUi = u.isOnline ?: true

            provider = Provider(
                id = providerId,
                name = u.name ?: "",
                phone = u.phone ?: "",
                providerType = providerType,
                rating = u.rating ?: 0.0,
                profileImageUrl = u.profileImageUrl ?: u.logoUrl ?: ""
            )
        } catch (e: Exception) {
            errorMsg = e.message ?: "Failed to load provider profile"
            provider = null
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProviderProfile()
    }

    LaunchedEffect(providerType, providerId) {
        if (!startedListening && providerType.isNotBlank() && providerId.isNotBlank()) {
            startedListening = true
            vm.start(providerType, providerId)
        }
    }

    val uploadImage = rememberProfileImagePicker { newUrl ->
        provider = provider?.copy(profileImageUrl = newUrl)

        scope.launch {
            runCatching {
                api.updateProviderProfile(
                    UpdateProfileRequest(
                        name = provider?.name ?: "",
                        phone = provider?.phone ?: "",
                        profileImageUrl = newUrl
                    )
                )
            }
        }
    }

    if (loading) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    isDark = isDark,
                    onToggleDark = { isDark = !isDark },
                    onBell = { navController.navigate(Routes.ProviderNotifications.route) },
                    onLogout = { logout() }
                )
            },
            bottomBar = {
                ProviderBottomNav(
                    navController = navController,
                    selectedRoute = Routes.ProviderDashboard.route
                )
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (provider == null) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    isDark = isDark,
                    onToggleDark = { isDark = !isDark },
                    onBell = { navController.navigate(Routes.ProviderNotifications.route) },
                    onLogout = { logout() }
                )
            },
            bottomBar = {
                ProviderBottomNav(
                    navController = navController,
                    selectedRoute = Routes.ProviderDashboard.route
                )
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMsg ?: "Provider profile not found",
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                startedListening = false
                                scope.launch { loadProviderProfile() }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        return
    }

    val safeProvider = provider!!

    Scaffold(
        topBar = {
            DashboardTopBar(
                isDark = isDark,
                onToggleDark = { isDark = !isDark },
                onBell = { navController.navigate(Routes.ProviderNotifications.route) },
                onLogout = { logout() }
            )
        },
        bottomBar = {
            ProviderBottomNav(
                navController = navController,
                selectedRoute = Routes.ProviderDashboard.route
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ProviderHeaderCard(
                    provider = safeProvider,
                    isOnline = isOnlineUi,
                    isAvailabilityUpdating = availabilityUpdating,
                    onOnlineChange = { isOnline ->
                        if (availabilityUpdating) return@ProviderHeaderCard

                        val previous = isOnlineUi
                        isOnlineUi = isOnline

                        scope.launch {
                            availabilityUpdating = true

                            val result = runCatching {
                                api.updateProviderAvailability(
                                    UpdateAvailabilityBody(
                                        isAvailable = isOnline
                                    )
                                )
                            }

                            result.onFailure {
                                isOnlineUi = previous
                                errorMsg = it.message ?: "Failed to update availability"
                            }

                            result.onSuccess { res ->
                                if (!res.isSuccessful) {
                                    isOnlineUi = previous
                                    errorMsg = runCatching {
                                        res.errorBody()?.string()
                                    }.getOrNull() ?: "Failed to update availability (${res.code()})"
                                }
                            }

                            availabilityUpdating = false
                        }
                    },
                    onEditProfile = {
                        navController.navigate(Routes.EditProviderProfile.route)
                    },
                    onChangeProfileImage = {
                        uploadImage()
                    }
                )
            }

            item {
                StatsSection(
                    pending = vm.pending.size,
                    active = vm.ongoing.size,
                    completed = vm.completed.size
                )
            }

            item {
                QuickActionsSection(
                    onWallet = {
                        navController.navigate(Routes.ProviderWallet.route) {
                            launchSingleTop = true
                        }
                    },
                    onPayoutInfo = {
                        navController.navigate(Routes.ProviderPayoutInfo.route) {
                            launchSingleTop = true
                        }
                    },
                    onPayoutRequests = {
                        navController.navigate(Routes.ProviderPayoutRequests.route) {
                            launchSingleTop = true
                        }
                    },
                    onMap = {
                        navController.navigate(Routes.ProviderMapHome.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            item {
                DashboardTabs(selected = tab, onSelect = { tab = it })
            }

            when (tab) {
                0 -> {
                    if (vm.pending.isEmpty()) {
                        item {
                            EmptyJobsState(
                                title = "No pending requests",
                                subtitle = "New nearby requests will appear here."
                            )
                        }
                    } else {
                        items(vm.pending, key = { it.id }) { request ->
                            ProviderRequestCard(
                                request = request,
                                onView = {
                                    navController.navigate(
                                        Routes.ProviderRequestDetails.createRoute(request.id)
                                    )
                                },
                                onAccept = {
                                    scope.launch {
                                        vm.accept(request.id)
                                        navController.navigate(
                                            Routes.ProviderActiveJob.createRoute(request.id)
                                        )
                                    }
                                },
                                onDecline = {
                                    scope.launch { vm.decline(request.id) }
                                }
                            )
                        }
                    }
                }

                1 -> {
                    if (vm.ongoing.isEmpty()) {
                        item {
                            EmptyJobsState(
                                title = "No active jobs",
                                subtitle = "When you accept a job, it will appear here."
                            )
                        }
                    } else {
                        items(vm.ongoing, key = { it.id }) { request ->
                            ProviderRequestCard(
                                request = request,
                                onAccept = {},
                                onDecline = {},
                                onView = {
                                    navController.navigate(
                                        Routes.ProviderActiveJob.createRoute(request.id)
                                    )
                                }
                            )
                        }
                    }
                }

                2 -> {
                    if (vm.completed.isEmpty()) {
                        item {
                            EmptyJobsState(
                                title = "No history yet",
                                subtitle = "Completed jobs will appear here."
                            )
                        }
                    } else {
                        items(vm.completed, key = { it.id }) { request ->
                            ProviderRequestCard(
                                request = request,
                                onAccept = {},
                                onDecline = {},
                                onView = {
                                    navController.navigate(
                                        Routes.ProviderRequestDetails.createRoute(request.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isDark: Boolean,
    onToggleDark: () -> Unit,
    onBell: () -> Unit,
    onLogout: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("Provider Dashboard", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = onBell) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "Notifications"
                )
            }

            IconButton(onClick = onToggleDark) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = "Logout"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    )
}

@Composable
private fun ProviderHeaderCard(
    provider: Provider,
    isOnline: Boolean,
    isAvailabilityUpdating: Boolean,
    onOnlineChange: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onChangeProfileImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onChangeProfileImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name.ifBlank { "Provider" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = provider.providerType
                            .ifBlank { "provider" }
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = provider.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOnline) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAvailabilityUpdating) "Updating..." else "Availability",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isOnline,
                        onCheckedChange = onOnlineChange,
                        enabled = !isAvailabilityUpdating
                    )
                }

                OutlinedButton(onClick = onEditProfile) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun StatsSection(
    pending: Int,
    active: Int,
    completed: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Pending",
            value = pending.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Active",
            value = active.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Done",
            value = completed.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onWallet: () -> Unit,
    onPayoutInfo: () -> Unit,
    onPayoutRequests: () -> Unit,
    onMap: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                title = "Wallet",
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onWallet,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Payout Info",
                icon = Icons.Outlined.Payments,
                onClick = onPayoutInfo,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                title = "Requests",
                icon = Icons.Outlined.RequestQuote,
                onClick = onPayoutRequests,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Map",
                icon = Icons.Outlined.Map,
                onClick = onMap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1.55f)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DashboardTabs(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selected,
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf("Requests", "Active", "History").forEachIndexed { index, title ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyJobsState(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProviderBottomNav(
    navController: NavHostController,
    selectedRoute: String
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = selectedRoute == Routes.ProviderDashboard.route,
            onClick = {
                navController.navigate(Routes.ProviderDashboard.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Routes.ProviderDashboard.route) { saveState = true }
                }
            },
            icon = {
                Icon(Icons.Outlined.Dashboard, contentDescription = "Dashboard")
            },
            label = { Text("Dashboard") }
        )

        NavigationBarItem(
            selected = selectedRoute == Routes.ProviderMapHome.route,
            onClick = {
                navController.navigate(Routes.ProviderMapHome.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Routes.ProviderDashboard.route) { saveState = true }
                }
            },
            icon = {
                Icon(Icons.Outlined.Map, contentDescription = "Map")
            },
            label = { Text("Map") }
        )

        NavigationBarItem(
            selected = selectedRoute == Routes.ProviderChatList.route,
            onClick = {
                navController.navigate(Routes.ProviderChatList.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Routes.ProviderDashboard.route) { saveState = true }
                }
            },
            icon = {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chat")
            },
            label = { Text("Chat") }
        )

        NavigationBarItem(
            selected = selectedRoute == Routes.ProviderProfile.route,
            onClick = {
                navController.navigate(Routes.ProviderProfile.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Routes.ProviderDashboard.route) { saveState = true }
                }
            },
            icon = {
                Icon(Icons.Outlined.PersonOutline, contentDescription = "Profile")
            },
            label = { Text("Profile") }
        )
    }
}