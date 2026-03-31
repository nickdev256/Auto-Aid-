package com.project.auto_aid.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.auto_aid.navigation.Routes
import com.project.auto_aid.screens.AppBottomNavigationBar

private val AutoAidBlue = Color(0xFF1DA1F2)
private val AutoAidDark = Color(0xFF114B5F)
private val ScreenBg = Color(0xFFF5F9FC)
private val CardBg = Color.White
private val SoftBlue = Color(0xFFEAF6FF)
private val SoftBlue2 = Color(0xFFF3FAFF)
private val SoftBorder = Color(0xFFDCEAF5)
private val MutedText = Color(0xFF6B7280)
private val DangerRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = AutoAidDark
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CardBg
                )
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            SettingsHeroCard()

            Spacer(modifier = Modifier.height(18.dp))

            SectionTitle("My Account")

            SectionCard {
                SettingsRow(
                    title = "Profile",
                    subtitle = "Manage your account details",
                    icon = Icons.Default.Person
                ) {
                    navController.navigate(Routes.UserInfoScreen.route)
                }

                SettingsRow(
                    title = "Promotions",
                    subtitle = "View available offers and updates",
                    icon = Icons.Default.LocalOffer
                ) {
                    navController.navigate(Routes.PromotionScreen.route)
                }

                SettingsRow(
                    title = "Payments & Wallet",
                    subtitle = "Manage current payment options",
                    icon = Icons.Default.AccountBalanceWallet
                ) {
                    navController.navigate(Routes.PayoutInformationScreen.route)
                }

                SettingsRow(
                    title = "Payment History",
                    subtitle = "View your previous transactions",
                    icon = Icons.Default.ReceiptLong
                ) {
                    navController.navigate(Routes.PaymentHistoryScreen.route)
                }

                SettingsRow(
                    title = "About Us",
                    subtitle = "Learn more about Auto Aid",
                    icon = Icons.Default.Info,
                    showDivider = false
                ) {
                    navController.navigate(Routes.AboutUsScreen.route)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("Information")

            SectionCard {
                SettingsRow(
                    title = "Version",
                    subtitle = "Current app version",
                    icon = Icons.Default.PhoneAndroid,
                    trailingText = "1.0.0"
                )

                SettingsRow(
                    title = "Terms & Conditions",
                    subtitle = "Read the service terms",
                    icon = Icons.Default.Description
                ) {
                    navController.navigate(Routes.TermsAndConditionsScreen.route)
                }

                SettingsRow(
                    title = "Privacy Policy",
                    subtitle = "How your data is handled",
                    icon = Icons.Default.Security,
                    showDivider = false
                ) {
                    navController.navigate(Routes.PrivacyPolicyScreen.route)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            SignOutCard {
                navController.navigate(Routes.LoginScreen.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(SoftBlue, SoftBlue2)
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(AutoAidBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AutoAidBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Account & Payments",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AutoAidDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage profile, payments, wallet, and history",
                            color = MutedText,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroMiniChip(label = "Profile")
                    HeroMiniChip(label = "Wallet")
                    HeroMiniChip(label = "History")
                }
            }
        }
    }
}

@Composable
private fun HeroMiniChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Text(
            text = label,
            color = AutoAidBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = MutedText,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    trailingText: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoftBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AutoAidBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AutoAidDark
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MutedText
                    )
                }
            }

            trailingText?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MutedText,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MutedText
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                color = SoftBorder
            )
        }
    }
}

@Composable
private fun SignOutCard(
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = "Sign Out",
            color = DangerRed,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(vertical = 18.dp)
        )
    }
}