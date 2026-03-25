package com.project.auto_aid.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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
            shape = RoundedCornerShape(16.dp), // Only one shape
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                focusedBorderColor = Color(0xFF0A9AD9),
                cursorColor = Color(0xFF0A9AD9),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF0A9AD9),
                unfocusedLabelColor = Color.Gray
            )
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

        // Row for buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Send Reset Link button on the left
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
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A9AD9), // Background color
                    contentColor = Color.White           // Text color
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp)) // Space between buttons



            OutlinedButton(
                onClick = { navController.navigate(Routes.LoginScreen.route) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0A9AD9) // Text color
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = Color(0xFF0A9AD9) // Border color
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Login")
            }
        }
    }
}

/* ✅ Helper function outside the Composable */
fun showToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    val navController = rememberNavController()
    ForgotPasswordScreen(navController = navController)
}