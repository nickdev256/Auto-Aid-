package com.project.auto_aid.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.CreatePaymentBody
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val AutoAidBlue = Color(0xFF1DA1F2)
private val AutoAidDark = Color(0xFF114B5F)
private val ScreenBg = Color(0xFFF5F9FC)
private val CardBg = Color.White
private val SoftBlue = Color(0xFFEAF6FF)
private val SoftBorder = Color(0xFFDCEAF5)
private val SuccessGreen = Color(0xFF20B26B)
private val SuccessGreenBg = Color(0xFFEAF8F1)
private val PendingAmber = Color(0xFFF59E0B)
private val PendingAmberBg = Color(0xFFFFF4DB)
private val DangerRed = Color(0xFFE53935)
private val DangerRedBg = Color(0xFFFDECEC)
private val MutedText = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    navController: NavHostController,
    requestId: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var request by remember { mutableStateOf<RequestDto?>(null) }
    var quote by remember { mutableStateOf<RequestQuoteDto?>(null) }

    var currentStatus by remember { mutableStateOf("pending") }
    var paymentStatus by remember { mutableStateOf("unpaid") }

    var userPhone by remember { mutableStateOf("") }
    var providerName by remember { mutableStateOf("Service Provider") }
    var serviceName by remember { mutableStateOf("AutoAid Service") }
    var problemText by remember { mutableStateOf("-") }
    var locationLabel by remember { mutableStateOf("Location not available") }

    var quotationAmount by remember { mutableDoubleStateOf(0.0) }
    var quotationNote by remember { mutableStateOf("") }

    var showQuotation by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }

    var quotationAcceptedLocally by remember { mutableStateOf(false) }

    fun normalizedStatus(value: String?): String {
        return value?.trim()?.lowercase()?.replace(" ", "_") ?: "pending"
    }

    fun normalizedPaymentStatus(value: String?): String {
        return value?.trim()?.lowercase()?.replace(" ", "_") ?: "unpaid"
    }

    fun serviceDisplayName(value: String?): String {
        return when (value?.trim()?.lowercase()) {
            "fuel" -> "Fuel Delivery"
            "garage" -> "Garage Service"
            "towing" -> "Towing Service"
            "ambulance" -> "Ambulance Service"
            else -> value?.replaceFirstChar { it.uppercase() } ?: "AutoAid Service"
        }
    }

    fun readError(responseCode: Int, errorText: String?, fallback: String): String {
        val clean = errorText?.trim()
        return if (!clean.isNullOrBlank()) clean else "$fallback ($responseCode)"
    }

    fun buildLocationLabel(r: RequestDto): String {
        val lat = r.userLocation?.lat
        val lng = r.userLocation?.lng

        return if (lat != null && lng != null) {
            "Lat: $lat, Lng: $lng"
        } else {
            "Location not available"
        }
    }

    fun loadRequest() {
        scope.launch {
            loading = true
            errorMessage = null

            runCatching {
                val requestRes = api.getRequestById(requestId)
                if (!requestRes.isSuccessful) {
                    throw Exception(
                        readError(
                            requestRes.code(),
                            requestRes.errorBody()?.string(),
                            "Failed to load request"
                        )
                    )
                }

                val r = requestRes.body() ?: throw Exception("Request not found")
                request = r
                currentStatus = normalizedStatus(r.status)
                paymentStatus = normalizedPaymentStatus(r.paymentStatus)
                providerName = r.assignedProviderName ?: "Service Provider"
                serviceName = serviceDisplayName(r.service ?: r.providerType)
                userPhone = r.userPhone ?: ""
                problemText = r.problem ?: r.note ?: "-"
                locationLabel = buildLocationLabel(r)

                val quoteRes = api.getRequestQuote(requestId)
                if (quoteRes.isSuccessful) {
                    val q = quoteRes.body()
                    quote = q
                    quotationAmount = when {
                        (q?.totalAmount ?: 0.0) > 0 -> q?.totalAmount ?: 0.0
                        (q?.providerAmount ?: 0.0) > 0 -> q?.providerAmount ?: 0.0
                        else -> 0.0
                    }
                    quotationNote = q?.pricingStatus ?: ""

                    if (paymentStatus == "paid") {
                        quotationAcceptedLocally = true
                    }
                } else {
                    quote = null
                    quotationAmount = when {
                        (r.totalAmount ?: 0.0) > 0 -> r.totalAmount ?: 0.0
                        (r.quotedAmount ?: 0.0) > 0 -> r.quotedAmount ?: 0.0
                        (r.quoteAmount ?: 0.0) > 0 -> r.quoteAmount ?: 0.0
                        (r.providerAmount ?: 0.0) > 0 -> r.providerAmount ?: 0.0
                        (r.finalAmount ?: 0.0) > 0 -> r.finalAmount ?: 0.0
                        (r.agreedAmount ?: 0.0) > 0 -> r.agreedAmount ?: 0.0
                        else -> 0.0
                    }
                    quotationNote = r.pricingStatus ?: ""

                    if (paymentStatus == "paid") {
                        quotationAcceptedLocally = true
                    }
                }
            }.onFailure {
                errorMessage = it.message ?: "Failed to load request details"
            }

            loading = false
        }
    }

    fun acceptQuotation() {
        if (quotationAcceptedLocally || actionLoading) return

        scope.launch {
            actionLoading = true
            errorMessage = null
            successMessage = null

            runCatching {
                val response = api.acceptQuotation(requestId)
                if (!response.isSuccessful) {
                    throw Exception(
                        readError(
                            response.code(),
                            response.errorBody()?.string(),
                            "Failed to accept quotation"
                        )
                    )
                }

                quotationAcceptedLocally = true
                showQuotation = true
                successMessage = "Quotation accepted"
                loadRequest()
            }.onFailure {
                errorMessage = it.message ?: "Failed to accept quotation"
            }

            actionLoading = false
        }
    }

    fun payNow() {
        if (phoneNumber.trim().isBlank()) {
            errorMessage = "Enter phone number"
            successMessage = null
            return
        }

        if (quotationAmount <= 0.0) {
            errorMessage = "Quotation amount is missing"
            successMessage = null
            return
        }

        scope.launch {
            actionLoading = true
            errorMessage = null
            successMessage = null

            runCatching {
                val response = api.createPayment(
                    CreatePaymentBody(
                        requestId = requestId,
                        amount = quotationAmount,
                        method = "mobile_money",
                        phoneNumber = phoneNumber.trim()
                    )
                )

                if (!response.isSuccessful) {
                    throw Exception(
                        readError(
                            response.code(),
                            response.errorBody()?.string(),
                            "Payment failed"
                        )
                    )
                }

                delay(700)

                paymentStatus = "paid"
                quotationAcceptedLocally = true
                successMessage = "Payment sent successfully"
                loadRequest()
            }.onFailure {
                errorMessage = it.message ?: "Payment failed"
            }

            actionLoading = false
        }
    }

    fun markUserDone() {
        scope.launch {
            actionLoading = true
            errorMessage = null
            successMessage = null

            runCatching {
                val response = api.confirmUserComplete(requestId)
                if (!response.isSuccessful) {
                    throw Exception(
                        readError(
                            response.code(),
                            response.errorBody()?.string(),
                            "Failed to complete request"
                        )
                    )
                }

                currentStatus = "completed"
                successMessage = "Job completed successfully"
                loadRequest()
            }.onFailure {
                errorMessage = it.message ?: "Failed to complete request"
            }

            actionLoading = false
        }
    }

    LaunchedEffect(requestId) {
        loadRequest()
    }

    val status = normalizedStatus(currentStatus)
    val payState = normalizedPaymentStatus(paymentStatus)

    val canViewQuotation = status in listOf(
        "quotation_sent",
        "provider_done",
        "completed"
    ) || payState == "paid"

    val canAcceptQuotation =
        status == "quotation_sent" &&
                !quotationAcceptedLocally &&
                payState != "paid"

    val canPay =
        status == "quotation_sent" &&
                quotationAcceptedLocally &&
                payState != "paid"

    val canUserDone = status == "provider_done"

    val progressValue = when {
        status == "pending" -> 0.10f
        status == "accepted" -> 0.20f
        status == "started" -> 0.35f
        status == "arrived" -> 0.50f
        status == "quotation_sent" && payState != "paid" && !quotationAcceptedLocally -> 0.65f
        status == "quotation_sent" && (quotationAcceptedLocally || payState == "paid") -> 0.80f
        status == "provider_done" -> 0.95f
        status == "completed" -> 1f
        else -> 0.10f
    }

    val bannerTitle = when {
        status == "quotation_sent" && !quotationAcceptedLocally && payState != "paid" ->
            "Quotation Available"
        status == "quotation_sent" && quotationAcceptedLocally && payState != "paid" ->
            "Quotation Accepted"
        payState == "paid" && status != "provider_done" && status != "completed" ->
            "Payment Sent"
        status == "provider_done" ->
            "Provider Finished"
        status == "completed" ->
            "Completed"
        else ->
            "Request in Progress"
    }

    val bannerBody = when {
        status == "quotation_sent" && !quotationAcceptedLocally && payState != "paid" ->
            "Your provider has sent a quotation. Tap View Quotation."
        status == "quotation_sent" && quotationAcceptedLocally && payState != "paid" ->
            "Enter your phone number. The quotation amount is already filled."
        payState == "paid" && status != "provider_done" && status != "completed" ->
            "Payment has been sent. Wait for the provider to click Job Done."
        status == "provider_done" ->
            "The provider marked the job done. You can now click Job Done."
        status == "completed" ->
            "This request has been fully completed."
        else ->
            "Track your request progress below."
    }

    val bannerType = when {
        status == "completed" -> BannerType.SUCCESS
        status == "provider_done" -> BannerType.SUCCESS
        status == "quotation_sent" -> BannerType.WARNING
        payState == "paid" -> BannerType.INFO
        else -> BannerType.INFO
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Request Details",
                        color = AutoAidDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AutoAidBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CardBg
                )
            )
        },
        bottomBar = {
            Surface(
                color = CardBg,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        canViewQuotation && !showQuotation -> {
                            Button(
                                onClick = { showQuotation = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AutoAidBlue)
                            ) {
                                Text("View Quotation", fontWeight = FontWeight.Bold)
                            }
                        }

                        canAcceptQuotation && showQuotation -> {
                            Button(
                                onClick = { acceptQuotation() },
                                enabled = !actionLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AutoAidBlue)
                            ) {
                                if (actionLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Accept Quotation", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        canPay -> {
                            Button(
                                onClick = { payNow() },
                                enabled = !actionLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AutoAidBlue)
                            ) {
                                if (actionLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Pay", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        canUserDone -> {
                            Button(
                                onClick = { markUserDone() },
                                enabled = !actionLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                if (actionLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
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
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AutoAidBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth(),
                    color = AutoAidBlue,
                    trackColor = SoftBlue
                )

                StatusBanner(
                    title = bannerTitle,
                    body = bannerBody,
                    type = bannerType
                )

                successMessage?.let { message ->
                    StatusBanner("Success", message, BannerType.SUCCESS)
                }

                errorMessage?.let { message ->
                    StatusBanner("Error", message, BannerType.ERROR)
                }

                RequestHeroCard(
                    serviceName = serviceName,
                    providerName = providerName,
                    status = formatStatus(status),
                    paymentStatus = formatPaymentStatus(payState)
                )

                InfoSectionCard(
                    title = "Request Information",
                    icon = Icons.Default.ReceiptLong
                ) {
                    InfoRow("Request ID", requestId)
                    InfoRow("Service", serviceName)
                    InfoRow("Problem", problemText)
                    InfoRow("Phone", userPhone.ifBlank { "-" })
                }

                InfoSectionCard(
                    title = "Location",
                    icon = Icons.Default.LocationOn,
                    containerColor = SoftBlue
                ) {
                    Text(
                        text = locationLabel,
                        color = AutoAidDark
                    )
                }

                if (
                    showQuotation ||
                    canPay ||
                    payState == "paid" ||
                    status in listOf("provider_done", "completed") ||
                    quotationAcceptedLocally
                ) {
                    InfoSectionCard(
                        title = "Quotation",
                        icon = Icons.Default.Payments
                    ) {
                        InfoRow("Provider", providerName)
                        InfoRow("Amount", formatUgx(quotationAmount), true)

                        val quoteStateLabel = when {
                            payState == "paid" -> "Paid"
                            quotationAcceptedLocally -> "Accepted"
                            quotationNote.isNotBlank() -> quotationNote
                            else -> "Quoted"
                        }

                        InfoRow("Status", quoteStateLabel)

                        if (canPay) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { newValue -> phoneNumber = newValue },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = formatUgx(quotationAmount),
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Amount") },
                                readOnly = true,
                                enabled = false,
                                singleLine = true
                            )
                        }
                    }
                }

                InfoSectionCard(
                    title = "Flow",
                    icon = Icons.Default.Info,
                    containerColor = SoftBlue
                ) {
                    StepText("1. Provider accepts request")
                    StepText("2. Provider clicks Start Job")
                    StepText("3. Provider clicks Arrived")
                    StepText("4. Provider sends quotation")
                    StepText("5. User clicks View Quotation")
                    StepText("6. User accepts quotation")
                    StepText("7. User enters phone number")
                    StepText("8. Amount stays auto-filled")
                    StepText("9. User clicks Pay")
                    StepText("10. Provider receives payment and clicks Job Done")
                    StepText("11. User confirms completion by clicking Job Done")
                }
            }
        }
    }
}

