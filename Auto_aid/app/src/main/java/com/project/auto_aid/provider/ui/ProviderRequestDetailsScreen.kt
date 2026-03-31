package com.project.auto_aid.provider.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.provider.ProviderViewModel
import kotlinx.coroutines.launch

private val Blue = Color(0xFF1DA1F2)
private val DeepBlue = Color(0xFF114B5F)
private val SoftBlue = Color(0xFFEAF6FF)
private val ScreenBg = Color(0xFFF6F9FC)
private val CardBg = Color.White
private val SuccessGreen = Color(0xFF20B26B)
private val SuccessGreenSoft = Color(0xFFEAF8F1)
private val WarningOrange = Color(0xFFF59E0B)
private val WarningOrangeSoft = Color(0xFFFFF4DB)
private val DangerRed = Color(0xFFE53935)
private val DangerRedSoft = Color(0xFFFDECEC)
private val MutedText = Color(0xFF6B7280)
private val BorderSoft = Color(0xFFE5EEF5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderRequestDetailsScreen(
    navController: NavHostController,
    requestId: String
) {
    val context = LocalContext.current
    val api = remember(context) { RetrofitClient.create(TokenStore(context)) }
    val vm: ProviderViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var request by remember { mutableStateOf<RequestDto?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val res = api.getRequestById(requestId)
                if (!res.isSuccessful) {
                    throw Exception("Failed to load request (HTTP ${res.code()})")
                }

                request = res.body() ?: throw Exception("Empty request response")
            } catch (e: Throwable) {
                error = e.message ?: "Failed to load request"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(requestId) {
        load()
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Details",
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Blue
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue)
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    StatusBanner(
                        title = "Something went wrong",
                        body = error ?: "Unable to load request",
                        background = DangerRedSoft,
                        contentColor = DangerRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { load() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Retry")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Back", color = Blue)
                    }
                }
            }

            else -> {
                val r = request ?: return@Scaffold

                val status = (r.status ?: "pending").trim().lowercase()
                val lat = r.userLocation?.lat ?: 0.0
                val lng = r.userLocation?.lng ?: 0.0
                val hasLocation = lat != 0.0 && lng != 0.0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HeroCard(
                        requestId = requestId,
                        serviceName = serviceDisplayName(r.service ?: r.providerType),
                        status = formatStatus(r.status),
                        customerName = r.userName ?: "Unknown customer",
                        phone = r.userPhone ?: "No phone"
                    )

                    InfoSectionCard(
                        title = "Problem Summary",
                        icon = Icons.Default.WarningAmber
                    ) {
                        DetailRow(
                            icon = Icons.Default.WarningAmber,
                            label = "Problem",
                            value = r.problem ?: "-"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(
                            icon = Icons.Default.DirectionsCar,
                            label = "Vehicle",
                            value = r.vehicleInfo ?: "-"
                        )

                        if (!r.towType.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow(
                                icon = Icons.Default.BuildCircle,
                                label = "Tow Type",
                                value = r.towType ?: "-"
                            )
                        }

                        if (!r.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow(
                                icon = Icons.Default.BuildCircle,
                                label = "Note",
                                value = r.note ?: "-"
                            )
                        }
                    }

                    InfoSectionCard(
                        title = "Customer Information",
                        icon = Icons.Default.Person
                    ) {
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "Customer",
                            value = r.userName ?: "-"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(
                            icon = Icons.Default.Phone,
                            label = "Phone",
                            value = r.userPhone ?: "-"
                        )
                    }

                    InfoSectionCard(
                        title = "Pickup Location",
                        icon = Icons.Default.LocationOn
                    ) {
                        if (hasLocation) {
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "Latitude",
                                value = lat.toString()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "Longitude",
                                value = lng.toString()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    navController.navigate(
                                        Routes.ProviderMapScreen.createRoute(
                                            requestId = requestId,
                                            pickupLat = lat,
                                            pickupLng = lng
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preview Route")
                            }
                        } else {
                            StatusBanner(
                                title = "Location Missing",
                                body = "No pickup location was saved for this request.",
                                background = DangerRedSoft,
                                contentColor = DangerRed
                            )
                        }
                    }

                    if (status == "pending") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (working) return@OutlinedButton
                                    working = true
                                    scope.launch {
                                        try {
                                            vm.declineJob(requestId)
                                            navController.popBackStack()
                                        } finally {
                                            working = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                enabled = !working
                            ) {
                                Text("Decline")
                            }

                            Button(
                                onClick = {
                                    if (working) return@Button
                                    working = true
                                    scope.launch {
                                        try {
                                            vm.acceptJob(requestId)
                                            navController.navigate(
                                                Routes.ProviderActiveJob.createRoute(requestId)
                                            ) {
                                                popUpTo(Routes.ProviderDashboard.route) {
                                                    inclusive = false
                                                }
                                            }
                                        } finally {
                                            working = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                                shape = RoundedCornerShape(18.dp),
                                enabled = !working
                            ) {
                                if (working) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Accept Request")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                navController.navigate(
                                    Routes.ProviderActiveJob.createRoute(requestId)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("Open Active Job", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    requestId: String,
    serviceName: String,
    status: String,
    customerName: String,
    phone: String
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(SoftBlue, Color.White)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(Blue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BuildCircle,
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = serviceName,
                            color = DeepBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Request #$requestId",
                            color = MutedText
                        )
                    }

                    StatusChip(status = status)
                }

                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Customer",
                    value = customerName
                )

                DetailRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = phone
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val bg = when (status.lowercase()) {
        "pending" -> WarningOrangeSoft
        "accepted", "assigned" -> SoftBlue
        "on the way" -> SoftBlue
        "arrived" -> SuccessGreenSoft
        "completed" -> SuccessGreenSoft
        else -> SoftBlue
    }

    val fg = when (status.lowercase()) {
        "pending" -> WarningOrange
        "accepted", "assigned" -> Blue
        "on the way" -> Blue
        "arrived" -> SuccessGreen
        "completed" -> SuccessGreen
        else -> Blue
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = status,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = bg,
            labelColor = fg
        )
    )
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SoftBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Blue,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = label,
                color = MutedText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = DeepBlue,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatusBanner(
    title: String,
    body: String,
    background: Color,
    contentColor: Color
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                color = contentColor
            )
        }
    }
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
        "garage" -> "Garage Service"
        "towing" -> "Towing Service"
        "ambulance" -> "Ambulance Service"
        else -> "AutoAid Service"
    }
}

private fun formatStatus(status: String?): String {
    return when (status?.trim()?.lowercase()) {
        "pending" -> "Pending"
        "assigned", "accepted" -> "Accepted"
        "en_route", "on_the_way", "provider_on_the_way" -> "On The Way"
        "arrived" -> "Arrived"
        "awaiting_payment" -> "Awaiting Payment"
        "payment_confirmed" -> "Payment Confirmed"
        "in_progress" -> "In Progress"
        "awaiting_dual_confirmation" -> "Awaiting Confirmation"
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        else -> status ?: "Unknown"
    }
}