package com.project.auto_aid.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

private val AppBlue = Color(0xFF0A9AD8)
private val SoftBg = Color(0xFFF6F8FB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(navController: NavHostController) {

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About Us") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SoftBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppBlue,
                        modifier = Modifier.size(36.dp)
                    )

                    Text(
                        text = "AutoAid",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "AutoAid is a smart roadside assistance platform designed to help drivers quickly access services such as towing, mechanics, fuel delivery, and emergency support.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Our Mission",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "To provide fast, reliable, and safe vehicle assistance anytime, anywhere.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )

                    Divider()

                    Text(
                        text = "Version",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "AutoAid v1.0.0",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}