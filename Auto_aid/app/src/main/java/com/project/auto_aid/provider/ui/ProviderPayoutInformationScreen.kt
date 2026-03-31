package com.project.auto_aid.provider.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.navigation.Routes

private val AutoAidBlue = Color(0xFF1DA1F2)
private val AutoAidDark = Color(0xFF114B5F)
private val ScreenBg = Color(0xFFF5F9FC)
private val CardBg = Color.White
private val SoftBlue = Color(0xFFEAF6FF)
private val SuccessGreen = Color(0xFF20B26B)
private val SuccessGreenBg = Color(0xFFEAF8F1)
private val MutedText = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPayoutInformationScreen(
    navController: NavHostController
) {
    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Provider Information",
                        color = AutoAidDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AutoAidBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CardBg
                )
            )
        },
        bottomBar = {
            Surface(
                color = CardBg,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            navController.navigate(Routes.ProviderMapHome.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AutoAidBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(
                            text = "Open Map",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text(
                            text = "Back to Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusBanner(
                title = "Simplified Flow Active",
                body = "Payout information is not used in your new Auto Aid flow. This screen is now kept only as a clean information page so the app compiles without the old payout API.",
                background = SuccessGreenBg,
                foreground = SuccessGreen
            )

            InfoCard(
                title = "Current Provider Flow",
                iconTint = AutoAidBlue
            ) {
                StepText("1. Provider sees request")
                StepText("2. Provider accepts request")
                StepText("3. Provider clicks Start Job")
                StepText("4. Provider clicks Arrived")
                StepText("5. Provider sends quotation")
                StepText("6. User views quotation")
                StepText("7. User accepts quotation")
                StepText("8. User enters phone number")
                StepText("9. User pays quoted amount")
                StepText("10. Provider clicks Job Done")
                StepText("11. User clicks Job Done")
            }

            InfoCard(
                title = "What Was Removed",
                iconTint = AutoAidBlue
            ) {
                BulletText("Payout API")
                BulletText("Wallet balance flow")
                BulletText("Escrow release flow")
                BulletText("Extra charge flow")
                BulletText("Payout requests")
            }

            InfoCard(
                title = "Recommended Quick Actions",
                iconTint = AutoAidBlue
            ) {
                ActionMiniCard(
                    title = "Map",
                    subtitle = "Use the provider map to navigate to the customer."
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionMiniCard(
                    title = "Active Jobs",
                    subtitle = "Continue jobs from the dashboard and provider active job screen."
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionMiniCard(
                    title = "Notifications",
                    subtitle = "Monitor request updates and payment progress."
                )
            }

            InfoCard(
                title = "Important Note",
                iconTint = AutoAidBlue
            ) {
                Text(
                    text = "This screen no longer loads payout details from the backend because your simplified system does not use payout endpoints anymore.",
                    color = AutoAidDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    title: String,
    body: String,
    background: Color,
    foreground: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = foreground
                )
            }

            Text(
                text = title,
                color = AutoAidDark,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = body,
                color = AutoAidDark,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SoftBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = iconTint
                )
            }

            Text(
                text = title,
                color = AutoAidDark,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            content()
        }
    }
}

@Composable
private fun StepText(text: String) {
    Text(
        text = text,
        color = AutoAidDark,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun BulletText(text: String) {
    Text(
        text = "• $text",
        color = MutedText,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun ActionMiniCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = AutoAidDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MutedText
            )
        }
    }
}