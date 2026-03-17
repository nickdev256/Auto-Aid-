package com.project.auto_aid.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.screens.AppBottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                notificationCount = 0
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {

            /* ================= MY ACCOUNT ================= */

            SectionTitle("My Account")

            SectionCard {

                SettingsRow(
                    title = "Profile",
                    icon = Icons.Default.Person
                ) {
                    navController.navigate(Routes.UserInfoScreen.route)
                }

                SettingsRow(
                    title = "Promotions",
                    icon = Icons.Default.LocalOffer
                ) {
                    navController.navigate(Routes.PromotionScreen.route)
                }

                SettingsRow(
                    title = "Payout Information",
                    icon = Icons.Default.AccountBalanceWallet
                ) {
                    navController.navigate(Routes.PayoutInformationScreen.route)
                }

                SettingsRow(
                    title = "About Us",
                    icon = Icons.Default.Info
                ) {
                    navController.navigate(Routes.AboutUsScreen.route)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            /* ================= INFORMATION ================= */

            SectionTitle("Information")

            SectionCard {

                SettingsRow(
                    title = "Version",
                    icon = Icons.Default.PhoneAndroid,
                    trailingText = "1.0.0"
                )

                SettingsRow(
                    title = "Terms & Conditions",
                    icon = Icons.Default.Description
                ) {
                    navController.navigate(Routes.TermsAndConditionsScreen.route)
                }

                SettingsRow(
                    title = "Privacy Policy",
                    icon = Icons.Default.Security
                ) {
                    navController.navigate(Routes.PrivacyPolicyScreen.route)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            /* ================= SIGN OUT ================= */

            Text(
                text = "Sign Out",
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(Routes.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    .padding(vertical = 18.dp)
            )
        }
    }
}

/* ===================================================
   REUSABLE COMPONENTS
=================================================== */

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    trailingText: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0A9AD8),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        trailingText?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }

    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}