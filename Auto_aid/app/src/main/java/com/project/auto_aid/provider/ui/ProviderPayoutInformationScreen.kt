package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.UpdatePayoutInfoBody
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPayoutInformationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(tokenStore) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var paymentMethod by remember { mutableStateOf("mobile_money") }
    var accountName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        successMessage = null

        runCatching {
            val response = api.getPayoutInfo()
            if (!response.isSuccessful) {
                throw Exception("Failed to load payout info (HTTP ${response.code()})")
            }

            val body = response.body()
            paymentMethod = body?.method?.ifBlank { "mobile_money" } ?: "mobile_money"
            accountName = body?.accountName.orEmpty()
            phoneNumber = body?.phoneNumber.orEmpty()
            bankName = body?.bankName.orEmpty()
            accountNumber = body?.accountNumber.orEmpty()
            isVerified = body?.isVerified == true
        }.onFailure {
            errorMessage = it.message ?: "Failed to load payout info"
        }

        loading = false
    }

    fun validateInputs(): String? {
        if (accountName.trim().isEmpty()) return "Account name is required."

        return if (paymentMethod == "mobile_money") {
            if (phoneNumber.trim().isEmpty()) "Mobile money number is required." else null
        } else {
            when {
                bankName.trim().isEmpty() -> "Bank name is required."
                accountNumber.trim().isEmpty() -> "Account number is required."
                else -> null
            }
        }
    }

    fun savePayoutInfo() {
        val validationError = validateInputs()
        if (validationError != null) {
            errorMessage = validationError
            successMessage = null
            return
        }

        scope.launch {
            saving = true
            errorMessage = null
            successMessage = null

            runCatching {
                val response = api.updatePayoutInfo(
                    UpdatePayoutInfoBody(
                        method = paymentMethod,
                        accountName = accountName.trim(),
                        phoneNumber = if (paymentMethod == "mobile_money") phoneNumber.trim() else "",
                        bankName = if (paymentMethod == "bank") bankName.trim() else "",
                        accountNumber = if (paymentMethod == "bank") accountNumber.trim() else ""
                    )
                )

                if (!response.isSuccessful) {
                    throw Exception("Failed to save payout info (HTTP ${response.code()})")
                }

                val body = response.body()
                val payoutInfo = body?.payoutInfo

                paymentMethod = payoutInfo?.method?.ifBlank { "mobile_money" } ?: "mobile_money"
                accountName = payoutInfo?.accountName.orEmpty()
                phoneNumber = payoutInfo?.phoneNumber.orEmpty()
                bankName = payoutInfo?.bankName.orEmpty()
                accountNumber = payoutInfo?.accountNumber.orEmpty()
                isVerified = payoutInfo?.isVerified == true

                successMessage = body?.message ?: "Payout information saved successfully."
            }.onFailure {
                errorMessage = it.message ?: "Failed to save payout information"
            }

            saving = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Provider Payout Information",
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
        if (loading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add your payout details to receive payments from completed jobs.",
                    style = MaterialTheme.typography.bodyMedium
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (isVerified) "Payout Info Verified" else "Payout Info Not Verified"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Payout Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = paymentMethod == "mobile_money",
                        onClick = {
                            paymentMethod = "mobile_money"
                            successMessage = null
                            errorMessage = null
                        },
                        label = { Text("Mobile Money") }
                    )

                    FilterChip(
                        selected = paymentMethod == "bank",
                        onClick = {
                            paymentMethod = "bank"
                            successMessage = null
                            errorMessage = null
                        },
                        label = { Text("Bank") }
                    )
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = {
                        accountName = it
                        successMessage = null
                    },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (paymentMethod == "mobile_money") {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            successMessage = null
                        },
                        label = { Text("Mobile Money Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                } else {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = {
                            bankName = it
                            successMessage = null
                        },
                        label = { Text("Bank Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = {
                            accountNumber = it
                            successMessage = null
                        },
                        label = { Text("Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

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
                    onClick = { savePayoutInfo() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !saving
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Payout Details")
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
}