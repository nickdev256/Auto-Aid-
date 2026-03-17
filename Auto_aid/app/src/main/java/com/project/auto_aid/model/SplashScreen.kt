package com.project.auto_aid.model

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.auto_aid.R
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.MaintenanceException
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    // Animation state
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    val tokenStore = remember { TokenStore(context) }
    val api = remember { RetrofitClient.create(tokenStore) }

    LaunchedEffect(Unit) {
        // Run animation + backend maintenance check together
        val maintenanceCheck = async {
            try {
                // This route is protected.
                // During maintenance -> interceptor throws MaintenanceException
                // Normal no-session case -> 401 response, which we ignore here
                api.getMe()
                null
            } catch (e: MaintenanceException) {
                e.message ?: "AutoAid is currently under maintenance. Please try again later."
            } catch (_: Exception) {
                null
            }
        }

        // Animate logo in
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        // Splash delay
        delay(2000)

        val maintenanceMessage = maintenanceCheck.await()

        if (!maintenanceMessage.isNullOrBlank()) {
            navController.navigate(
                Routes.MaintenanceScreen.createRoute(maintenanceMessage)
            ) {
                popUpTo(Routes.SplashScreen.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(Routes.OnBoardScreen.route) {
                popUpTo(Routes.SplashScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo01),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        10.1.dp,
                        Color(0xFF0A9AD9),
                        RoundedCornerShape(100.dp)
                    )
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        alpha = alpha.value
                    ),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "AUTO AID",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0A9AD9)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Smart Vehicle Care, Anywhere",
                fontSize = 20.sp,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}