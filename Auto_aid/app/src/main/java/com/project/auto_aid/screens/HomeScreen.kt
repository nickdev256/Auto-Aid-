package com.project.auto_aid.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.auto_aid.R
import com.project.auto_aid.components.GpsLocationSearchField
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.viewmodel.HomeViewModel
import com.project.auto_aid.viewmodel.HomeViewModelFactory

object AppColors {
    val background = Color(0xFFF9F9F9)
    val primary = Color(0xFF19ABD9)
    val secondary = Color(0xFFE5E7EB)
    val textPrimary = Color(0xFF374151)
    val textSecondary = Color(0xFF4B5563)
    val referralCardBackground = Color(0xFFE3F2FD)
    val referralCardIcon = Color(0xFFFFC107)
}

data class ReferralUiData(
    val title: String = "Refer a Friend",
    val subtitle: String = "Invite friends to AutoAid. They get UGX 5,000 off their first service, and you get UGX 5,000 off your next service after they complete it.",
    val code: String = "",
    val earnedAmount: String = "UGX 0",
    val bonusText: String = "0 rewarded"
)

data class QuickAccessItem(val iconRes: Int, val title: String)

data class LiveFeaturedServiceItem(
    val name: String,
    val subtitle: String,
    val type: String,
    val rating: Double,
    val imageRes: Int
)

data class RecentItem(
    val requestId: String,
    val service: String,
    val date: String,
    val status: String,
    val icon: ImageVector
)

object AppImages {
    val shell = R.drawable.fuel1
    val stabex = R.drawable.garage
    val total = R.drawable.towi1
    val rubis = R.drawable.ambulance
}

val quickAccessData = listOf(
    QuickAccessItem(R.drawable.garage, "Garage"),
    QuickAccessItem(R.drawable.tow, "Towing Track"),
    QuickAccessItem(R.drawable.fu, "Fuel Delivery"),
    QuickAccessItem(R.drawable.medical, "Ambulance")
)

