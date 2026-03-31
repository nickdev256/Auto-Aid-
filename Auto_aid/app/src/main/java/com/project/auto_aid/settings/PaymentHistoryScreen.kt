package com.project.auto_aid.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.PaymentHistoryDto
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private val AutoAidBlue = Color(0xFF1DA1F2)
private val AutoAidDark = Color(0xFF114B5F)
private val ScreenBg = Color(0xFFF5F9FC)
private val CardBg = Color.White
private val SoftBlue = Color(0xFFEAF6FF)
private val SuccessGreen = Color(0xFF20B26B)
private val SuccessGreenBg = Color(0xFFEAF8F1)
private val PendingAmber = Color(0xFFF59E0B)
private val PendingAmberBg = Color(0xFFFFF4DB)
private val DangerRed = Color(0xFFE53935)
private val DangerRedBg = Color(0xFFFDECEC)
private val MutedText = Color(0xFF6B7280)
private val SoftGray = Color(0xFFF4F8FB)

private enum class PaymentFilter {
    ALL, PAID, PENDING, CASH, WALLET, AIRTEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    navController: NavHostController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    val payments = remember { mutableStateListOf<PaymentHistoryDto>() }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(PaymentFilter.ALL) }

    fun loadHistory() {
        scope.launch {
            loading = true
            error = null

            runCatching {
                val response = api.getPaymentHistory(limit = 100, page = 1)

                if (!response.isSuccessful) {
                    val message = response.errorBody()?.string()
                    throw Exception(message ?: "Failed to load payment history (${response.code()})")
                }

                val historyList = response.body().orEmpty()

                payments.clear()
                payments.addAll(historyList)
            }.onFailure {
                error = it.message ?: "Failed to load payment history"
            }

            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHistory()
    }

    val filteredPayments: List<PaymentHistoryDto> = payments
        .filter { item ->
            when (selectedFilter) {
                PaymentFilter.ALL -> true
                PaymentFilter.PAID -> {
                    item.paymentStatus.equals("paid", true) ||
                            item.paymentConfirmedByProvider == true
                }
                PaymentFilter.PENDING -> {
                    item.paymentStatus.equals("pending", true) ||
                            item.paymentStatus.equals("unpaid", true)
                }
                PaymentFilter.CASH -> item.method.equals("cash", true)
                PaymentFilter.WALLET -> item.method.equals("wallet", true)
                PaymentFilter.AIRTEL -> {
                    item.method.equals("airtel_money", true) ||
                            item.method.equals("airtel", true)
                }
            }
        }
        .sortedByDescending { parseServerDateToMillis(it.createdAt) }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Payment History",
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
        }
    ) { padding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AutoAidBlue)
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    StatusCard(
                        title = "Error",
                        body = error ?: "Something went wrong",
                        background = DangerRedBg,
                        foreground = DangerRed
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { loadHistory() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    PaymentHistorySummaryCard(
                        totalPayments = payments.size,
                        paidCount = payments.count {
                            it.paymentStatus.equals("paid", true) ||
                                    it.paymentConfirmedByProvider == true
                        },
                        pendingCount = payments.count {
                            it.paymentStatus.equals("pending", true) ||
                                    it.paymentStatus.equals("unpaid", true)
                        }
                    )

                    FilterRow(
                        selected = selectedFilter,
                        onSelected = { selectedFilter = it }
                    )

                    if (filteredPayments.isEmpty()) {
                        EmptyStateCard()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredPayments,
                                key = { item ->
                                    item.requestId
                                        ?: item.id
                                        ?: item._id
                                        ?: item.reference
                                        ?: item.createdAt
                                        ?: item.hashCode().toString()
                                }
                            ) { item: PaymentHistoryDto ->
                                PaymentHistoryItemCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistorySummaryCard(
    totalPayments: Int,
    paidCount: Int,
    pendingCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SoftBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = AutoAidBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Your Payments",
                        fontWeight = FontWeight.Bold,
                        color = AutoAidDark,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Track all previous payments",
                        color = MutedText
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMiniChip("Total", totalPayments.toString())
                SummaryMiniChip("Paid", paidCount.toString())
                SummaryMiniChip("Pending", pendingCount.toString())
            }
        }
    }
}

