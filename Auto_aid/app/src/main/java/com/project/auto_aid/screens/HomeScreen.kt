package com.project.auto_aid.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.auto_aid.R
import com.project.auto_aid.components.GpsLocationSearchField
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

object AppColors {
    val background = Color(0xFFF9F9F9)
    val primary = Color(0xFF19ABD9)
    val secondary = Color(0xFFE5E7EB)
    val textPrimary = Color(0xFF374151)
    val textSecondary = Color(0xFF4B5563)
    val referralCardBackground = Color(0xFFEDE9FE)
    val referralCardIcon = Color(0xFF7C3AED)
}

data class QuickAccessItem(val iconRes: Int, val title: String)

data class RecentItem(
    val requestId: String,
    val service: String,
    val date: String,
    val status: String,
    val icon: ImageVector
)

data class ReferralUiData(
    val title: String = "Refer a friend",
    val subtitle: String = "Earn rewards instantly",
    val code: String = "AUTOAID",
    val earnedAmount: String = "UGX 0"
)

data class LiveFeaturedServiceItem(
    val name: String,
    val subtitle: String,
    val type: String,
    val rating: Double,
    val imageRes: Int
)

val quickAccessData = listOf(
    QuickAccessItem(R.drawable.garage, "Garage"),
    QuickAccessItem(R.drawable.towing_vehicle, "Towing Track"),
    QuickAccessItem(R.drawable.fu, "Fuel Delivery"),
    QuickAccessItem(R.drawable.ambulance, "Ambulance")
)

object AppImages {
    val shell = R.drawable.shell_2
    val total = R.drawable.total_1
    val stabex = R.drawable.stabex_2
    val rubis = R.drawable.rubis_1
    val hass = R.drawable.hass_1
    val gazz = R.drawable.gazz_1
}

private fun normalizeServiceKey(value: String?): String {
    return when (value?.trim()?.lowercase()) {
        "fuel", "fuel delivery" -> "fuel"
        "garage", "garage repair" -> "garage"
        "towing", "tow", "towing service", "towing track" -> "towing"
        "ambulance", "ambulance service" -> "ambulance"
        else -> ""
    }
}

private fun serviceDisplayName(service: String?): String {
    return when (normalizeServiceKey(service)) {
        "fuel" -> "Fuel Delivery"
        "garage" -> "Garage"
        "towing" -> "Towing Service"
        "ambulance" -> "Ambulance Service"
        else -> "AutoAid Service"
    }
}

private fun serviceIcon(service: String?): ImageVector {
    return when (normalizeServiceKey(service)) {
        "fuel" -> Icons.Default.LocalGasStation
        "garage" -> Icons.Default.CarRepair
        "towing" -> Icons.Default.LocalShipping
        "ambulance" -> Icons.Default.MedicalServices
        else -> Icons.Default.Build
    }
}

private fun featuredImageFor(service: String?): Int {
    return when (normalizeServiceKey(service)) {
        "fuel" -> AppImages.shell
        "garage" -> AppImages.stabex
        "towing" -> AppImages.total
        "ambulance" -> AppImages.rubis
        else -> AppImages.shell
    }
}

private fun formatStatus(status: String?): String {
    return when (status?.trim()?.lowercase()) {
        "pending", "request_sent" -> "Pending"
        "assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned" -> "Assigned"
        "driver_on_the_way", "mechanic_on_the_way", "vendor_on_the_way", "ambulance_on_the_way" -> "On Going"
        "arrived" -> "Arrived"
        "in_progress", "delivering", "patient_picked", "vehicle_towed", "repaired" -> "On Going"
        "delivered", "at_hospital", "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        else -> status?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Unknown"
    }
}

private fun statusColorFor(status: String): Color {
    return when (status.lowercase()) {
        "completed" -> Color(0xFF16A34A)
        "cancelled" -> Color(0xFFDC2626)
        "pending", "assigned", "on going", "arrived" -> Color(0xFFF59E0B)
        else -> Color.Gray
    }
}

