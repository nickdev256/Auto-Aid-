package com.project.auto_aid.settings

import android.util.Log
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutInformationScreen(
    navController: NavHostController,
    requestId: String = "",
    providerName: String = "Service Provider",
    serviceName: String = "Completed Job",
    amount: Double = 0.0
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var paymentMethod by remember { mutableStateOf("mobile_money") }
    var phoneNumber by remember { mutableStateOf("") }
    var transactionReference by remember { mutableStateOf("") }

    var quoteLoading by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }

    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var providerAmount by remember { mutableDoubleStateOf(0.0) }
    var systemFee by remember { mutableDoubleStateOf(0.0) }
    var totalAmount by remember { mutableDoubleStateOf(amount) }
    var pricingStatus by remember { mutableStateOf("not_set") }
    var paymentStatus by remember { mutableStateOf("unpaid") }

    fun validateInputs(): String? {
        return when {
            totalAmount <= 0.0 -> "Payment amount is invalid."
            paymentMethod == "mobile_money" && phoneNumber.trim().isEmpty() ->
                "Phone number is required."
            else -> null
        }
    }

    fun loadQuote() {
        if (requestId.isBlank()) {
            providerAmount = amount
            systemFee = 0.0
            totalAmount = amount
            return
        }

        scope.launch {
            quoteLoading = true
            errorMessage = null

            runCatching {
                val response = api.getRequestQuote(requestId)
                if (!response.isSuccessful) {
                    throw Exception("Failed to load quote (${response.code()})")
                }

                val quote = response.body()
                providerAmount = quote?.providerAmount ?: amount
                systemFee = quote?.systemFee ?: 0.0
                totalAmount = quote?.totalAmount ?: amount
                pricingStatus = quote?.pricingStatus ?: "not_set"
                paymentStatus = quote?.paymentStatus ?: "unpaid"
            }.onFailure {
                Log.e("CLIENT_PAYMENT", "Quote load error", it)
                errorMessage = it.message ?: "Failed to load quote"
                providerAmount = amount
                systemFee = 0.0
                totalAmount = amount
            }

            quoteLoading = false
        }
    }

    fun makePayment() {
        val validationError = validateInputs()
        if (validationError != null) {
            errorMessage = validationError
            successMessage = null
            return
        }

        scope.launch {
            paying = true
            errorMessage = null
            successMessage = null

            runCatching {
                // Replace this with your real backend payment call when ready.
                // Example:
                // val response = api.makePayment(
                //     CreatePaymentBody(
                //         requestId = requestId,
                //         amount = totalAmount,
                //         method = if (paymentMethod == "mobile_money") "momo" else paymentMethod,
                //         phoneNumber = phoneNumber.trim(),
                //         transactionReference = transactionReference.trim()
                //     )
                // )
                //
                // if (!response.isSuccessful) {
                //     throw Exception("Payment failed (${response.code()})")
                // }

                Log.d("CLIENT_PAYMENT", "requestId=$requestId")
                Log.d("CLIENT_PAYMENT", "providerAmount=$providerAmount")
                Log.d("CLIENT_PAYMENT", "systemFee=$systemFee")
                Log.d("CLIENT_PAYMENT", "totalAmount=$totalAmount")
                Log.d("CLIENT_PAYMENT", "method=$paymentMethod")
                Log.d("CLIENT_PAYMENT", "phoneNumber=$phoneNumber")
                Log.d("CLIENT_PAYMENT", "transactionReference=$transactionReference")

                successMessage = "Payment submitted successfully."
                paymentStatus = "pending"
            }.onFailure {
                Log.e("CLIENT_PAYMENT", "Payment error", it)
                errorMessage = it.message ?: "Failed to process payment"
            }

            paying = false
        }
    }

    LaunchedEffect(requestId) {
        loadQuote()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Make Payment",
                        fontWeight = FontWeight.SemiBold
                    )
                },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Complete payment for the finished AutoAid service.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (quoteLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Provider: $providerName")
                    Text(text = "Request ID: ${if (requestId.isBlank()) "N/A" else requestId}")
                    Text(text = "Pricing Status: $pricingStatus")
                    Text(text = "Payment Status: $paymentStatus")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Payment Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Provider Charge: UGX ${providerAmount.roundToInt()}")
                    Text(text = "System Fee: UGX ${systemFee.roundToInt()}")
                    Text(
                        text = "Total Amount: UGX ${totalAmount.roundToInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = paymentMethod == "mobile_money",
                    onClick = {
                        paymentMethod = "mobile_money"
                        errorMessage = null
                        successMessage = null
                    },
                    label = { Text("Mobile Money") }
                )

                FilterChip(
                    selected = paymentMethod == "cash",
                    onClick = {
                        paymentMethod = "cash"
                        errorMessage = null
                        successMessage = null
                    },
                    label = { Text("Cash") }
                )
            }

            if (paymentMethod == "mobile_money") {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        successMessage = null
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 0772123456") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            OutlinedTextField(
                value = transactionReference,
                onValueChange = {
                    transactionReference = it
                    successMessage = null
                },
                label = { Text("Reference / Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { makePayment() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !paying && !quoteLoading && totalAmount > 0.0
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

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}