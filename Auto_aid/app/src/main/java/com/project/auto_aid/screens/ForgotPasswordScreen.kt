package com.project.auto_aid.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ForgotPasswordRequest
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(navController: NavController) {

    val context = LocalContext.current
    val api = remember(context) { RetrofitClient.create(TokenStore(context)) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(20.dp)
    ) {

        Text(
            text = "Forgot Password",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Enter your email and we will send instructions to reset your password.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        error?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }

        success?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(text = msg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                if (email.isBlank()) {
                    showToast(context, "Enter your email")
                    return@Button
                }

                loading = true
                error = null
                success = null

                scope.launch {
                    val res = runCatching {
                        api.forgotPassword(ForgotPasswordRequest(email.trim().lowercase()))
                    }.getOrElse { e ->
                        loading = false
                        error = e.message ?: "Network error"
                        return@launch
                    }

                    loading = false

                    if (res.isSuccessful) {
                        val msg = res.body()?.message ?: "Request sent ✅"
                        success = msg
                        showToast(context, msg)
                    } else {
                        val err = res.errorBody()?.string()
                        error = err ?: "Failed (${res.code()})"
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send Reset Link", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { navController.navigate(Routes.LoginScreen.route) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back to Login") }
    }
}

/* ✅ renamed helper to avoid overload ambiguity */
fun showToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}