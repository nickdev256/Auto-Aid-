package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.CreatePayoutRequestBody
import com.project.auto_aid.data.network.dto.PayoutRequestDto
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPayoutRequestsScreen(
    navController: NavHostController
) {
    val context = navController.context
    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var payoutRequests by remember { mutableStateOf<List<PayoutRequestDto>>(emptyList()) }

    suspend fun loadRequests(showRefreshLoader: Boolean = false) {
        if (showRefreshLoader) {
            refreshing = true
        } else {
            loading = true
        }

        errorMessage = null

        runCatching {
            val response = api.getProviderPayoutRequests()

            if (!response.isSuccessful) {
                val err = runCatching { response.errorBody()?.string() }.getOrNull()
                throw Exception(err ?: "Failed to load payout requests (${response.code()})")
            }

            payoutRequests = response.body().orEmpty()
        }.onFailure {
            errorMessage = it.message ?: "Failed to load payout requests"
        }

        loading = false
        refreshing = false
    }

    fun createRequest() {
        val cleanAmount = amount.trim().replace(",", "")
        val value = cleanAmount.toDoubleOrNull()

        if (value == null || value <= 0.0) {
            errorMessage = "Enter a valid payout amount"
            successMessage = null
            return
        }

        scope.launch {
            creating = true
            errorMessage = null
            successMessage = null

            runCatching {
                val response = api.createPayoutRequest(
                    CreatePayoutRequestBody(amount = value)
                )

                if (!response.isSuccessful) {
                    val err = runCatching { response.errorBody()?.string() }.getOrNull()
                    throw Exception(err ?: "Failed to create payout request (${response.code()})")
                }

                successMessage = "Payout request created successfully"
                amount = ""
                loadRequests(showRefreshLoader = true)
            }.onFailure {
                errorMessage = it.message ?: "Failed to create payout request"
            }

            creating = false
        }
    }

    LaunchedEffect(Unit) {
        loadRequests()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Payout Requests",
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
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!loading && !refreshing && !creating) {
                                scope.launch { loadRequests(showRefreshLoader = true) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create New Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { input ->
                            amount = input.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        label = { Text("Amount (UGX)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = { createRequest() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !creating
                    ) {
                        if (creating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(vertical = 2.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Request Payout")
                        }
                    }
                }
            }

            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            successMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (refreshing) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Text(
                    text = "Request History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (payoutRequests.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No payout requests yet",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your submitted payout requests will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = payoutRequests,
                            key = { item ->
                                item._id
                                    ?: item.id
                                    ?: item.createdAt
                                    ?: "${item.amount}_${item.status}"
                            }
                        ) { item ->
                            PayoutRequestCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayoutRequestCard(
    item: PayoutRequestDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatUgx(item.amount ?: 0.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Status: ${item.status?.uppercase() ?: "PENDING"}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (item.status?.lowercase()) {
                    "paid" -> MaterialTheme.colorScheme.primary
                    "rejected" -> MaterialTheme.colorScheme.error
                    "approved" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }
            )

            item.method?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Method: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.accountName?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Account Name: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Phone: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.bankName?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Bank: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.accountNumber?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Account Number: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.adminNote?.takeIf { it.isNotBlank() }?.let {
                HorizontalDivider()
                Text(
                    text = "Admin Note: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.paidAt?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Paid At: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item.createdAt?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Requested on: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatUgx(amount: Double): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        "UGX ${formatter.format(amount.roundToInt())}"
    } catch (_: Exception) {
        "UGX ${amount.roundToInt()}"
    }
}