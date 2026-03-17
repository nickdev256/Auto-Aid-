package com.project.auto_aid.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavHostController) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "AutoAid Privacy Policy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = buildAnnotatedString {

                    append("AutoAid respects your privacy.\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("DATA WE COLLECT\n")
                    }
                    append("• Name, email, phone number\n")
                    append("• Location during service requests\n")
                    append("• Usage and diagnostic data\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("HOW WE USE DATA\n")
                    }
                    append("• To connect users with service providers\n")
                    append("• Improve service reliability\n")
                    append("• Ensure safety and fraud prevention\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("DATA SHARING\n")
                    }
                    append("AutoAid does not sell personal data.\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("SECURITY\n")
                    }
                    append("We apply reasonable technical and organizational measures to protect your data.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("CONSENT\n")
                    }
                    append("By using AutoAid, you consent to this policy.")
                },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate(Routes.LoginScreen.route) {
                        popUpTo(Routes.PrivacyPolicyScreen.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A9AD9),
                    contentColor = Color.White
                )
            ) {
                Text("CONTINUE")
            }
        }
    }
}