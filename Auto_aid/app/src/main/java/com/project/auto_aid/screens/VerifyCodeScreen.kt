package com.project.auto_aid.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.ResendOtpRequest
import com.project.auto_aid.data.network.dto.VerifyOtpRequest
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun VerifyCodeScreen(
    navController: NavController,
    email: String
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    // ✅ Decode email from route
    val cleanEmail = remember(email) { Uri.decode(email).trim().lowercase() }

    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { navController.navigateUp() }
        ) {
            Text("Back", color = Color(0xFF0A9AD9), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text("Enter Code", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("6-digit code sent to: $cleanEmail", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) code = it
            },
            label = { Text("Verification Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (code.length != 6) {
                    Toast.makeText(context, "Enter 6 digits", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                loading = true
                scope.launch {
                    val res = runCatching {
                        api.verifyOtp(VerifyOtpRequest(email = cleanEmail, otp = code))
                    }.getOrElse { e ->
                        loading = false
                        Toast.makeText(context, e.message ?: "Network error", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    loading = false

                    if (res.isSuccessful) {
                        val data = res.body()
                        val token = data?.token.orEmpty()
                        val role = data?.user?.role?.trim()?.lowercase().orEmpty()

                        Log.d("OTP", "✅ verify success role=$role user=${data?.user}")

                        if (token.isNotBlank()) tokenStore.saveToken(token)

                        val targetRoute = if (role == "provider") {
                            Routes.ProviderDashboard.route
                        } else {
                            Routes.HomeScreen.route
                        }

                        navController.navigate(targetRoute) {
                            // ✅ remove signup + verify from backstack so back can't return to them
                            popUpTo(Routes.SignupScreen.route) { inclusive = true }
                            popUpTo(Routes.VerifyCodeScreen.route.substringBefore("/{")) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        val err = res.errorBody()?.string()
                        Log.e("OTP", "❌ verify failed ${res.code()} err=$err email=$cleanEmail otp=$code")
                        Toast.makeText(context, err ?: "OTP failed (${res.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A9AD9),
                contentColor = Color.White
            )
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Verify")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Resend should ONLY resend and show toast
        Text(
            text = "Resend Code",
            color = Color(0xFF0A9AD9),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    scope.launch {
                        val res = runCatching {
                            api.resendOtp(ResendOtpRequest(email = cleanEmail))
                        }.getOrElse {
                            Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (res.isSuccessful) {
                            val msg = res.body()?.message ?: "New OTP sent ✅"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val err = res.errorBody()?.string()
                            Toast.makeText(context, err ?: "Failed to resend OTP", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        )
    }
}