@Composable
private fun SummaryMiniChip(
    label: String,
    value: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = AutoAidDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
    selected: PaymentFilter,
    onSelected: (PaymentFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == PaymentFilter.ALL,
            onClick = { onSelected(PaymentFilter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selected == PaymentFilter.PAID,
            onClick = { onSelected(PaymentFilter.PAID) },
            label = { Text("Paid") }
        )
        FilterChip(
            selected = selected == PaymentFilter.PENDING,
            onClick = { onSelected(PaymentFilter.PENDING) },
            label = { Text("Pending") }
        )
        FilterChip(
            selected = selected == PaymentFilter.AIRTEL,
            onClick = { onSelected(PaymentFilter.AIRTEL) },
            label = { Text("Airtel") }
        )
        FilterChip(
            selected = selected == PaymentFilter.WALLET,
            onClick = { onSelected(PaymentFilter.WALLET) },
            label = { Text("Wallet") }
        )
        FilterChip(
            selected = selected == PaymentFilter.CASH,
            onClick = { onSelected(PaymentFilter.CASH) },
            label = { Text("Cash") }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PaymentHistoryItemCard(
    item: PaymentHistoryDto
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        .background(methodBackground(item.method)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = methodIcon(item.method),
                        contentDescription = null,
                        tint = methodColor(item.method)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.serviceName ?: "Service Payment",
                        fontWeight = FontWeight.Bold,
                        color = AutoAidDark
                    )
                    Text(
                        text = "Provider: ${item.providerName ?: "-"}",
                        color = MutedText
                    )
                }

                PaymentStatusPill(
                    paymentStatus = item.paymentStatus,
                    paymentConfirmed = item.paymentConfirmedByProvider == true
                )
            }

            InfoRow("Amount", formatUgx(item.amount ?: 0.0), true)
            InfoRow("Method", paymentMethodLabel(item.method))
            InfoRow("Request ID", item.requestId ?: item.id ?: item._id ?: "-")
            InfoRow("Reference", item.reference?.takeIf { it.isNotBlank() } ?: "-")
            InfoRow("Date", parseServerDateToDisplay(item.createdAt))
            InfoRow(
                "Provider Confirmation",
                if (item.paymentConfirmedByProvider == true) "Confirmed" else "Pending"
            )
        }
    }
}

@Composable
private fun PaymentStatusPill(
    paymentStatus: String?,
    paymentConfirmed: Boolean
) {
    val bg: Color
    val fg: Color
    val text: String

    when {
        paymentConfirmed -> {
            bg = SuccessGreenBg
            fg = SuccessGreen
            text = "Confirmed"
        }
        paymentStatus.equals("paid", true) -> {
            bg = SoftBlue
            fg = AutoAidBlue
            text = "Paid"
        }
        paymentStatus.equals("pending", true) -> {
            bg = PendingAmberBg
            fg = PendingAmber
            text = "Pending"
        }
        paymentStatus.equals("unpaid", true) -> {
            bg = PendingAmberBg
            fg = PendingAmber
            text = "Unpaid"
        }
        else -> {
            bg = SoftGray
            fg = MutedText
            text = paymentStatus?.prettyStatus() ?: "Unknown"
        }
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    background: Color,
    foreground: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = foreground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                color = AutoAidDark
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No payment history found",
                    fontWeight = FontWeight.Bold,
                    color = AutoAidDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your previous payments will appear here.",
                    color = MutedText
                )
            }
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

private fun paymentMethodLabel(value: String?): String {
    return when (value?.trim()?.lowercase()) {
        "airtel_money" -> "Airtel Money"
        "airtel" -> "Airtel"
        "wallet" -> "Wallet"
        "cash" -> "Cash"
        else -> "Not set"
    }
}

private fun methodIcon(method: String?): ImageVector {
    return when (method?.trim()?.lowercase()) {
        "airtel_money", "airtel" -> Icons.Default.PhoneAndroid
        "wallet" -> Icons.Default.AccountBalanceWallet
        "cash" -> Icons.Default.LocalAtm
        else -> Icons.Default.ReceiptLong
    }
}

private fun methodBackground(method: String?): Color {
    return when (method?.trim()?.lowercase()) {
        "airtel_money", "airtel" -> SoftBlue
        "wallet" -> SuccessGreenBg
        "cash" -> PendingAmberBg
        else -> SoftGray
    }
}

private fun methodColor(method: String?): Color {
    return when (method?.trim()?.lowercase()) {
        "airtel_money", "airtel" -> AutoAidBlue
        "wallet" -> SuccessGreen
        "cash" -> PendingAmber
        else -> MutedText
    }
}

private fun formatUgx(value: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${fmt.format(value.roundToInt())}"
}

private fun parseServerDateToDisplay(value: String?): String {
    if (value.isNullOrBlank()) return "-"

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    for (pattern in patterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.getDefault())
            val date = parser.parse(value) ?: continue
            return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
        } catch (_: Exception) {
        }
    }

    return value
}

private fun parseServerDateToMillis(value: String?): Long {
    if (value.isNullOrBlank()) return Long.MIN_VALUE

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    for (pattern in patterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.getDefault())
            val date = parser.parse(value)
            if (date != null) return date.time
        } catch (_: Exception) {
        }
    }

    return Long.MIN_VALUE
}

private fun String.prettyStatus(): String {
    return replace("_", " ")
        .trim()
        .replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
}