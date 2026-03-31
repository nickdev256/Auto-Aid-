package com.project.auto_aid.settings

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
import androidx.compose.material.icons.filled.PhoneAndroid
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
fun PayoutInformationScreen(
    navController: NavHostController,
    requestId: String = "",
    providerName: String = "Service Provider",
    serviceName: String = "Service Request",
    amount: Double = 0.0
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var quoteLoading by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }

    var phoneNumber by remember { mutableStateOf("") }
    var quotationAmount by remember { mutableDoubleStateOf(amount) }
    var pricingStatus by remember { mutableStateOf("not_set") }
    var paymentStatus by remember { mutableStateOf("unpaid") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun loadQuotation() {
        if (requestId.isBlank()) {
            quotationAmount = amount
            pricingStatus = if (amount > 0) "quotation_sent" else "not_set"
            return
        }

        scope.launch {
            quoteLoading = true
            errorMessage = null

            runCatching {
                val response = api.getRequestQuote(requestId)
                if (!response.isSuccessful) {
                    throw Exception("Failed to load quotation (${response.code()})")
                }

                val q = response.body()
                quotationAmount = when {
                    (q?.totalAmount ?: 0.0) > 0 -> q?.totalAmount ?: 0.0
                    (q?.providerAmount ?: 0.0) > 0 -> q?.providerAmount ?: 0.0
                    else -> amount
                }
                pricingStatus = q?.pricingStatus ?: "quotation_sent"
                paymentStatus = q?.paymentStatus ?: "unpaid"
            }.onFailure {
                errorMessage = it.message ?: "Failed to load quotation"
                quotationAmount = amount
            }

            quoteLoading = false
        }
    }

    fun payNow() {
        if (phoneNumber.trim().isEmpty()) {
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
            paying = true
            errorMessage = null
            successMessage = null

            runCatching {
                val response = api.createPayment(
                    CreatePaymentBody(
                        requestId = requestId,
                        amount = quotationAmount,
                        method = "airtel_money",
                        phoneNumber = phoneNumber.trim()
                    )
                )

                if (!response.isSuccessful) {
                    throw Exception("Payment failed (${response.code()})")
                }

                delay(800)
                paymentStatus = "paid"
                successMessage = response.body()?.message ?: "Payment sent successfully"
            }.onFailure {
                errorMessage = it.message ?: "Payment failed"
            }

            paying = false
        }
    }

    LaunchedEffect(requestId) {
        loadQuotation()
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "View Quotation",
                        color = AutoAidDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
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
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = { payNow() },
                        enabled = !paying && !quoteLoading && quotationAmount > 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AutoAidBlue)
                    ) {
                        if (paying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Pay", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (quoteLoading) {
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
                StatusBanner(
                    title = when {
                        paymentStatus.equals("paid", true) -> "Payment Sent"
                        quotationAmount > 0 -> "Quotation Ready"
                        else -> "Waiting for Quotation"
                    },
                    body = when {
                        paymentStatus.equals("paid", true) ->
                            "Your payment has been submitted. The provider can now continue and later mark the job done."
                        quotationAmount > 0 ->
                            "Review the quotation below, enter your phone number, and pay."
                        else ->
                            "The provider has not sent a quotation yet."
                    },
                    type = when {
                        paymentStatus.equals("paid", true) -> BannerType.SUCCESS
                        quotationAmount > 0 -> BannerType.INFO
                        else -> BannerType.WARNING
                    }
                )

                successMessage?.let {
                    StatusBanner(
                        title = "Success",
                        body = it,
                        type = BannerType.SUCCESS
                    )
                }

                errorMessage?.let {
                    StatusBanner(
                        title = "Error",
                        body = it,
                        type = BannerType.ERROR
                    )
                }

                PaymentHeroCard(
                    serviceName = serviceName,
                    providerName = providerName,
                    requestId = if (requestId.isBlank()) "N/A" else requestId,
                    amount = quotationAmount,
                    paymentStatus = paymentStatus
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SoftBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = AutoAidBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    "Quotation Summary",
                                    fontWeight = FontWeight.Bold,
                                    color = AutoAidDark,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "This amount is auto-filled from the provider quotation.",
                                    color = MutedText
                                )
                            }
                        }

                        Divider(color = SoftBorder)
                        InfoRow("Service", serviceName)
                        InfoRow("Provider", providerName)
                        InfoRow("Amount", formatUgx(quotationAmount), true)
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SoftBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = AutoAidBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    "Payment Details",
                                    fontWeight = FontWeight.Bold,
                                    color = AutoAidDark,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Enter your phone number. Amount is already filled from quotation.",
                                    color = MutedText
                                )
                            }
                        }

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        OutlinedTextField(
                            value = formatUgx(quotationAmount),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Amount") },
                            readOnly = true,
                            enabled = false
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Flow",
                            fontWeight = FontWeight.Bold,
                            color = AutoAidDark,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("1. Provider sends quotation", color = AutoAidDark)
                        Text("2. User enters phone number", color = AutoAidDark)
                        Text("3. Amount stays auto-filled", color = AutoAidDark)
                        Text("4. User clicks Pay", color = AutoAidDark)
                        Text("5. Provider later clicks Job Done", color = AutoAidDark)
                        Text("6. User finally clicks Job Done", color = AutoAidDark)
                    }
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
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = AutoAidDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = body,
                    color = AutoAidDark
                )
            }
        }
    }
}

@Composable
private fun PaymentHeroCard(
    serviceName: String,
    providerName: String,
    requestId: String,
    amount: Double,
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

            Text(
                text = "Provider: $providerName",
                color = MutedText
            )

            InfoRow("Request ID", requestId)
            InfoRow("Quotation", formatUgx(amount), true)
            InfoRow("Payment Status", prettyStatus(paymentStatus))
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
        Text(text = label, color = MutedText)
        Text(
            text = value,
            color = if (highlight) AutoAidBlue else AutoAidDark,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun formatUgx(value: Double): String {
    return try {
        "UGX ${NumberFormat.getNumberInstance(Locale.US).format(value)}"
    } catch (_: Throwable) {
        "UGX $value"
    }
}

private fun prettyStatus(value: String?): String {
    return value
        ?.replace("_", " ")
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        ?: "Unknown"
}