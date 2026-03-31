package com.project.auto_aid.provider.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.SocketManager
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.data.network.dto.SetRequestPriceBody
import com.project.auto_aid.data.network.dto.UpdateStatusBody
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private val SkyBlueDark = Color(0xFF1DA1F2)
private val SkyBlueLight = Color(0xFFEAF6FF)
private val BackgroundColor = Color(0xFFF6F9FC)
private val SurfaceColor = Color.White
private val SurfaceVariantColor = Color(0xFFF4F8FB)
private val TextPrimaryColor = Color(0xFF114B5F)
private val TextSecondaryColor = Color(0xFF6B7280)
private val OnlineGreen = Color(0xFF20B26B)
private val OnlineGreenLight = Color(0xFFEAF8F1)
private val PendingOrange = Color(0xFFF59E0B)
private val DangerRedLight = Color(0xFFFDECEC)
private val OfflineGray = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderActiveJobScreen(
    requestId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }
    var trackingStatus by remember { mutableStateOf("Idle") }

    var loading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    var request by remember { mutableStateOf<RequestDto?>(null) }
    var quote by remember { mutableStateOf<RequestQuoteDto?>(null) }

    var pickupLat by remember { mutableDoubleStateOf(0.0) }
    var pickupLng by remember { mutableDoubleStateOf(0.0) }
    var pickupLabel by remember { mutableStateOf("Location not available") }

    var currentStatus by remember { mutableStateOf("pending") }
    var userPhone by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }

    var quotationAmountInput by remember { mutableStateOf("") }
    var quotationAmount by remember { mutableDoubleStateOf(0.0) }
    var paymentStatus by remember { mutableStateOf("unpaid") }

    fun normalizedStatus(value: String?): String {
        return value?.trim()?.lowercase()?.replace(" ", "_") ?: "pending"
    }

    fun normalizedPaymentStatus(value: String?): String {
        return value?.trim()?.lowercase()?.replace(" ", "_") ?: "unpaid"
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startLiveLocationUpdates() {
        if (!hasLocationPermission()) {
            trackingStatus = "Location permission missing"
            return
        }

        if (!SocketManager.isConnected()) {
            val token = tokenStore.getToken()
            if (!token.isNullOrBlank()) {
                SocketManager.connect(
                    token = token,
                    onConnected = { trackingStatus = "Tracking connected" },
                    onError = { msg -> trackingStatus = "Socket error: $msg" }
                )
            }
        }

        if (locationCallback != null) return

        val requestObj = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(2000L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                SocketManager.sendProviderLocation(location.latitude, location.longitude)
                SocketManager.providerPing()
                trackingStatus = "Sending live location"
            }
        }

        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(requestObj, callback, context.mainLooper)
        trackingStatus = "Live tracking started"
    }

    fun stopLiveLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        trackingStatus = "Tracking stopped"
    }

    fun loadData() {
        scope.launch {
            loading = true
            error = null

            try {
                val requestRes = api.getRequestById(requestId)
                if (!requestRes.isSuccessful) {
                    throw Exception("Failed to load request (${requestRes.code()})")
                }

                val r = requestRes.body() ?: throw Exception("Request not found")
                request = r
                currentStatus = normalizedStatus(r.status)
                userPhone = r.userPhone
                userName = r.userName
                pickupLat = r.userLocation?.lat ?: 0.0
                pickupLng = r.userLocation?.lng ?: 0.0
                paymentStatus = normalizedPaymentStatus(r.paymentStatus)

                pickupLabel = if (pickupLat != 0.0 || pickupLng != 0.0) {
                    getLocationName(context, pickupLat, pickupLng)
                } else {
                    "Location not available"
                }

                val quoteRes = api.getRequestQuote(requestId)
                if (quoteRes.isSuccessful) {
                    val q = quoteRes.body()
                    quote = q
                    quotationAmount = when {
                        (q?.totalAmount ?: 0.0) > 0 -> q?.totalAmount ?: 0.0
                        (q?.providerAmount ?: 0.0) > 0 -> q?.providerAmount ?: 0.0
                        else -> 0.0
                    }

                    if (quotationAmount > 0.0 && quotationAmountInput.isBlank()) {
                        quotationAmountInput = quotationAmount.toInt().toString()
                    }
                }
            } catch (e: Throwable) {
                error = e.message ?: "Failed to load job"
            } finally {
                loading = false
            }
        }
    }

    fun acceptRequest() {
        scope.launch {
            actionLoading = true
            error = null
            success = null

            try {
                val res = api.assignRequest(requestId)
                if (!res.isSuccessful) {
                    throw Exception("Failed to accept request (${res.code()})")
                }

                currentStatus = normalizedStatus(res.body()?.status ?: "accepted")
                success = "Request accepted"
                loadData()
            } catch (e: Throwable) {
                error = e.message ?: "Failed to accept request"
            } finally {
                actionLoading = false
            }
        }
    }

    fun updateSimpleStatus(status: String, doneMessage: String) {
        scope.launch {
            actionLoading = true
            error = null
            success = null

            try {
                val res = api.updateRequestStatus(
                    requestId = requestId,
                    body = UpdateStatusBody(status = status)
                )

                if (!res.isSuccessful) {
                    throw Exception("Failed to update status (${res.code()})")
                }

                currentStatus = normalizedStatus(res.body()?.request?.status ?: status)
                success = doneMessage
                loadData()
            } catch (e: Throwable) {
                error = e.message ?: "Failed to update status"
            } finally {
                actionLoading = false
            }
        }
    }

    fun sendQuotation() {
        val amount = quotationAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            error = "Enter a valid quotation amount"
            success = null
            return
        }

        scope.launch {
            actionLoading = true
            error = null
            success = null

            try {
                val res = api.setRequestPrice(
                    requestId = requestId,
                    body = SetRequestPriceBody(providerAmount = amount)
                )

                if (!res.isSuccessful) {
                    throw Exception("Failed to send quotation (${res.code()})")
                }

                quotationAmount = amount
                currentStatus = "quotation_sent"
                success = "Quotation sent successfully"
                loadData()
            } catch (e: Throwable) {
                error = e.message ?: "Failed to send quotation"
            } finally {
                actionLoading = false
            }
        }
    }

    fun markProviderDone() {
        scope.launch {
            actionLoading = true
            error = null
            success = null

            try {
                val response = api.markProviderComplete(requestId)
                if (!response.isSuccessful) {
                    throw Exception("Failed to mark job done (${response.code()})")
                }

                currentStatus = normalizedStatus(response.body()?.request?.status ?: "provider_done")
                success = response.body()?.message ?: "Job marked done"
                loadData()
            } catch (e: Throwable) {
                error = e.message ?: "Failed to mark job done"
            } finally {
                actionLoading = false
            }
        }
    }

    LaunchedEffect(requestId) {
        loadData()
    }

    LaunchedEffect(currentStatus, paymentStatus) {
        val status = normalizedStatus(currentStatus)
        val payState = normalizedPaymentStatus(paymentStatus)

        if (
            status in listOf("accepted", "started", "arrived", "quotation_sent", "provider_done") ||
            (status == "quotation_sent" && payState == "paid")
        ) {
            startLiveLocationUpdates()
        } else {
            stopLiveLocationUpdates()
        }
    }

    LaunchedEffect(requestId) {
        val token = tokenStore.getToken()
        if (token.isNullOrBlank()) return@LaunchedEffect

        SocketManager.connect(
            token = token,
            onConnected = {
                trackingStatus = "Tracking connected"
                SocketManager.joinRequestRoom(requestId)
            },
            onError = { msg ->
                trackingStatus = "Socket error: $msg"
            }
        )

        SocketManager.listenRequestUpdated {
            scope.launch { loadData() }
        }

        SocketManager.listenProviderLocation { _, _, _, incomingRequestId ->
            if (incomingRequestId == requestId || incomingRequestId == null) {
                scope.launch { loadData() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopLiveLocationUpdates()
            SocketManager.clearTrackingListeners()
        }
    }

    val status = normalizedStatus(currentStatus)
    val payState = normalizedPaymentStatus(paymentStatus)

    val canAccept = status == "pending"
    val canStartJob = status == "accepted"
    val canArrive = status == "started"
    val canSendQuotation = status == "arrived"
    val canMarkDone =
        payState == "paid" &&
                status !in listOf("provider_done", "completed", "cancelled")

    val progressValue = when {
        status == "pending" -> 0.10f
        status == "accepted" -> 0.25f
        status == "started" -> 0.45f
        status == "arrived" -> 0.60f
        status == "quotation_sent" && payState != "paid" -> 0.75f
        status == "quotation_sent" && payState == "paid" -> 0.88f
        status == "provider_done" -> 0.96f
        status == "completed" -> 1f
        else -> 0.10f
    }

    val serviceName = serviceDisplayName(request?.service ?: request?.providerType)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Active Job",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryColor
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (loading) {
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth(),
                    color = SkyBlueDark,
                    trackColor = SkyBlueLight
                )
            } else {
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth(),
                    color = SkyBlueDark,
                    trackColor = SkyBlueLight
                )
            }

            error?.let {
                PremiumStatusCard(
                    title = "Error",
                    body = it,
                    color = DangerRedLight
                )
            }

            success?.let {
                PremiumStatusCard(
                    title = "Success",
                    body = it,
                    color = OnlineGreenLight
                )
            }

            HeroSummaryCard(
                serviceName = serviceName,
                userName = userName ?: "-",
                userPhone = userPhone ?: "-",
                status = formatStatus(status),
                paymentStatus = formatPaymentStatus(payState),
                tracking = trackingStatus
            )

            PremiumSectionCard(
                title = "Location & Navigation",
                icon = Icons.Default.LocationOn,
                containerColor = SkyBlueLight
            ) {
                KeyValueRow("Pickup", pickupLabel)
                KeyValueRow("Current Status", formatStatus(status))
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        navController.navigate(
                            Routes.ProviderMapScreen.createRoute(
                                requestId = requestId,
                                pickupLat = pickupLat,
                                pickupLng = pickupLng
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueDark),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Live Map", fontWeight = FontWeight.Bold)
                }
            }

            PremiumSectionCard(
                title = "Job Flow",
                icon = Icons.Default.Work
            ) {
                Text(
                    "Follow only this order: Accept → Start Job → Arrived → Send Quotation → after payment click Job Done.",
                    color = TextSecondaryColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { acceptRequest() },
                        enabled = canAccept && !actionLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAccept) SkyBlueDark else OfflineGray
                        )
                    ) {
                        if (actionLoading && canAccept) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Accept", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { updateSimpleStatus("started", "Job started") },
                        enabled = canStartJob && !actionLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canStartJob) SkyBlueDark else OfflineGray
                        )
                    ) {
                        if (actionLoading && canStartJob) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Start Job", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { updateSimpleStatus("arrived", "Provider arrived") },
                    enabled = canArrive && !actionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canArrive) PendingOrange else OfflineGray
                    )
                ) {
                    if (actionLoading && canArrive) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Arrived", fontWeight = FontWeight.Bold)
                    }
                }
            }

            PremiumSectionCard(
                title = "Quotation",
                icon = Icons.Default.Payments
            ) {
                Text(
                    "After analysis, enter the quotation amount and send it to the user.",
                    color = TextSecondaryColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quotationAmountInput,
                    onValueChange = { quotationAmountInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quotation Amount") },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (quotationAmount > 0.0) {
                    QuotationPreviewCard(
                        amount = quotationAmount,
                        paymentStatus = payState
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = { sendQuotation() },
                    enabled = canSendQuotation && !actionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSendQuotation) SkyBlueDark else OfflineGray
                    )
                ) {
                    if (actionLoading && canSendQuotation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Send Quotation", fontWeight = FontWeight.Bold)
                    }
                }
            }

            PremiumSectionCard(
                title = "Completion",
                icon = Icons.Default.Info,
                containerColor = SurfaceVariantColor
            ) {
                Text(
                    text = when {
                        payState == "paid" && status !in listOf("provider_done", "completed") ->
                            "Payment has been made. You can now click Job Done."
                        status == "quotation_sent" && payState != "paid" ->
                            "Wait for the customer to pay after viewing the quotation."
                        status == "provider_done" ->
                            "You already marked this job as done. Waiting for user confirmation."
                        status == "completed" ->
                            "This job has been fully completed."
                        else ->
                            "Job Done becomes available only after payment."
                    },
                    color = TextSecondaryColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { markProviderDone() },
                    enabled = canMarkDone && !actionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canMarkDone) OnlineGreen else OfflineGray
                    )
                ) {
                    if (actionLoading && canMarkDone) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Job Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    serviceName: String,
    userName: String,
    userPhone: String,
    status: String,
    paymentStatus: String,
    tracking: String
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = serviceName,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimaryColor,
                style = MaterialTheme.typography.titleLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniInfoPill(icon = Icons.Default.Person, text = userName)
                MiniInfoPill(icon = Icons.Default.Info, text = userPhone)
            }

            HorizontalDivider()

            KeyValueRow("Status", status)
            KeyValueRow("Payment", paymentStatus)
            KeyValueRow("Tracking", tracking)
        }
    }
}

