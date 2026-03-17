package com.project.auto_aid.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.project.auto_aid.R
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.MaintenanceException
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.SignupRequest
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }

    var role by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    var serviceTypeUi by remember { mutableStateOf("") }
    var subscriptionUi by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }

    if (role == null) {
        RoleSelection(onSelect = { selected -> role = selected })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {

        HeroImageSlider()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Create Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            "Fast help at your location",
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                InputField("Full Name", name) { name = it }
                InputField("Email Address", email) { email = it }

                UgandaPhoneInput(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    isError = phone.isNotEmpty() && !isValidUgandaPhone(phone)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PasswordInput(
                            label = "Password",
                            value = password,
                            show = showPassword,
                            toggle = { showPassword = !showPassword }
                        ) { password = it }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        PasswordInput(
                            label = "Confirm",
                            value = confirmPassword,
                            show = showConfirm,
                            toggle = { showConfirm = !showConfirm }
                        ) { confirmPassword = it }
                    }
                }

                if (role == "provider") {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Dropdown(
                                label = "Service Type",
                                options = listOf("Towing", "Garage", "Fuel", "Ambulance"),
                                selected = serviceTypeUi,
                                onSelect = { serviceTypeUi = it }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Dropdown(
                                label = "Subscription",
                                options = listOf("Monthly", "Quarterly", "Yearly"),
                                selected = subscriptionUi,
                                onSelect = { subscriptionUi = it }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (loading) return@Button

                if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                    toast(context, "Fill in all fields")
                    return@Button
                }

                if (!isValidUgandaPhone(phone)) {
                    toast(context, "Invalid Uganda phone number")
                    return@Button
                }

                if (password != confirmPassword) {
                    toast(context, "Passwords do not match")
                    return@Button
                }

                if (role == "provider" && serviceTypeUi.isBlank()) {
                    toast(context, "Select Service Type")
                    return@Button
                }

                if (role == "provider" && subscriptionUi.isBlank()) {
                    toast(context, "Select Subscription")
                    return@Button
                }

                val businessType = if (role == "provider") serviceTypeUi.trim().lowercase() else ""
                val subscriptionPlan = if (role == "provider") subscriptionUi.trim().lowercase() else null

                loading = true

                scope.launch {
                    val body = SignupRequest(
                        name = name.trim(),
                        email = email.trim().lowercase(),
                        phone = phone.trim(),
                        password = password.trim(),
                        role = role!!.trim().lowercase(),
                        businessName = "",
                        businessType = businessType,
                        servicesOffered = if (businessType.isNotBlank()) listOf(businessType) else emptyList(),
                        subscriptionPlan = subscriptionPlan
                    )

                    val res = try {
                        api.signup(body)
                    } catch (e: MaintenanceException) {
                        loading = false
                        navController.navigate(
                            Routes.MaintenanceScreen.createRoute(
                                e.message ?: "AutoAid is currently under maintenance. Please try again later."
                            )
                        ) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                        return@launch
                    } catch (e: Exception) {
                        loading = false
                        toast(context, e.message ?: "Network error")
                        return@launch
                    }

                    loading = false

                    if (res.isSuccessful) {
                        val data = res.body()
                        val pendingEmail = (data?.email ?: body.email).trim().lowercase()

                        toast(context, "OTP sent to $pendingEmail ✅")
                        navController.navigate(Routes.VerifyCodeScreen.createRoute(pendingEmail))
                    } else {
                        val err = res.errorBody()?.string()
                        Log.e("AUTH", "❌ signup ${res.code()} err=$err body=$body")
                        toast(context, err ?: "Signup failed (${res.code()})")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .height(52.dp),
            shape = RoundedCornerShape(25.dp),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A9AD9),
                contentColor = Color.White
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(text = "Continue", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { navController.navigate(Routes.LoginScreen.route) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Gray)) { append("Already have an account? ") }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF0A9AD9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    ) { append("Login") }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/* ================= HERO SLIDER ================= */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroImageSlider() {
    val images = listOf(R.drawable.total_1, R.drawable.shell_2, R.drawable.logo14)
    val pagerState = rememberPagerState { images.size }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % images.size)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) { page ->
        AsyncImage(
            model = images[page],
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/* ================= INPUTS ================= */

@Composable
fun InputField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun PasswordInput(
    label: String,
    value: String,
    show: Boolean,
    toggle: () -> Unit,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = toggle) {
                Icon(
                    painter = painterResource(id = if (show) R.drawable.no_see else R.drawable.see),
                    contentDescription = "toggle",
                    modifier = Modifier.size(25.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun UgandaPhoneInput(phone: String, onPhoneChange: (String) -> Unit, isError: Boolean) {
    OutlinedTextField(
        value = phone,
        onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() }.take(9)) },
        label = { Text("Phone Number") },
        leadingIcon = { Text("+256 ") },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/* ================= DROPDOWN ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.zIndex(1f)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ================= ROLE SELECTION ================= */

@Composable
fun RoleSelection(onSelect: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

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
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sign Up As", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSelect("user") },
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

                Spacer(modifier = Modifier.height(8.dp))

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

/* ================= UTILS ================= */

fun isValidUgandaPhone(phone: String): Boolean {
    val prefixes = listOf("70", "74", "75", "76", "77", "78")
    return phone.length == 9 && prefixes.any { phone.startsWith(it) }
}

fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}