@Composable
fun HomeScreen(navController: NavHostController) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val tokenStore = if (isPreview) null else remember(context) { TokenStore(context) }
    val api = if (isPreview || tokenStore == null) null else remember(tokenStore) { RetrofitClient.create(tokenStore) }

    val viewModel: HomeViewModel? =
        if (isPreview || api == null) null
        else viewModel(factory = HomeViewModelFactory(api))

    val uiState = viewModel?.uiState

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

    val referral = ReferralUiData(
        title = "Refer a Friend",
        subtitle = "Invite friends to AutoAid. They get UGX 5,000 off their first service, and you get UGX 5,000 off your next service after they complete it.",
        code = uiState?.referralCode.orEmpty(),
        earnedAmount = "UGX ${(uiState?.nextReferralDiscountAmount ?: 0.0).toInt()}",
        bonusText = "${uiState?.rewardedReferralCount ?: 0} rewarded"
    )

    LaunchedEffect(viewModel) {
        viewModel?.loadInitialData()
    }

    LaunchedEffect(viewModel, pickedLat, pickedLng) {
        viewModel?.loadProviders(pickedLat, pickedLng)
    }

    LaunchedEffect(viewModel) {
        viewModel?.startRealtimeUpdates()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel?.stopRealtimeUpdates()
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                notificationCount = uiState?.notificationCount ?: 0
            )
        },
        floatingActionButton = {
            FloatingAiChatButton(
                navController = navController,
                pickedLabel = pickedLabel,
                pickedLat = pickedLat,
                pickedLng = pickedLng
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.background)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            item { TopHeader(userName = uiState?.userName ?: "User") }

            item {
                SearchAndProfileBar(
                    navController = navController,
                    pickedLabel = pickedLabel,
                    pickedLat = pickedLat,
                    pickedLng = pickedLng
                )
            }

            item {
                QuickAccessGrid(
                    navController = navController,
                    pickedLabel = pickedLabel,
                    pickedLat = pickedLat,
                    pickedLng = pickedLng
                )
            }

            item {
                ReferralCard(
                    referral = referral,
                    isLoading = uiState?.isReferralLoading == true,
                    onCopyClick = { code ->
                        if (code.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Referral code copied", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Referral code not ready yet", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReferClick = { data ->
                        val playStoreLink = "https://play.google.com/store/apps/details?id=com.project.auto_aid"
                        val websiteLink = "https://autoaid-web.vercel.app"

                        val shareMessage = """
Join AutoAid for roadside help 🚗

Use my referral code: ${data.code}

You get UGX 5,000 off your first completed service.

Play Store:
$playStoreLink

Website:
$websiteLink
""".trimIndent()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Join AutoAid")
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }

                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share AutoAid with a friend")
                        )
                    }
                )
            }

            if ((uiState?.isProfileLoading == true) || (uiState?.isProvidersLoading == true)) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                FeaturesSection(
                    title = uiState?.featuredTitle ?: "Featured Services",
                    items = uiState?.featuredServices.orEmpty()
                )
            }

            item {
                RecentsSection(
                    navController = navController,
                    items = uiState?.recentItems.orEmpty()
                )
            }

            uiState?.error?.let { errorMessage ->
                if (errorMessage.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3F3)
                            )
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(14.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun TopHeader(userName: String) {
    var showName by rememberSaveable(userName) { mutableStateOf(false) }

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
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(35.dp),
                    tint = Color.White
                )
            },
            label = {
                Text(
                    "Home",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = currentRoute == Routes.NotificationScreen.route,
            onClick = { navigateSingleTop(Routes.NotificationScreen.route) },
            icon = {
                Box(
                    modifier = Modifier.size(35.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(35.dp)
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
            label = {
                Text(
                    "Alerts",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = currentRoute == Routes.SettingsScreen.route,
            onClick = { navigateSingleTop(Routes.SettingsScreen.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    modifier = Modifier.size(35.dp),
                    contentDescription = "Settings",
                    tint = Color.White
                )
            },
            label = {
                Text(
                    "Settings",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
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
    var locationText by rememberSaveable { mutableStateOf(pickedLabel) }

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
            lat = pickedLat,
            lng = pickedLng,
            onOpenMapPicker = { lat, lng ->
                navController.navigate(
                    Routes.LocationPicker.createRoute(lat = lat, lng = lng)
                )
            }
        )

        if (pickedLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Picked: $pickedLabel",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun FloatingAiChatButton(
    navController: NavHostController,
    pickedLabel: String,
    pickedLat: Double,
    pickedLng: Double
) {
    Card(
        modifier = Modifier
            .clickable {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("picked_location_label", pickedLabel)

                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("picked_location_lat", pickedLat)

                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("picked_location_lng", pickedLng)

                navController.navigate(
                    Routes.AiAssistantScreen.createRoute(
                        address = pickedLabel,
                        lat = pickedLat,
                        lng = pickedLng
                    )
                )
            },
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF132238)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF0F172A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ai_icon),
                    contentDescription = "AI Assistant",
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "\uD83E\uDD16 Ask AI",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
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
                navController.currentBackStackEntry?.savedStateHandle?.set("picked_location_label", pickedLabel)
                navController.currentBackStackEntry?.savedStateHandle?.set("picked_location_lat", pickedLat)
                navController.currentBackStackEntry?.savedStateHandle?.set("picked_location_lng", pickedLng)

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
                modifier = Modifier.size(50.dp)
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
fun ReferralCard(
    referral: ReferralUiData,
    isLoading: Boolean = false,
    onReferClick: (ReferralUiData) -> Unit,
    onCopyClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.star),
                    contentDescription = "Referral reward",
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = referral.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = referral.subtitle,
                        fontSize = 13.sp,
                        color = AppColors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoading && referral.earnedAmount != "UGX 0") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFDFF7E8))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Reward available",
                        color = Color(0xFF15803D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Referral Code",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoading) "Loading..." else referral.code.ifBlank { "AUTOAID" },
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary
                )

                TextButton(
                    onClick = { onCopyClick(referral.code) },
                    enabled = !isLoading && referral.code.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Text("Copy")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReferralStatItem(
                    label = "Next Reward",
                    value = if (isLoading) "..." else referral.earnedAmount
                )
                ReferralStatItem(
                    label = "Referrals",
                    value = if (isLoading) "..." else referral.bonusText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onReferClick(referral) },
                enabled = !isLoading && referral.code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Refer Now",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ReferralStatItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
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

    if (items.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Text(
                text = "No featured providers yet",
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
            ServiceCard(item)
        }
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
        items(items, key = { it.requestId }) { item ->
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
    val statusColor = when (item.status.lowercase()) {
        "completed", "paid", "payment confirmed" -> Color(0xFF16A34A)
        "cancelled" -> Color(0xFFDC2626)
        "pending", "assigned", "on going", "arrived", "quotation sent", "awaiting payment" -> Color(0xFFF59E0B)
        else -> Color.Gray
    }

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