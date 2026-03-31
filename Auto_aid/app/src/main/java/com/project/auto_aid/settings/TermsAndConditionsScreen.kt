package com.project.auto_aid.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
WELCOME TO AUTO AID

Effective Date: [Insert Date]

By using Auto Aid, you agree to the following Terms and Conditions. Please read carefully.

--------------------------------------------------

1. PLATFORM ROLE
Auto Aid is a digital platform that connects users with independent service providers for:
- Towing
- Fuel delivery
- Mechanical assistance
- Emergency services

Auto Aid is NOT a direct service provider and is not responsible for how services are executed.

--------------------------------------------------

2. ELIGIBILITY
You must:
- Be at least 18 years old
- Provide accurate information
- Use the platform lawfully

--------------------------------------------------

3. SERVICE FLOW AGREEMENT (STRICT)
All users and providers MUST follow this system:

1. Request created
2. Provider accepts
3. Provider clicks Start Job
4. Provider clicks Arrived
5. Provider analyzes issue
6. Provider sends quotation
7. User accepts & pays
8. Provider completes job
9. Both confirm Job Done

Any attempt to bypass this flow is a violation.

--------------------------------------------------

4. USER RESPONSIBILITIES
Users must:
- Provide correct location and problem details
- Pay agreed amount through the platform
- Treat providers respectfully
- Avoid false or misleading requests

--------------------------------------------------

5. PROVIDER RESPONSIBILITIES
Providers must:
- Offer professional and honest services
- Send fair quotations
- Avoid off-platform payments
- Follow the Auto Aid workflow strictly

--------------------------------------------------

6. PAYMENTS
- Payments may be simulated or real depending on system stage
- Service proceeds after payment confirmation
- Auto Aid may apply commission fees

--------------------------------------------------

7. PROHIBITED ACTIVITIES
The following are strictly forbidden:
- Fraud or scams
- Fake requests or fake services
- Harassment or abuse
- Bypassing platform payments
- Impersonation

--------------------------------------------------

8. CRIMINAL ACTIVITIES & LEGAL LIABILITY
If any user or provider commits a crime including:
- Theft
- Assault
- Fraud
- Damage to property
- Any illegal act under Ugandan law

THEN:

- The individual is fully responsible under the laws of Uganda
- Auto Aid will:
    • Suspend or terminate the account
    • Report to law enforcement authorities
    • Provide evidence including:
        - Location data
        - Chat records
        - Payment records

Auto Aid is NOT liable for criminal actions.

--------------------------------------------------

9. CONSEQUENCES OF VIOLATIONS
Auto Aid may:
- Suspend accounts
- Permanently ban users/providers
- Withhold payments
- Report to authorities

Repeated violations lead to permanent removal.

--------------------------------------------------

10. DATA & PRIVACY
Auto Aid may collect and use:
- Location data
- Communication logs
- Payment records

This data may be shared with authorities if required by law.

--------------------------------------------------

11. LIMITATION OF LIABILITY
Auto Aid is not responsible for:
- Service quality
- Delays or cancellations
- Damages or injuries

All responsibility lies with the service provider or user.

--------------------------------------------------

12. SAFETY NOTICE
Users and providers use the platform at their own risk.

Always:
- Verify details
- Meet in safe environments
- Report suspicious activity

--------------------------------------------------

13. TERMINATION
Auto Aid may suspend or terminate any account that violates these terms.

--------------------------------------------------

14. GOVERNING LAW
These terms are governed by the laws of the Republic of Uganda.

--------------------------------------------------

15. ACCEPTANCE
By continuing, you confirm that:
- You have read and understood these terms
- You agree to comply with all rules
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {
                        navController.navigate(Routes.PrivacyPolicyScreen.route) {
                            popUpTo(Routes.TermsAndConditionsScreen.route) { inclusive = false }
                        }
                    },
                    enabled = accepted,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A9AD9),
                        contentColor = Color.White
                    )
                ) {
                    Text("ACCEPT")
                }

                OutlinedButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = BorderStroke(1.dp, Color.Red)
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