private fun parseServerDateToDisplay(value: String?): String {
    if (value.isNullOrBlank()) return "Unknown time"

    val inputPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    for (pattern in inputPatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.getDefault())
            val date = parser.parse(value)
            if (date != null) {
                val formatter = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
                return formatter.format(date)
            }
        } catch (_: Exception) {
        }
    }

    return value
}

private fun requestToRecentItem(req: RequestDto): RecentItem {
    return RecentItem(
        requestId = req.resolvedId(),
        service = serviceDisplayName(req.service ?: req.providerType),
        date = parseServerDateToDisplay(req.createdAt),
        status = formatStatus(req.status),
        icon = serviceIcon(req.service ?: req.providerType)
    )
}

private fun serviceUsageOrderFromRequests(requests: List<RequestDto>): List<String> {
    val counts = linkedMapOf(
        "garage" to 0,
        "towing" to 0,
        "fuel" to 0,
        "ambulance" to 0
    )

    requests.forEach { req ->
        val key = normalizeServiceKey(req.service ?: req.providerType)
        if (counts.containsKey(key)) {
            counts[key] = (counts[key] ?: 0) + 1
        }
    }

    val sorted = counts.entries
        .sortedByDescending { it.value }
        .map { it.key }

    val fallback = listOf("garage", "towing", "fuel", "ambulance")
    return (sorted + fallback).distinct()
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }

    var userName by rememberSaveable { mutableStateOf("User") }
    var notificationCount by rememberSaveable { mutableIntStateOf(0) }
    var isLoadingHome by rememberSaveable { mutableStateOf(false) }
    var featuredTitle by rememberSaveable { mutableStateOf("Featured Services") }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val pickedLocationLabelState =
        savedStateHandle?.getStateFlow("picked_location_label", "")?.collectAsState()
    val pickedLocationLatState =
        savedStateHandle?.getStateFlow("picked_location_lat", 0.0)?.collectAsState()
    val pickedLocationLngState =
        savedStateHandle?.getStateFlow("picked_location_lng", 0.0)?.collectAsState()

    val pickedLabel = pickedLocationLabelState?.value.orEmpty()
    val pickedLat = pickedLocationLatState?.value ?: 0.0
    val pickedLng = pickedLocationLngState?.value ?: 0.0

    var featuredServicesState by remember {
        mutableStateOf(
            listOf(
                LiveFeaturedServiceItem(
                    name = "Loading...",
                    subtitle = "Please wait",
                    type = "AutoAid",
                    rating = 0.0,
                    imageRes = AppImages.shell
                )
            )
        )
    }

    var recentItemsState by remember { mutableStateOf(emptyList<RecentItem>()) }

    var referralState by remember {
        mutableStateOf(
            ReferralUiData(
                title = "Refer a friend",
                subtitle = "Earn rewards instantly",
                code = "AUTOAID",
                earnedAmount = "UGX 0"
            )
        )
    }

    LaunchedEffect(pickedLat, pickedLng) {
        if (isPreview) return@LaunchedEffect

        isLoadingHome = true

        runCatching {
            val res = api.getMe()
            if (res.isSuccessful) {
                val user = res.body()?.user
                userName = user?.name?.trim().takeIf { !it.isNullOrEmpty() } ?: "User"
            } else {
                userName = "User"
            }
        }.onFailure {
            userName = "User"
        }

        runCatching {
            notificationCount = 3
        }.onFailure {
            notificationCount = 0
        }

        val serviceOrder = runCatching {
            val requestsResponse = api.getMyRequests()
            val requests = if (requestsResponse.isSuccessful) {
                requestsResponse.body().orEmpty()
            } else {
                emptyList()
            }

            val sortedRequests = requests.sortedByDescending { it.createdAt ?: "" }

            recentItemsState = sortedRequests.take(10).map { req ->
                requestToRecentItem(req)
            }

            serviceUsageOrderFromRequests(sortedRequests)
        }.getOrElse {
            recentItemsState = emptyList()
            listOf("garage", "towing", "fuel", "ambulance")
        }

        runCatching {
            val grouped = linkedMapOf(
                "garage" to mutableListOf<LiveFeaturedServiceItem>(),
                "towing" to mutableListOf<LiveFeaturedServiceItem>(),
                "fuel" to mutableListOf<LiveFeaturedServiceItem>(),
                "ambulance" to mutableListOf<LiveFeaturedServiceItem>()
            )

            suspend fun loadProvidersFor(serviceKey: String) {
                val response = api.getAvailableProviders(serviceKey, pickedLat, pickedLng)
                if (response.isSuccessful) {
                    val providers = response.body().orEmpty()
                    grouped[serviceKey]?.addAll(
                        providers.take(3).map {
                            LiveFeaturedServiceItem(
                                name = it.name ?: "${serviceDisplayName(serviceKey)} Provider",
                                subtitle = if (
                                    recentItemsState.any { item ->
                                        normalizeServiceKey(item.service) == serviceKey
                                    }
                                ) {
                                    "Based on your activity"
                                } else {
                                    "Online provider"
                                },
                                type = it.businessType ?: serviceDisplayName(serviceKey),
                                rating = it.rating ?: 0.0,
                                imageRes = featuredImageFor(serviceKey)
                            )
                        }
                    )
                }
            }

            loadProvidersFor("garage")
            loadProvidersFor("towing")
            loadProvidersFor("fuel")
            loadProvidersFor("ambulance")

            val prioritized = serviceOrder.flatMap { grouped[it].orEmpty() }

            featuredTitle =
                if (recentItemsState.isNotEmpty()) {
                    "Featured Based on Your Activity"
                } else {
                    "Featured Services"
                }

            featuredServicesState =
                if (prioritized.isNotEmpty()) {
                    prioritized
                } else {
                    listOf(
                        LiveFeaturedServiceItem(
                            name = "No providers online",
                            subtitle = "Try again later",
                            type = "AutoAid",
                            rating = 0.0,
                            imageRes = AppImages.shell
                        )
                    )
                }
        }.onFailure {
            featuredServicesState = listOf(
                LiveFeaturedServiceItem(
                    name = "Failed to load providers",
                    subtitle = "Check connection",
                    type = "AutoAid",
                    rating = 0.0,
                    imageRes = AppImages.shell
                )
            )
        }

        runCatching {
            referralState = ReferralUiData(
                title = "Refer a friend",
                subtitle = "Earn rewards instantly",
                code = "AUTOAID",
                earnedAmount = "UGX 0"
            )
        }.onFailure {
            referralState = ReferralUiData()
        }

        isLoadingHome = false
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                notificationCount = notificationCount
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.background)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            TopHeader(userName = userName)

            SearchAndProfileBar(
                navController = navController,
                pickedLabel = pickedLabel,
                pickedLat = pickedLat,
                pickedLng = pickedLng
            )

            QuickAccessGrid(
                navController = navController,
                pickedLabel = pickedLabel,
                pickedLat = pickedLat,
                pickedLng = pickedLng
            )

            ReferralCard(referralState)
            FeaturesSection(featuredTitle, featuredServicesState)

            RecentsSection(
                navController = navController,
                items = recentItemsState
            )

            if (isLoadingHome) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TopHeader(userName: String) {
    var showName by remember { mutableStateOf(false) }

    LaunchedEffect(userName) {
        showName = userName.isNotBlank()
    }

    val nameAlpha by animateFloatAsState(
        targetValue = if (showName) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "nameAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                append("Hello, ")
                withStyle(
                    style = SpanStyle(
                        color = AppColors.primary,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(userName)
                }
            },
            fontSize = 18.sp,
            modifier = Modifier.alpha(nameAlpha)
        )
    }
}

