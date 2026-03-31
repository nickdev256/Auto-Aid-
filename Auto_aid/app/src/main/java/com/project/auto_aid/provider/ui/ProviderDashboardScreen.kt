package com.project.auto_aid.provider.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ProviderDto
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.UpdateAvailabilityBody
import com.project.auto_aid.data.network.dto.UpdateProfileRequest
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.provider.ProviderViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private object StandardDashboardColors {
    val Background = Color(0xFFF6F8FC)
    val Surface = Color.White
    val SurfaceSoft = Color(0xFFF1F5F9)

    val Primary = Color(0xFF1565C0)
    val PrimaryLight = Color(0xFFE3F2FD)
    val PrimaryDark = Color(0xFF0D47A1)

    val Success = Color(0xFF16A34A)
    val SuccessSoft = Color(0xFFDCFCE7)

    val Warning = Color(0xFFF59E0B)
    val WarningSoft = Color(0xFFFEF3C7)

    val Danger = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFEE2E2)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextMuted = Color(0xFF94A3B8)
    val Border = Color(0xFFE2E8F0)

    val HeaderGradient = Brush.horizontalGradient(
        listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
    )
}

data class DashboardProviderUi(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val providerType: String = "provider",
    val rating: Double = 0.0,
    val profileImageUrl: String = ""
)

