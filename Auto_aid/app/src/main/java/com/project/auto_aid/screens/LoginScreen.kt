package com.project.auto_aid.screens

import Components.HeroImageSlider
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.auto_aid.R
import com.project.auto_aid.authentcation.presentation.components.SocialMediaOptions
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.SocketManager
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController) {

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val tokenStore = if (isPreview) null else remember(context) { TokenStore(context) }

    val vm: AuthViewModel? = if (isPreview) null else viewModel()
    val authState = if (isPreview) null else vm!!.state.collectAsState().value

    var role by remember {
        mutableStateOf<String?>(if (isPreview) "User" else null)
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var navigated by remember { mutableStateOf(false) }

    val loading = authState?.loading ?: false
    val errorMsg = authState?.error ?: ""

    if (role == null) {
        RoleSelectionScreen { selectedRole ->
            role = selectedRole
        }
        return
    }

    LaunchedEffect(errorMsg) {
        if (isPreview) return@LaunchedEffect
        if (errorMsg.isBlank()) return@LaunchedEffect
        if (navigated) return@LaunchedEffect

        val lower = errorMsg.lowercase()
        val isMaintenance =
            "under maintenance" in lower ||
                    "maintenance mode" in lower ||
                    "currently under maintenance" in lower

        if (isMaintenance) {
            navigated = true

            navController.navigate(
                Routes.MaintenanceScreen.createRoute(
                    errorMsg.ifBlank {
                        "AutoAid is currently under maintenance. Please try again later."
                    }
                )
            ) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(authState?.data) {
        val data = authState?.data ?: return@LaunchedEffect
        if (navigated) return@LaunchedEffect

        val userRole = (data.user.role ?: "").lowercase()
        val selectedRole = (role ?: "").lowercase()

        if (userRole == selectedRole) {
            val token = tokenStore?.getToken()

            if (!token.isNullOrBlank()) {
                SocketManager.connect(
                    token = token,
                    onNotify = { payload ->
                        Log.d("GLOBAL_NOTIFY", "Received notify: $payload")
                    },
                    onConnected = {
                        Log.d("GLOBAL_NOTIFY", "Socket connected after login")
                    },
                    onError = { msg ->
                        Log.e("GLOBAL_NOTIFY", "Socket error: $msg")
                    }
                )
            }

            navigated = true

            val target = if (userRole == "provider") {
                Routes.ProviderDashboard.route
            } else {
                Routes.HomeScreen.route
            }

            navController.navigate(target) {
                popUpTo(Routes.LoginScreen.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            vm?.logout()
            SocketManager.disconnect()
            Toast.makeText(context, "Wrong role selected", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState()),
    ) {

        HeroImageSlider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
//
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Box(
                modifier = Modifier
                    .offset(y = (-57).dp)
                    .size(110.dp)
                    .border(
                        width = 7.dp,
                        color = Color(0xFF0A9AD9),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo01),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color.White)

                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .offset(y = (-36).dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    modifier = Modifier.padding(bottom = 5.dp),
                    text = "Welcome back!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text("Login to your account", color = Color.Gray)

                Spacer(modifier = Modifier.height(3.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(1.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .border(
                            width = 0.1.dp,
                            color = Color(0xFF0A9AD9),
                            shape = RoundedCornerShape(20.dp)
                        ),

                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = Color.Red)
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                focusedBorderColor = Color(0xFF0A9AD9),
                                cursorColor = Color(0xFF0A9AD9),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFF0A9AD9),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation =
                                if (showPassword) VisualTransformation.None
                                else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (showPassword) R.drawable.no_see else R.drawable.see
                                        ),
                                        contentDescription = if (showPassword) "Hide Password" else "Show Password",
                                        modifier = Modifier.size(25.dp),

                                        )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                focusedBorderColor = Color(0xFF0A9AD9),
                                cursorColor = Color(0xFF0A9AD9),
                                focusedLabelColor = Color(0xFF0A9AD9),
                                unfocusedBorderColor = Color.Gray,
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Forgot password?",
                            color = Color(0xFF0A9AD9),
                            fontSize = 15.sp,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable {
                                    if (!isPreview) {
                                        navController.navigate(Routes.ForgotPasswordScreen.route)
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (loading || isPreview) return@Button
                        navigated = false
                        SocketManager.disconnect()
                        vm?.login(email, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A9AD9),
                        contentColor = Color.White
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(25.dp)
                        )
                    } else {
                        Text("Login", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                Text("Or Sign Up with", color = Color.Gray)

                Spacer(modifier = Modifier.height(5.dp))

                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    Toast.makeText(
                        context,
                        "Google login will be connected to Node later",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                SocialMediaOptions(
                    onGoogleClick = {
                        Toast.makeText(context, "Google login: later", Toast.LENGTH_SHORT).show()
                    },
                    onFacebookClick = {
                        Toast.makeText(context, "Facebook login: later", Toast.LENGTH_SHORT).show()
                    },
                    onTikTokClick = {
                        Toast.makeText(context, "TikTok login: later", Toast.LENGTH_SHORT).show()
                    },
                    onInstagramClick = {
                        Toast.makeText(context, "Instagram login: later", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (!isPreview) {
                            navController.navigate(Routes.SignupScreen.route)
                        }
                    }
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.Gray)) {
                                append("Don’t have an account? ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF0A9AD9),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            ) {
                                append(" Sign Up")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(onSelect: (String) -> Unit) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.fuel),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Login As",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onSelect("User") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A9AD9),
                        contentColor = Color.White
                    )
                ) {
                    Text("User")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onSelect("provider") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A9AD9),
                        contentColor = Color.White
                    )
                ) {
                    Text("Service Provider")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(navController = rememberNavController())
}