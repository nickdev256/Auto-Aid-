package com.project.auto_aid.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.auto_aid.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    navController: NavController,
    fromSignup: Boolean = false
) {

    var accepted by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        (navController.context as? Activity)?.finish() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red)
                ) {
                    Text("Exit App")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF0A9AD9))
                    ) {
                    Text("Cancel")
                }
            },
            title = { Text("Terms Required") },
            text = {
                Text("You must accept the Terms & Conditions to use AutoAid.")
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Terms & Conditions",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
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
        ) {

            /* ================= TERMS CONTENT ================= */

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                Text(
                    text = "Welcome to AutoAid",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = """
                        By using AutoAid, you agree to the following terms and conditions.
                        1. AutoAid provides roadside assistance services including towing, fuel delivery, garage assistance, and emergency services.
                        
                        2. Users must provide accurate personal and vehicle information.
                        
                        3. Payments are processed securely and are non-refundable once a service is completed.
                        
                        4. Service providers are independent contractors; AutoAid is not liable for damages caused during service delivery.
                        
                        5. Misuse of the platform may result in account suspension.
                        
                        6. Your data is handled according to our Privacy Policy.
                        
                        Please read carefully before accepting.
                    """.trimIndent(),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = { accepted = it },
                                colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF0A9AD9),      // Box color when checked
                        checkmarkColor = Color.White,         // Tick color
                        uncheckedColor = Color.Black           // Border color when not checked
                    )
                    )



                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "I have read and agree to the Terms & Conditions",
                        fontSize = 14.sp
                    )
                }
            }

            /* ================= ACTION BUTTONS ================= */

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                val isClicked = false
                Button(
                    onClick = {
                        navController.navigate(Routes.PrivacyPolicyScreen.route) {
                            // so user can’t go back to onboard
                            popUpTo(Routes.TermsAndConditionsScreen.route) { inclusive = false }
                        }
                    },

            enabled = accepted,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isClicked)
                    Color(0xFF1565C0)     // Darker blue when clicked
                else
                    Color(0xFF0A9AD9),    // Normal blue

                contentColor = Color.White
            )
            ) {
            Text("ACCEPT")
        }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DECLINE")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TermsAndConditionsScreenPreview() {
    MaterialTheme {
        TermsAndConditionsScreen(
            navController = rememberNavController(),
            fromSignup = false
        )
    }
}