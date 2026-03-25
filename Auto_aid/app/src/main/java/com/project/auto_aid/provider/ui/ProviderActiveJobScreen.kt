package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.RequestDto
import com.project.auto_aid.data.network.dto.RequestQuoteDto
import com.project.auto_aid.data.network.dto.SetRequestPriceBody
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.provider.ProviderViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderActiveJobScreen(
    requestId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val vm: ProviderViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var request by remember { mutableStateOf<RequestDto?>(null) }
    var quote by remember { mutableStateOf<RequestQuoteDto?>(null) }

    var pickupLat by remember { mutableDoubleStateOf(0.0) }
    var pickupLng by remember { mutableDoubleStateOf(0.0) }

    var currentStatus by remember { mutableStateOf("assigned") }
    var userPhone by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }

    var providerAmountInput by remember { mutableStateOf("") }
    var providerAmount by remember { mutableDoubleStateOf(0.0) }
    var systemFee by remember { mutableDoubleStateOf(0.0) }
    var totalAmount by remember { mutableDoubleStateOf(0.0) }

    var paymentStatus by remember { mutableStateOf("unpaid") }
    var providerCompleted by remember { mutableStateOf(false) }
    var userCompleted by remember { mutableStateOf(false) }

    var quoteLoading by remember { mutableStateOf(false) }
    var quoteMessage by remember { mutableStateOf<String?>(null) }
    var quoteError by remember { mutableStateOf<String?>(null) }

    var completionLoading by remember { mutableStateOf(false) }
    var completionMessage by remember { mutableStateOf<String?>(null) }
    var completionError by remember { mutableStateOf<String?>(null) }

    var priceAlreadySet by remember { mutableStateOf(false) }

    fun localSystemFee(amount: Double): Double {
        return maxOf(3000.0, kotlin.math.round(amount * 0.1))
    }

    fun recalculatePreview() {
        val amount = providerAmountInput.toDoubleOrNull() ?: 0.0
        providerAmount = amount
        systemFee = if (amount > 0) localSystemFee(amount) else 0.0
        totalAmount = if (amount > 0) amount + systemFee else 0.0
    }

    fun loadPickup() {
        scope.launch {
            loading = true
            error = null
            quoteError = null
            completionError = null

            try {
                val res = api.getRequestById(requestId)
                if (!res.isSuccessful) {
                    throw Exception("Failed to load request (HTTP ${res.code()})")
                }

                val r = res.body() ?: throw Exception("Request body is null")
                request = r

                pickupLat = r.userLocation?.lat ?: 0.0
                pickupLng = r.userLocation?.lng ?: 0.0
                currentStatus = r.status ?: "assigned"
                userPhone = r.userPhone
                userName = r.userName
                paymentStatus = (r.paymentStatus ?: "unpaid").trim().lowercase()
                providerCompleted = r.providerCompleted == true
                userCompleted = r.userCompleted == true

                val quoteRes = api.getRequestQuote(requestId)
                if (quoteRes.isSuccessful) {
                    val q = quoteRes.body()
                    quote = q
                    providerAmount = q?.providerAmount ?: 0.0
                    systemFee = q?.systemFee ?: 0.0
                    totalAmount = q?.totalAmount ?: 0.0
                    priceAlreadySet = q?.priceSetByProvider ?: false
                    if (priceAlreadySet && providerAmount > 0) {
                        providerAmountInput = providerAmount.roundToInt().toString()
                    }
                } else {
                    quote = null
                }

                val fallbackTotal = r.totalAmount ?: r.amount ?: r.price ?: 0.0
                if (totalAmount <= 0.0 && fallbackTotal > 0.0) {
                    totalAmount = fallbackTotal
                }

                if (pickupLat == 0.0 && pickupLng == 0.0) {
                    error = "User location is missing for this request."
                }
            } catch (e: Throwable) {
                error = e.message ?: "Failed to load pickup location"
            } finally {
                loading = false
            }
        }
    }

    fun sendQuote() {
        val amount = providerAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            quoteError = "Enter a valid amount"
            quoteMessage = null
            return
        }

        scope.launch {
            quoteLoading = true
            quoteError = null
            quoteMessage = null
            completionMessage = null
            completionError = null

            try {
                val res = api.setRequestPrice(
                    requestId = requestId,
                    body = SetRequestPriceBody(providerAmount = amount)
                )

                if (!res.isSuccessful) {
                    throw Exception("Failed to send quote (HTTP ${res.code()})")
                }

                val body = res.body()
                val quoteRes = body?.request

                providerAmount = quoteRes?.providerAmount ?: amount
                systemFee = quoteRes?.systemFee ?: localSystemFee(amount)
                totalAmount = quoteRes?.totalAmount ?: (providerAmount + systemFee)
                currentStatus = quoteRes?.status ?: "awaiting_payment"
                priceAlreadySet = true
                quoteMessage = body?.message ?: "Quote sent successfully"

                loadPickup()
            } catch (e: Throwable) {
                quoteError = e.message ?: "Failed to send quote"
            } finally {
                quoteLoading = false
            }
        }
    }

    fun markJobCompleted() {
        scope.launch {
            completionLoading = true
            completionMessage = null
            completionError = null

            try {
                val res = api.markProviderComplete(requestId)

                if (!res.isSuccessful) {
                    throw Exception("Failed to mark job completed (HTTP ${res.code()})")
                }

                completionMessage = res.body()?.message ?: "Job marked as completed."
                loadPickup()
            } catch (e: Throwable) {
                completionError = e.message ?: "Failed to mark job completed"
            } finally {
                completionLoading = false
            }
        }
    }

    LaunchedEffect(requestId) {
        loadPickup()
    }

    LaunchedEffect(providerAmountInput) {
        recalculatePreview()
    }

    val serviceName = serviceDisplayName(request?.service ?: request?.providerType)
    val paymentStatusLabel = paymentStatusLabel(
        paymentStatus = paymentStatus,
        providerCompleted = providerCompleted,
        userCompleted = userCompleted
    )
    val activeRequest = isRequestActive(currentStatus)
    val showMarkCompletedButton =
        paymentStatus == "held_in_escrow" &&
                activeRequest &&
                !providerCompleted

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Job") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = "Request ID: $requestId")
            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { loadPickup() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry Loading Location")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (error == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Job Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LabelValue("Service Type", serviceName)
                        LabelValue("Customer", userName ?: "-")
                        LabelValue("User Phone", userPhone ?: "-")
                        LabelValue("Status", formatStatus(currentStatus))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Pickup Location", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Lat: $pickupLat")
                        Text("Lng: $pickupLng")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Payment & Completion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LabelValue("Total Amount", formatUgx(totalAmount))
                        LabelValue("Payment Status", paymentStatusLabel)
                        LabelValue("Provider Completed", if (providerCompleted) "Yes" else "No")
                        LabelValue("User Completed", if (userCompleted) "Yes" else "No")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (paymentStatus == "held_in_escrow" && !providerCompleted) {
                    StatusMessageCard(
                        title = "Paid in Escrow",
                        message = "Customer payment is secured. Mark the job completed when your work is done."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (paymentStatus == "held_in_escrow" && providerCompleted && !userCompleted) {
                    StatusMessageCard(
                        title = "Waiting for customer confirmation",
                        message = "You marked the job as completed. Payment will be released after customer confirmation."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (paymentStatus == "released" && providerCompleted && userCompleted) {
                    StatusMessageCard(
                        title = "Completed - Payment Released",
                        message = "This job is complete and payment has been released."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

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
                enabled = !loading && error == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Map")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        vm.updateStatus(requestId, "arrived")
                        currentStatus = "arrived"
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) {
                    Text("Arrived")
                }

                OutlinedButton(
                    onClick = {
                        vm.updateStatus(requestId, "in_progress")
                        currentStatus = "in_progress"
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !loading && priceAlreadySet
                ) {
                    Text("Start Job")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Set Service Price",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter the provider amount after reaching the site. The system fee is added automatically.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = providerAmountInput,
                        onValueChange = { providerAmountInput = it },
                        label = { Text("Provider Amount (UGX)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !quoteLoading && !providerCompleted && paymentStatus != "held_in_escrow"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Quote Preview", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Provider Charge: ${formatUgx(providerAmount)}")
                            Text("System Fee: ${formatUgx(systemFee)}")
                            Text("Total User Pays: ${formatUgx(totalAmount)}")
                        }
                    }

                    quoteError?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    quoteMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val allowQuoteChange = paymentStatus == "unpaid" && !providerCompleted

                    Button(
                        onClick = { sendQuote() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !quoteLoading && allowQuoteChange
                    ) {
                        if (quoteLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (priceAlreadySet) "Update Quote" else "Send Quote")
                        }
                    }
                }
            }

            completionError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            completionMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            if (showMarkCompletedButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { markJobCompleted() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !completionLoading
                ) {
                    if (completionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Mark Job Completed")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProviderChatPanel(
                requestId = requestId,
                userPhone = userPhone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(modifier = Modifier.height(10.dp))
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
        Column(modifier = Modifier.padding(14.dp)) {
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
        "paid",
        "active",
        "ongoing",
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