@Composable
fun AppBottomNavigationBar(
    navController: NavHostController,
    notificationCount: Int
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateSingleTop(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Routes.HomeScreen.route) {
                saveState = true
            }
        }
    }

    NavigationBar(containerColor = AppColors.primary) {
        NavigationBarItem(
            selected = currentRoute == Routes.HomeScreen.route,
            onClick = { navigateSingleTop(Routes.HomeScreen.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White) },
            label = { Text("Home", color = Color.White) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = currentRoute == Routes.NotificationScreen.route,
            onClick = { navigateSingleTop(Routes.NotificationScreen.route) },
            icon = {
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )

                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            label = { Text("Alerts", color = Color.White) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = currentRoute == Routes.SettingsScreen.route,
            onClick = { navigateSingleTop(Routes.SettingsScreen.route) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White) },
            label = { Text("Settings", color = Color.White) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}

@Composable
fun SearchAndProfileBar(
    navController: NavHostController,
    pickedLabel: String,
    pickedLat: Double,
    pickedLng: Double
) {
    var locationText by remember { mutableStateOf(pickedLabel) }

    LaunchedEffect(pickedLabel) {
        if (pickedLabel.isNotBlank()) {
            locationText = pickedLabel
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        GpsLocationSearchField(
            value = locationText,
            onValueChange = { locationText = it },
            onOpenMapPicker = { lat, lng ->
                navController.navigate(
                    Routes.LocationPicker.createRoute(
                        lat = lat,
                        lng = lng
                    )
                )
            }
        )

        if (pickedLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Picked: $pickedLabel ($pickedLat, $pickedLng)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun QuickAccessGrid(
    navController: NavHostController,
    pickedLabel: String,
    pickedLat: Double,
    pickedLng: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        quickAccessData.forEach { item ->
            QuickAccessItemView(
                item = item,
                modifier = Modifier.weight(1f),
                navController = navController,
                pickedLabel = pickedLabel,
                pickedLat = pickedLat,
                pickedLng = pickedLng
            )
        }
    }
}

@Composable
fun QuickAccessItemView(
    item: QuickAccessItem,
    modifier: Modifier,
    navController: NavHostController,
    pickedLabel: String,
    pickedLat: Double,
    pickedLng: Double
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable {
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "picked_location_label", pickedLabel
                )
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "picked_location_lat", pickedLat
                )
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "picked_location_lng", pickedLng
                )

                when (item.title) {
                    "Garage" -> navController.navigate(Routes.GarageScreen.route)
                    "Towing Track" -> navController.navigate(Routes.TowingScreen.route)
                    "Fuel Delivery" -> navController.navigate(Routes.FuelScreen.route)
                    "Ambulance" -> navController.navigate(Routes.AmbulanceScreen.route)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp), clip = false)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, AppColors.primary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.title,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            fontSize = 13.sp,
            color = AppColors.textSecondary
        )
    }
}

@Composable
fun ReferralCard(referral: ReferralUiData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(referral.title, fontWeight = FontWeight.Bold)
                Text(referral.subtitle, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Code: ${referral.code}",
                    fontSize = 12.sp,
                    color = AppColors.textSecondary
                )
                Text(
                    "Earned: ${referral.earnedAmount}",
                    fontSize = 12.sp,
                    color = AppColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Refer")
                }
            }
        }
    }
}

