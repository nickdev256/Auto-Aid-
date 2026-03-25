package com.project.auto_aid.provider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderWalletScreen(
    navController: NavHostController
) {
    val context = navController.context
    val lifecycleOwner = LocalLifecycleOwner.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var totalEarned by remember { mutableDoubleStateOf(0.0) }
    var pendingBalance by remember { mutableDoubleStateOf(0.0) }
    var totalPaidOut by remember { mutableDoubleStateOf(0.0) }
    var availableBalance by remember { mutableDoubleStateOf(0.0) }

    suspend fun loadWallet(showRefreshLoader: Boolean = false) {
        if (loading || refreshing) return

        if (showRefreshLoader) {
            refreshing = true
        } else {
            loading = true
        }

        errorMessage = null

        runCatching {
            val response = api.getProviderWallet()

            if (!response.isSuccessful) {
                val message = when (response.code()) {
                    401 -> "Your session expired. Please log in again."
                    403 -> "You are not allowed to access this wallet."
                    404 -> "Wallet endpoint not found on server."
                    500 -> "Server error while loading wallet."
                    else -> "Failed to load wallet (${response.code()})"
                }
                throw Exception(message)
            }

            val body = response.body()
                ?: throw Exception("Wallet response is empty")

            totalEarned = body.totalEarned ?: 0.0
            pendingBalance = body.pendingBalance ?: 0.0
            totalPaidOut = body.totalPaidOut ?: 0.0
            availableBalance = body.availableBalance ?: 0.0
        }.onFailure {
            errorMessage = it.message ?: "Failed to load wallet"
        }

        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) {
        loading = false
        loadWallet(showRefreshLoader = false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    loadWallet(showRefreshLoader = true)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Wallet",
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
                            if (!loading && !refreshing) {
                                scope.launch {
                                    loadWallet(showRefreshLoader = true)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh wallet"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (refreshing) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    WalletCard(
                        title = "Available Balance",
                        value = formatUgx(availableBalance)
                    )

                    WalletCard(
                        title = "Total Earned",
                        value = formatUgx(totalEarned)
                    )

                    WalletCard(
                        title = "Pending Payouts",
                        value = formatUgx(pendingBalance)
                    )

                    WalletCard(
                        title = "Total Paid Out",
                        value = formatUgx(totalPaidOut)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            navController.navigate(Routes.ProviderPayoutRequests.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Request Payout")
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatUgx(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    formatter.minimumFractionDigits = 0
    return "UGX ${formatter.format(amount)}"
}