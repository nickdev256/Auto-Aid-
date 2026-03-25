package com.project.auto_aid.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.CreatePaymentBody
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    navController: NavHostController,
    requestId: String
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var paying by remember { mutableStateOf(false) }
    var confirmingCompletion by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var request by remember { mutableStateOf<RequestDto?>(null) }
    var quote by remember { mutableStateOf<RequestQuoteDto?>(null) }

    var paymentMessage by remember { mutableStateOf<String?>(null) }
    var completionMessage by remember { mutableStateOf<String?>(null) }

    fun openLiveTracking(serviceKey: String) {
        when (serviceKey) {
            "garage" -> navController.navigate(Routes.GarageActiveScreen.createRoute(requestId))
            "towing" -> navController.navigate(Routes.TowingActiveScreen.createRoute(requestId))
            "fuel" -> navController.navigate(Routes.FuelActiveScreen.createRoute(requestId))
            "ambulance" -> navController.navigate(Routes.AmbulanceActiveScreen.createRoute(requestId))
        }
    }

    fun loadData() {
        scope.launch {
            loading = true
            error = null

            try {
                val requestRes = api.getRequestById(requestId)
                if (!requestRes.isSuccessful) {
                    throw Exception("Failed to load request (HTTP ${requestRes.code()})")
                }

                val requestBody = requestRes.body()
                    ?: throw Exception("Request not found")

                request = requestBody

                val quoteRes = api.getRequestQuote(requestId)
                quote = if (quoteRes.isSuccessful) quoteRes.body() else null
            } catch (e: Throwable) {
                error = e.message ?: "Failed to load request details"
            } finally {
                loading = false
            }
        }
    }

    fun payNow() {
        val total = quote?.totalAmount
            ?: request?.totalAmount
            ?: request?.amount
            ?: request?.price
            ?: 0.0

        if (total <= 0.0) {
            paymentMessage = "Invalid payment amount"
            return
        }

        scope.launch {
            paying = true
            paymentMessage = null
            completionMessage = null

            try {
                val res = api.makePayment(
                    CreatePaymentBody(
                        requestId = requestId,
                        amount = total,
                        method = "mobile_money"
                    )
                )

                if (!res.isSuccessful) {
                    throw Exception("Payment failed (HTTP ${res.code()})")
                }

                paymentMessage = res.body()?.message
                    ?: "Payment successful. Funds are now held in escrow."

                loadData()
            } catch (e: Throwable) {
                paymentMessage = e.message ?: "Payment failed"
            } finally {
                paying = false
            }
        }
    }

    fun confirmJobCompleted() {
        scope.launch {
            confirmingCompletion = true
            completionMessage = null
            paymentMessage = null

            try {
                val res = api.confirmUserComplete(requestId)

                if (!res.isSuccessful) {
                    throw Exception("Failed to confirm completion (HTTP ${res.code()})")
                }

                completionMessage = res.body()?.message
                    ?: "Job completion confirmed successfully."

                loadData()
            } catch (e: Throwable) {
                completionMessage = e.message ?: "Failed to confirm completion"
            } finally {
                confirmingCompletion = false
            }
        }
    }

    LaunchedEffect(requestId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
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
                    CircularProgressIndicator()
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
                    Text(
                        text = error ?: "Error",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { loadData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }
                }
            }

            else -> {
                val r = request ?: return@Scaffold
                val q = quote

                val serviceName = serviceDisplayName(r.service ?: r.providerType)
                val statusText = formatStatus(r.status)
                val requestTime = parseServerDateToDisplay(r.createdAt)
                val serviceKey = normalizeServiceKey(r.service ?: r.providerType)

                val totalAmount = q?.totalAmount
                    ?: r.totalAmount
                    ?: r.amount
                    ?: r.price
                    ?: 0.0

                val quoteExists = totalAmount > 0.0
                val paymentStatusRaw = (r.paymentStatus ?: "unpaid").trim().lowercase()
                val providerCompleted = r.providerCompleted == true
                val userCompleted = r.userCompleted == true
                val activeRequest = isRequestActive(r.status)

                val assignedProviderName = r.assignedProviderName?.takeIf { it.isNotBlank() }
                val assignedProviderPhone = r.assignedProviderPhone?.takeIf { it.isNotBlank() }
                val assignedProviderRating = r.assignedProviderRating
                val targetProviderId = r.targetProviderId?.takeIf { it.isNotBlank() }

                val providerStateText = when {
                    !assignedProviderName.isNullOrBlank() -> "Provider assigned"
                    !targetProviderId.isNullOrBlank() -> "Provider selected, waiting for acceptance"
                    else -> "Waiting for provider"
                }

                val showPayNow =
                    quoteExists &&
                            paymentStatusRaw == "unpaid" &&
                            !userCompleted &&
                            !providerCompleted

                val showConfirmCompletedButton =
                    paymentStatusRaw == "held_in_escrow" &&
                            providerCompleted &&
                            !userCompleted &&
                            activeRequest

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoSectionCard(
                        title = "Request Summary",
                        icon = Icons.Default.ReceiptLong
                    ) {
                        LabelValue("Request ID", r.resolvedId())
                        LabelValue("Status", statusText)
                        LabelValue("Service", serviceName)
                        LabelValue("Created At", requestTime)
                        LabelValue("Problem", r.problem ?: "-")
                        LabelValue("Vehicle Info", r.vehicleInfo ?: "-")

                        if (!r.towType.isNullOrBlank()) {
                            LabelValue("Tow Type", r.towType)
                        }

                        if (!r.note.isNullOrBlank()) {
                            LabelValue("Note", r.note)
                        }

                        if (!r.urgency.isNullOrBlank()) {
                            LabelValue(
                                "Urgency",
                                r.urgency.replaceFirstChar {
                                    if (it.isLowerCase()) {
                                        it.titlecase(Locale.getDefault())
                                    } else {
                                        it.toString()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoSectionCard(
                        title = "Provider Information",
                        icon = Icons.Default.SupportAgent
                    ) {
                        LabelValue("Provider Status", providerStateText)
                        LabelValue("Name", assignedProviderName ?: "-")
                        LabelValue("Phone", assignedProviderPhone ?: "-")
                        LabelValue(
                            "Rating",
                            assignedProviderRating?.toString()?.takeIf { it.isNotBlank() } ?: "-"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoSectionCard(
                        title = "Service Location",
                        icon = Icons.Default.LocationOn
                    ) {
                        LabelValue("Latitude", "${r.userLocation?.lat ?: 0.0}")
                        LabelValue("Longitude", "${r.userLocation?.lng ?: 0.0}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoSectionCard(
                        title = "Payment & Completion",
                        icon = Icons.Default.Lock,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        LabelValue("Total Amount", formatUgx(totalAmount))
                        LabelValue(
                            "Payment Status",
                            paymentStatusLabel(
                                paymentStatus = paymentStatusRaw,
                                providerCompleted = providerCompleted,
                                userCompleted = userCompleted
                            )
                        )
                        LabelValue("Provider Completed", if (providerCompleted) "Yes" else "No")
                        LabelValue("User Completed", if (userCompleted) "Yes" else "No")
                    }

                    if (q != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoSectionCard(
                            title = "Provider Quotation",
                            icon = Icons.Default.Person,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            LabelValue("Provider Charge", formatUgx(q.providerAmount ?: 0.0))
                            LabelValue("System Fee", formatUgx(q.systemFee ?: 0.0))
                            LabelValue("Total User Pays", formatUgx(q.totalAmount ?: 0.0))
                            LabelValue(
                                "Price Set By Provider",
                                if (q.priceSetByProvider == true) "Yes" else "No"
                            )
                        }
                    }

                    if (showPayNow) {
                        Spacer(modifier = Modifier.height(12.dp))

                        StatusMessageCard(
                            title = "System-secured payment",
                            message = "Pay now so your funds are held safely in escrow until the job is completed."
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { payNow() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !paying
                        ) {
                            if (paying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Pay Now")
                            }
                        }
                    }

                    if (paymentStatusRaw == "held_in_escrow" && !providerCompleted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusMessageCard(
                            title = "Payment Held in Escrow",
                            message = "Your payment is secured. Waiting for service completion."
                        )
                    }

                    if (paymentStatusRaw == "held_in_escrow" && providerCompleted && !userCompleted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusMessageCard(
                            title = "Waiting for your confirmation",
                            message = "The provider marked the job as completed. Confirm to release payment."
                        )
                    }

                    if (paymentStatusRaw == "released" && providerCompleted && userCompleted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusMessageCard(
                            title = "Completed - Payment Released",
                            message = "This request is fully completed and payment has been released to the provider."
                        )
                    }

                    paymentMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = if (
                                message.contains("fail", ignoreCase = true) ||
                                message.contains("invalid", ignoreCase = true)
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    completionMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = if (message.contains("fail", ignoreCase = true)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    if (showConfirmCompletedButton) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { confirmJobCompleted() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !confirmingCompletion
                        ) {
                            if (confirmingCompletion) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Confirm Job Completed")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (serviceKey.isNotBlank()) {
                        Button(
                            onClick = { openLiveTracking(serviceKey) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Live Tracking")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { loadData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Refresh")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StatusMessageCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LabelValue(
    label: String,
    value: String
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall
    )
    Text(text = value)
    Spacer(modifier = Modifier.height(10.dp))
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

private fun formatStatus(status: String?): String {
    return when (status?.trim()?.lowercase()) {
        "pending", "request_sent" -> "Pending"
        "assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned" -> "Assigned"
        "driver_on_the_way",
        "mechanic_on_the_way",
        "vendor_on_the_way",
        "ambulance_on_the_way" -> "On Going"
        "arrived" -> "Arrived"
        "in_progress",
        "delivering",
        "patient_picked",
        "vehicle_towed",
        "repaired" -> "On Going"
        "awaiting_dual_confirmation" -> "Waiting for Confirmation"
        "delivered", "at_hospital", "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        "awaiting_payment" -> "Awaiting Payment"
        "quoted" -> "Quoted"
        else -> status?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Unknown"
    }
}

private fun paymentStatusLabel(
    paymentStatus: String?,
    providerCompleted: Boolean,
    userCompleted: Boolean
): String {
    val status = paymentStatus?.trim()?.lowercase() ?: "unpaid"

    return when {
        status == "released" && providerCompleted && userCompleted ->
            "Completed - Payment Released"

        status == "held_in_escrow" && !providerCompleted ->
            "Waiting for Provider Completion"

        status == "held_in_escrow" && providerCompleted && !userCompleted ->
            "Waiting for User Confirmation"

        status == "held_in_escrow" ->
            "Payment Held in Escrow"

        else ->
            "Unpaid"
    }
}

private fun isRequestActive(status: String?): Boolean {
    return when (status?.trim()?.lowercase()) {
        "assigned",
        "arrived",
        "in_progress",
        "quoted",
        "awaiting_payment",
        "awaiting_dual_confirmation",
        "paid",
        "active",
        "ongoing",
        "on going",
        "driver_on_the_way",
        "mechanic_on_the_way",
        "vendor_on_the_way",
        "ambulance_on_the_way",
        "delivering",
        "patient_picked",
        "vehicle_towed",
        "repaired" -> true

        else -> false
    }
}

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "UGX ${formatter.format(amount)}"
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