@Composable
fun FeaturesSection(
    title: String,
    items: List<LiveFeaturedServiceItem>
) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(20.dp),
        color = AppColors.textPrimary
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { ServiceCard(it) }
    }
}

@Composable
fun ServiceCard(item: LiveFeaturedServiceItem) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(240.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp), clip = false),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(12.dp)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = item.subtitle,
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.type,
                    color = Color(0xFFB3E5FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Rating: ${String.format("%.1f", item.rating)}",
                    color = Color(0xFFFFF59D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RecentsSection(
    navController: NavHostController,
    items: List<RecentItem>
) {
    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Recents",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = AppColors.textPrimary,
        modifier = Modifier.padding(horizontal = 20.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (items.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Text(
                text = "No requests yet",
                modifier = Modifier.padding(16.dp),
                color = AppColors.textSecondary
            )
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            RecentCard(
                item = item,
                onClick = {
                    if (item.requestId.isNotBlank()) {
                        navController.navigate(
                            Routes.RequestDetails.createRoute(item.requestId)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun RecentCard(
    item: RecentItem,
    onClick: () -> Unit
) {
    val statusColor = statusColorFor(item.status)

    Card(
        modifier = Modifier
            .width(230.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AppColors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = AppColors.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.service,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view",
                    fontSize = 11.sp,
                    color = AppColors.primary,
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    HomeScreen(rememberNavController())
}