package com.project.auto_aid.screens

import androidx.compose.foundation.Image
import com.project.auto_aid.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun MaintenanceScreen(
    navController: NavHostController,
    message: String = "AutoAid is currently under maintenance. Please try again later."
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.sorry),
            contentDescription = "Maintenance Image",
            modifier = Modifier
                .size(400.dp)
                .padding(bottom = 0.dp),
            contentScale = ContentScale.Crop
        )

        Card(
            modifier = Modifier.padding(8.dp)
                .padding(bottom = 40.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "AutoAid Under Maintenance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    onClick = {
                        navController.navigate("login") {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A9AD9),
                        contentColor = Color.White
                    )
                ) {
                    Text("Back to Login")
                }

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaintenanceScreenPreview() {
    val navController = rememberNavController()

    MaintenanceScreen(
        navController = navController
    )
}