@Composable
private fun PremiumStatusCard(
    title: String,
    body: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryColor
            )
            Text(
                text = body,
                color = TextPrimaryColor
            )
        }
    }
}

@Composable
private fun PremiumSectionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color = SurfaceColor,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SkyBlueDark
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun QuotationPreviewCard(
    amount: Double,
    paymentStatus: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SkyBlueLight)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyValueRow("Quoted Amount", formatUgx(amount))
            KeyValueRow("Payment Status", formatPaymentStatus(paymentStatus))
        }
    }
}

@Composable
private fun MiniInfoPill(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .background(SkyBlueLight, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SkyBlueDark
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = TextPrimaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondaryColor)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            color = TextPrimaryColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun serviceDisplayName(value: String?): String {
    return when (value?.trim()?.lowercase()) {
        "garage" -> "Garage Service"
        "fuel" -> "Fuel Delivery"
        "towing" -> "Towing Service"
        "ambulance" -> "Ambulance Service"
        else -> value?.replaceFirstChar { it.uppercase() } ?: "Service Request"
    }
}

private fun formatStatus(value: String?): String {
    return value
        ?.replace("_", " ")
        ?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        ?: "Unknown"
}

private fun formatPaymentStatus(value: String?): String {
    return when (value?.trim()?.lowercase()) {
        "paid" -> "Paid"
        "unpaid" -> "Unpaid"
        else -> formatStatus(value)
    }
}

private fun formatUgx(value: Double): String {
    return try {
        "UGX ${NumberFormat.getNumberInstance(Locale.US).format(value)}"
    } catch (_: Throwable) {
        "UGX $value"
    }
}

private suspend fun getLocationName(
    context: Context,
    lat: Double,
    lng: Double
): String = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = geocoder.getFromLocation(lat, lng, 1)
        val address = results?.firstOrNull()
        address?.getAddressLine(0) ?: "$lat, $lng"
    } catch (_: Throwable) {
        "$lat, $lng"
    }
}