@Composable
fun ProviderDashboardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()
    val vm: ProviderViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var provider by remember { mutableStateOf<DashboardProviderUi?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) }

    var isOnlineUi by remember { mutableStateOf(true) }
    var availabilityUpdating by remember { mutableStateOf(false) }

    var totalEarned by remember { mutableDoubleStateOf(0.0) }

    fun logout() = scope.launch {
        runCatching { tokenStore.saveToken("") }
        navController.navigate(Routes.LoginScreen.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    suspend fun loadWalletSummary() {
        totalEarned = 0.0
    }

    suspend fun loadProviderProfile() {
        loading = true
        errorMessage = null

        try {
            val res = api.getProviderMe()
            if (!res.isSuccessful) {
                val err = runCatching { res.errorBody()?.string() }.getOrNull()
                errorMessage = when (res.code()) {
                    401 -> "Session expired. Please login again."
                    403 -> "Access denied."
                    else -> err ?: "Failed to load profile (${res.code()})"
                }
                provider = null
                if (res.code() == 401) logout()
                return
            }

            val dto: ProviderDto = res.body() ?: run {
                errorMessage = "Empty profile response."
                provider = null
                return
            }

            if ((dto.role ?: "").lowercase() != "provider") {
                errorMessage = "This account is not a provider account."
                provider = null
                return
            }

            isOnlineUi = dto.isOnline ?: state.isOnline

            provider = DashboardProviderUi(
                id = dto.resolvedId(),
                name = dto.businessName?.takeIf { it.isNotBlank() } ?: dto.name.orEmpty(),
                phone = dto.phone.orEmpty(),
                providerType = dto.providerType ?: dto.businessType ?: "provider",
                rating = dto.rating ?: 0.0,
                profileImageUrl = dto.profileImageUrl ?: dto.logoUrl ?: ""
            )

            loadWalletSummary()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load dashboard."
            provider = null
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProviderProfile()
        vm.refreshAll()
    }

    LaunchedEffect(state.isOnline) {
        isOnlineUi = state.isOnline
    }

    val changeProfileImage = rememberProfileImagePicker { newUrl ->
        provider = provider?.copy(profileImageUrl = newUrl)
        scope.launch {
            runCatching {
                api.updateProviderProfile(
                    UpdateProfileRequest(
                        name = provider?.name.orEmpty(),
                        phone = provider?.phone.orEmpty(),
                        profileImageUrl = newUrl
                    )
                )
            }
        }
    }

    val pendingRequests = state.pendingJobs
    val ongoingRequests = state.ongoingJobs
    val completedRequests = state.completedJobs

    Scaffold(
        containerColor = StandardDashboardColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            StandardTopBar(
                onNotifications = {
                    navController.navigate(Routes.ProviderNotifications.route) {
                        launchSingleTop = true
                    }
                },
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

        when {
            loading -> LoadingState(innerPadding)

            provider == null -> ErrorState(
                modifier = Modifier.padding(innerPadding),
                message = errorMessage ?: state.error.ifBlank { "Provider profile not found." },
                onRetry = {
                    scope.launch {
                        loadProviderProfile()
                        vm.refreshAll()
                    }
                }
            )

            else -> {
                val safeProvider = provider!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        StandardHeaderCard(
                            provider = safeProvider,
                            isOnline = isOnlineUi,
                            availabilityUpdating = availabilityUpdating,
                            activeJobs = ongoingRequests.size,
                            onToggleAvailability = { newValue ->
                                if (availabilityUpdating) return@StandardHeaderCard

                                val previous = isOnlineUi
                                isOnlineUi = newValue

                                scope.launch {
                                    availabilityUpdating = true
                                    vm.toggleAvailability(newValue)

                                    runCatching {
                                        api.updateProviderAvailability(
                                            UpdateAvailabilityBody(isAvailable = newValue)
                                        )
                                    }.onFailure {
                                        isOnlineUi = previous
                                        errorMessage = it.message ?: "Failed to update availability."
                                    }.onSuccess { res ->
                                        if (!res.isSuccessful) {
                                            isOnlineUi = previous
                                            errorMessage = runCatching { res.errorBody()?.string() }
                                                .getOrNull() ?: "Failed (${res.code()})"
                                        }
                                    }

                                    vm.refreshAll()
                                    availabilityUpdating = false
                                }
                            },
                            onEditProfile = {
                                navController.navigate(Routes.EditProviderProfile.route) {
                                    launchSingleTop = true
                                }
                            },
                            onChangeProfileImage = { changeProfileImage() }
                        )
                    }

                    item {
                        StatsRow(
                            completed = completedRequests.size,
                            totalEarned = totalEarned,
                            rating = safeProvider.rating
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Quick Actions",
                            subtitle = "Access the main tools quickly"
                        )
                    }

                    item {
                        QuickActionsGrid(
                            onWallet = {
                                navController.navigate(Routes.ProviderWallet.route) {
                                    launchSingleTop = true
                                }
                            },
                            onChats = {
                                navController.navigate(Routes.ProviderChatList.route) {
                                    launchSingleTop = true
                                }
                            },
                            onNotifications = {
                                navController.navigate(Routes.ProviderNotifications.route) {
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
                        SectionHeader(
                            title = "Job Activity",
                            subtitle = "Pending, active, and completed jobs"
                        )
                    }

                    item {
                        StandardTabs(
                            selected = tabIndex,
                            onSelect = { tabIndex = it }
                        )
                    }

                    when (tabIndex) {
                        0 -> {
                            if (pendingRequests.isEmpty()) {
                                item {
                                    EmptyJobsState(
                                        title = "No pending requests",
                                        subtitle = "New service requests will appear here."
                                    )
                                }
                            } else {
                                item { PendingBanner(count = pendingRequests.size) }

                                items(
                                    items = pendingRequests,
                                    key = { it.safeRequestId() }
                                ) { request ->
                                    DashboardRequestCard(
                                        request = request,
                                        mode = RequestCardMode.PENDING,
                                        onView = {
                                            navController.navigate(
                                                Routes.ProviderRequestDetails.createRoute(
                                                    request.safeRequestId()
                                                )
                                            )
                                        },
                                        onAccept = {
                                            scope.launch {
                                                val id = request.safeRequestId()
                                                if (id.isNotBlank()) {
                                                    vm.acceptJob(id)
                                                    vm.refreshAll()
                                                    navController.navigate(
                                                        Routes.ProviderActiveJob.createRoute(id)
                                                    )
                                                }
                                            }
                                        },
                                        onDecline = {
                                            scope.launch {
                                                val id = request.safeRequestId()
                                                if (id.isNotBlank()) {
                                                    vm.declineJob(id)
                                                    vm.refreshAll()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        1 -> {
                            if (ongoingRequests.isEmpty()) {
                                item {
                                    EmptyJobsState(
                                        title = "No active jobs",
                                        subtitle = "Accepted and ongoing jobs will appear here."
                                    )
                                }
                            } else {
                                items(
                                    items = ongoingRequests,
                                    key = { it.safeRequestId() }
                                ) { request ->
                                    DashboardRequestCard(
                                        request = request,
                                        mode = RequestCardMode.ACTIVE,
                                        onView = {
                                            val id = request.safeRequestId()
                                            if (id.isNotBlank()) {
                                                navController.navigate(
                                                    Routes.ProviderActiveJob.createRoute(id)
                                                )
                                            }
                                        },
                                        onAccept = {},
                                        onDecline = {}
                                    )
                                }
                            }
                        }

                        2 -> {
                            if (completedRequests.isEmpty()) {
                                item {
                                    EmptyJobsState(
                                        title = "No history yet",
                                        subtitle = "Completed jobs will appear here."
                                    )
                                }
                            } else {
                                items(
                                    items = completedRequests,
                                    key = { it.safeRequestId() }
                                ) { request ->
                                    DashboardRequestCard(
                                        request = request,
                                        mode = RequestCardMode.HISTORY,
                                        onView = {
                                            val id = request.safeRequestId()
                                            if (id.isNotBlank()) {
                                                navController.navigate(
                                                    Routes.ProviderRequestDetails.createRoute(id)
                                                )
                                            }
                                        },
                                        onAccept = {},
                                        onDecline = {}
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

private fun RequestDto.safeRequestId(): String {
    return _id ?: id ?: requestId ?: ""
}

private fun RequestDto.safeServiceText(): String {
    return when {
        !service.isNullOrBlank() -> service ?: ""
        !providerType.isNullOrBlank() -> providerType ?: ""
        else -> "service"
    }
}

private fun RequestDto.safeProblemText(): String {
    return when {
        !problem.isNullOrBlank() -> problem ?: ""
        !note.isNullOrBlank() -> note ?: ""
        else -> "No problem details provided."
    }
}

private fun RequestDto.safeVehicleText(): String {
    return vehicleInfo?.takeIf { it.isNotBlank() } ?: "Vehicle not specified"
}

private fun RequestDto.safeCustomerName(): String {
    return userName?.takeIf { it.isNotBlank() } ?: "Customer"
}

private fun RequestDto.safeCustomerPhone(): String {
    return userPhone?.takeIf { it.isNotBlank() } ?: "No phone"
}

private fun RequestDto.safeUrgency(): String {
    return urgency?.lowercase()?.trim().orEmpty()
}

private fun RequestDto.safePaymentStatus(): String {
    return paymentStatus?.trim()?.lowercase().orEmpty()
}

private fun RequestDto.bestAmount(): Double? {
    return totalAmount?.takeIf { it > 0 }
        ?: amount?.takeIf { it > 0 }
        ?: price?.takeIf { it > 0 }
}

private fun RequestDto.hasLocation(): Boolean {
    val lat = userLocation?.lat ?: 0.0
    val lng = userLocation?.lng ?: 0.0
    return lat != 0.0 || lng != 0.0
}

@Composable
private fun StandardTopBar(
    onNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        color = StandardDashboardColors.Surface,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StandardDashboardColors.HeaderGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Auto Aid",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StandardDashboardColors.TextPrimary
                        )
                        Text(
                            text = "Provider Dashboard",
                            fontSize = 12.sp,
                            color = StandardDashboardColors.TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TopBarAction(
                        icon = Icons.Outlined.NotificationsNone,
                        background = StandardDashboardColors.PrimaryLight,
                        tint = StandardDashboardColors.PrimaryDark,
                        onClick = onNotifications
                    )

                    TopBarAction(
                        icon = Icons.Outlined.Logout,
                        background = StandardDashboardColors.DangerSoft,
                        tint = StandardDashboardColors.Danger,
                        onClick = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarAction(
    icon: ImageVector,
    background: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
    }
}

@Composable
private fun StandardHeaderCard(
    provider: DashboardProviderUi,
    isOnline: Boolean,
    availabilityUpdating: Boolean,
    activeJobs: Int,
    onToggleAvailability: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onChangeProfileImage: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StandardDashboardColors.Surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderAvatar(
                    displayName = provider.name,
                    onClick = onChangeProfileImage
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name.ifBlank { "Provider" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StandardDashboardColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = prettyLabel(provider.providerType),
                        fontSize = 13.sp,
                        color = StandardDashboardColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusBadge(isOnline = isOnline)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StandardDashboardColors.PrimaryLight)
                        .clickable { onEditProfile() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = StandardDashboardColors.PrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StandardDashboardColors.PrimaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StandardDashboardColors.SurfaceSoft,
                border = BorderStroke(1.dp, StandardDashboardColors.Border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Availability",
                            fontSize = 12.sp,
                            color = StandardDashboardColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (availabilityUpdating) {
                                "Updating..."
                            } else if (isOnline) {
                                "You are online"
                            } else {
                                "You are offline"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) {
                                StandardDashboardColors.Success
                            } else {
                                StandardDashboardColors.TextPrimary
                            }
                        )
                    }

                    Switch(
                        checked = isOnline,
                        onCheckedChange = onToggleAvailability,
                        enabled = !availabilityUpdating,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StandardDashboardColors.Primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = StandardDashboardColors.TextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StandardDashboardColors.PrimaryDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Overview",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$activeJobs Active Job${if (activeJobs == 1) "" else "s"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.Dashboard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderAvatar(
    displayName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(StandardDashboardColors.PrimaryLight)
            .border(1.dp, StandardDashboardColors.Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.trim().take(1).uppercase().ifBlank { "P" },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = StandardDashboardColors.PrimaryDark
        )
    }
}

@Composable
private fun StatusBadge(isOnline: Boolean) {
    val bg = if (isOnline) {
        StandardDashboardColors.SuccessSoft
    } else {
        StandardDashboardColors.SurfaceSoft
    }

    val textColor = if (isOnline) {
        StandardDashboardColors.Success
    } else {
        StandardDashboardColors.TextSecondary
    }

    val pulse by rememberInfiniteTransition(label = "status").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusPulse"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isOnline) pulse else 1f)
                .background(textColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun StatsRow(
    completed: Int,
    totalEarned: Double,
    rating: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatsCard(
            modifier = Modifier.weight(1f),
            title = "Completed",
            value = completed.toString(),
            icon = Icons.Outlined.CheckCircle
        )
        StatsCard(
            modifier = Modifier.weight(1f),
            title = "Earned",
            value = shortUgx(totalEarned),
            icon = Icons.Outlined.AccountBalanceWallet
        )
        StatsCard(
            modifier = Modifier.weight(1f),
            title = "Rating",
            value = formatRating(rating),
            icon = Icons.Outlined.StarOutline
        )
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = StandardDashboardColors.Surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StandardDashboardColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StandardDashboardColors.PrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StandardDashboardColors.TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = StandardDashboardColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StandardDashboardColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = StandardDashboardColors.TextSecondary
        )
    }
}

@Composable
private fun QuickActionsGrid(
    onWallet: () -> Unit,
    onChats: () -> Unit,
    onNotifications: () -> Unit,
    onMap: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Wallet",
                subtitle = "Open wallet",
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onWallet
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Chats",
                subtitle = "Open chats",
                icon = Icons.Outlined.ChatBubbleOutline,
                onClick = onChats
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Alerts",
                subtitle = "Notifications",
                icon = Icons.Outlined.NotificationsNone,
                onClick = onNotifications
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Map",
                subtitle = "Location tools",
                icon = Icons.Outlined.Map,
                onClick = onMap
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = StandardDashboardColors.Surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StandardDashboardColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StandardDashboardColors.PrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = StandardDashboardColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = StandardDashboardColors.TextSecondary
            )
        }
    }
}

@Composable
private fun StandardTabs(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val tabs = listOf("Requests", "Active", "History")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StandardDashboardColors.Surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val active = selected == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) StandardDashboardColors.Primary else Color.Transparent
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White else StandardDashboardColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingBanner(count: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StandardDashboardColors.WarningSoft,
        border = BorderStroke(1.dp, StandardDashboardColors.Warning)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "You have $count pending request${if (count == 1) "" else "s"} waiting.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = StandardDashboardColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StandardDashboardColors.Warning)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "NEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private enum class RequestCardMode {
    PENDING, ACTIVE, HISTORY
}

@Composable
private fun DashboardRequestCard(
    request: RequestDto,
    mode: RequestCardMode,
    onView: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val serviceText = request.safeServiceText()
    val problemText = request.safeProblemText()
    val vehicleText = request.safeVehicleText()
    val urgency = request.safeUrgency()
    val amount = request.bestAmount()

    val urgencyColor = when (urgency) {
        "high" -> StandardDashboardColors.Danger
        "medium" -> StandardDashboardColors.Warning
        "low" -> StandardDashboardColors.Success
        else -> StandardDashboardColors.TextMuted
    }

    val paymentColor = when (request.safePaymentStatus()) {
        "paid" -> StandardDashboardColors.Success
        "pending" -> StandardDashboardColors.Warning
        else -> StandardDashboardColors.TextMuted
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = RoundedCornerShape(18.dp),
        color = StandardDashboardColors.Surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prettyLabel(serviceText),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StandardDashboardColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = vehicleText,
                        fontSize = 12.sp,
                        color = StandardDashboardColors.TextSecondary
                    )
                }

                if (urgency.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(urgencyColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = urgency.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = urgencyColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            InfoLine(Icons.Outlined.PersonOutline, request.safeCustomerName())
            Spacer(modifier = Modifier.height(6.dp))
            InfoLine(Icons.Outlined.Call, request.safeCustomerPhone())
            Spacer(modifier = Modifier.height(6.dp))
            InfoLine(
                Icons.Outlined.LocationOn,
                if (request.hasLocation()) "Pickup available" else "No location"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = problemText,
                fontSize = 12.sp,
                color = StandardDashboardColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = amount?.let { formatUgx(it) } ?: "No amount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StandardDashboardColors.PrimaryDark
                )

                if (request.safePaymentStatus().isNotBlank()) {
                    Text(
                        text = request.safePaymentStatus().uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = paymentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = StandardDashboardColors.Border)
            Spacer(modifier = Modifier.height(12.dp))

            when (mode) {
                RequestCardMode.PENDING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StandardDashboardColors.DangerSoft,
                                contentColor = StandardDashboardColors.Danger
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Decline")
                        }

                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StandardDashboardColors.Primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Accept")
                        }
                    }
                }

                RequestCardMode.ACTIVE -> {
                    OutlineActionButton("Open Job", onView)
                }

                RequestCardMode.HISTORY -> {
                    OutlineActionButton("View Details", onView)
                }
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StandardDashboardColors.PrimaryDark,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = StandardDashboardColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OutlineActionButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = StandardDashboardColors.PrimaryLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = StandardDashboardColors.PrimaryDark,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = StandardDashboardColors.PrimaryDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyJobsState(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = StandardDashboardColors.Surface,
        border = BorderStroke(1.dp, StandardDashboardColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(StandardDashboardColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inbox,
                    contentDescription = null,
                    tint = StandardDashboardColors.PrimaryDark,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StandardDashboardColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = StandardDashboardColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingState(
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = StandardDashboardColors.Primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading dashboard...",
                fontSize = 13.sp,
                color = StandardDashboardColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = StandardDashboardColors.Surface,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StandardDashboardColors.DangerSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = StandardDashboardColors.Danger
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StandardDashboardColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StandardDashboardColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun ProviderBottomNav(
    navController: NavHostController,
    selectedRoute: String
) {
    val items = listOf(
        Triple(Routes.ProviderDashboard.route, Icons.Outlined.Dashboard, "Dashboard"),
        Triple(Routes.ProviderMapHome.route, Icons.Outlined.Map, "Map"),
        Triple(Routes.ProviderChatList.route, Icons.Outlined.ChatBubbleOutline, "Chat"),
        Triple(Routes.ProviderProfile.route, Icons.Outlined.PersonOutline, "Profile"),
    )

    Surface(
        color = StandardDashboardColors.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { (route, icon, label) ->
                val isSelected = selectedRoute == route

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) StandardDashboardColors.PrimaryLight else Color.Transparent
                        )
                        .clickable {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.ProviderDashboard.route) { saveState = true }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) {
                            StandardDashboardColors.PrimaryDark
                        } else {
                            StandardDashboardColors.TextMuted
                        },
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            StandardDashboardColors.PrimaryDark
                        } else {
                            StandardDashboardColors.TextMuted
                        }
                    )

                    AnimatedVisibility(visible = isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 16.dp, height = 3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(StandardDashboardColors.Primary)
                        )
                    }
                }
            }
        }
    }
}

private fun prettyLabel(value: String): String {
    return value
        .replace("_", " ")
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
        .ifBlank { "-" }
}

private fun formatRating(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "UGX ${formatter.format(amount)}"
}

private fun shortUgx(amount: Double): String {
    return when {
        amount >= 1_000_000 -> {
            val v = amount / 1_000_000.0
            "UGX ${String.format(Locale.US, "%.1f", v)}M"
        }
        amount >= 1_000 -> {
            val v = amount / 1_000.0
            "UGX ${String.format(Locale.US, "%.0f", v)}K"
        }
        else -> "UGX ${amount.toInt()}"
    }
}