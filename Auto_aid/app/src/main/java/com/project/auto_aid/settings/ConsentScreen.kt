package com.project.auto_aid.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.auto_aid.navigation.Routes

@Composable
fun ConsentScreen(navController: NavController) {

    var acceptTerms by remember { mutableStateOf(false) }
    var acceptPrivacy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Before you continue",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = acceptTerms,
                onCheckedChange = { acceptTerms = it }
            )
            Text("I agree to the Terms & Conditions")
        }

        TextButton(
            onClick = {
                navController.navigate(Routes.TermsAndConditionsScreen.route)
            }
        ) {
            Text("View Terms")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = acceptPrivacy,
                onCheckedChange = { acceptPrivacy = it }
            )
            Text("I agree to the Privacy Policy")
        }

        TextButton(
            onClick = {
                navController.navigate(Routes.PrivacyPolicyScreen.route)
            }
        ) {
            Text("View Privacy Policy")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                navController.navigate(Routes.LoginScreen.route)
            },
            enabled = acceptTerms && acceptPrivacy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}