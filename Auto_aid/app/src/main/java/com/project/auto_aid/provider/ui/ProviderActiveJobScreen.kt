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
import androidx.compose.material3.AssistChip
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
    var pickupLabel by remember { mutableStateOf("") }

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
                pickupLabel = r.note?.takeIf { it.isNotBlank() } ?: "Location not labeled"

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
    val paymentStatusText = paymentStatusLabel(
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
                        Text(
                            text = "Pickup Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pickupLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Coords: $pickupLat, $pickupLng",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Navigate on Map")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentStatus == "assigned" || currentStatus == "pending") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (priceAlreadySet) "Price Sent" else "Set Your Service Price",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter the amount you will charge. We add a small system fee for the platform.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = providerAmountInput,
                                onValueChange = {
                                    if (!priceAlreadySet) {
                                        providerAmountInput = it
                                    }
                                },
                                label = { Text("Your Amount (UGX)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !priceAlreadySet && !quoteLoading,
                                prefix = { Text("UGX ") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("System Fee (10%):")
                                Text(formatUgx(systemFee))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "User Pays Total:",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatUgx(totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!priceAlreadySet) {
                                Button(
                                    onClick = { sendQuote() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !quoteLoading && providerAmountInput.isNotBlank()
                                ) {
                                    if (quoteLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Send Quote to User")
                                    }
                                }
                            } else {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Awaiting User Payment") }
                                )
                            }

                            quoteMessage?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            quoteError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (priceAlreadySet || totalAmount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Job Status & Payment",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LabelValue("Payment status", paymentStatusText)
                            LabelValue("Provider Completed", if (providerCompleted) "Yes" else "No")
                            LabelValue("User Confirmed", if (userCompleted) "Yes" else "No")

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
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Mark Job Finished")
                                    }
                                }

                                Text(
                                    text = "Click this after you have finished the work. Funds will be released when user confirms.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            completionMessage?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            completionError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Dashboard")
                }
            }
        }
    }
}

@Composable
fun LabelValue(
    label: String,
    value: String
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "UG"))
    return formatter.format(amount).replace("UGX", "Shs")
}

fun serviceDisplayName(s: String?): String {
    return when (s?.lowercase()?.trim()) {
        "garage" -> "Mechanical / Garage"
        "fuel" -> "Fuel Delivery"
        "towing" -> "Towing Service"
        "ambulance" -> "Ambulance / Emergency"
        else -> s ?: "-"
    }
}

fun formatStatus(s: String?): String {
    return s?.replace("_", " ")
        ?.replaceFirstChar { it.uppercase() }
        ?: "-"
}

fun paymentStatusLabel(
    paymentStatus: String,
    providerCompleted: Boolean,
    userCompleted: Boolean
): String {
    return when (paymentStatus) {
        "unpaid" -> "Waiting for User to pay"
        "held_in_escrow" -> {
            when {
                providerCompleted && userCompleted -> "Completed & Released"
                providerCompleted -> "Finished (Awaiting user confirmation)"
                else -> "Paid (Money in Escrow)"
            }
        }
        "released" -> "Paid & Completed"
        "refunded" -> "Refunded to User"
        else -> paymentStatus.replaceFirstChar { it.uppercase() }
    }
}

fun isRequestActive(status: String): Boolean {
    val s = status.lowercase().trim()
    return s != "completed" && s != "cancelled" && s != "declined"
}