private enum class BannerType {
    SUCCESS, WARNING, ERROR, INFO
}

@Composable
private fun StatusBanner(
    title: String,
    body: String,
    type: BannerType
) {
    val bg = when (type) {
        BannerType.SUCCESS -> SuccessGreenBg
        BannerType.WARNING -> PendingAmberBg
        BannerType.ERROR -> DangerRedBg
        BannerType.INFO -> SoftBlue
    }

    val fg = when (type) {
        BannerType.SUCCESS -> SuccessGreen
        BannerType.WARNING -> PendingAmber
        BannerType.ERROR -> DangerRed
        BannerType.INFO -> AutoAidBlue
    }

    val icon = when (type) {
        BannerType.SUCCESS -> Icons.Default.CheckCircle
        BannerType.WARNING -> Icons.Default.WarningAmber
        BannerType.ERROR -> Icons.Default.WarningAmber
        BannerType.INFO -> Icons.Default.Info
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = fg)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = AutoAidDark, fontWeight = FontWeight.Bold)
                Text(body, color = AutoAidDark)
            }
        }
    }
}

@Composable
private fun RequestHeroCard(
    serviceName: String,
    providerName: String,
    status: String,
    paymentStatus: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = serviceName,
                color = AutoAidDark,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniPill(Icons.Default.Person, providerName)
                MiniPill(Icons.Default.Info, status)
            }

            Divider(color = SoftBorder)
            InfoRow("Current Status", status)
            InfoRow("Payment", paymentStatus)
        }
    }
}

@Composable
private fun MiniPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .background(SoftBlue, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AutoAidBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = AutoAidDark, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color = CardBg,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AutoAidBlue)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    title,
                    color = AutoAidDark,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedText)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            color = if (highlight) AutoAidBlue else AutoAidDark,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun StepText(text: String) {
    Text(text = text, color = AutoAidDark)
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