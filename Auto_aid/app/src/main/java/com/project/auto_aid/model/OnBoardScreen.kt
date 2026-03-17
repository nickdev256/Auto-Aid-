@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.project.auto_aid.model

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.project.auto_aid.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.auto_aid.navigation.Routes
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun OnBoardScreen(navController: NavHostController, modifier: Modifier = Modifier) {

    val pages = listOf(
        OnBoardModel(
            title = "Instant Vehicle Assistance",
            description = "Get roadside help, towing, and emergency support anytime, anywhere with AutoAid.",
            imageRes = R.drawable.logo10,
            buttonText = "Skip"
        ),
        OnBoardModel(
            title = "Track & Manage Repairs",
            description = "Keep track of your vehicle services, repairs, and maintenance history all in one place.",
            imageRes = R.drawable.logo11,
            buttonText = "Next"
        ),
        OnBoardModel(
            title = "Smart & User-Friendly!",
            description = "Easily request assistance, track your requests, and stay updated with a simple, intuitive interface.",
            imageRes = R.drawable.fuel,
            buttonText = "Get Started"
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val model = pages[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {

                if (page == 0) {
                    OnboardVideoPlayer(
                        rawResId = R.raw.road_safety,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f)
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 80.dp,
                                    bottomEnd = 80.dp
                                )
                            )
                    )
                } else {
                    Image(
                        painter = painterResource(id = model.imageRes),
                        contentDescription = model.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f)
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 80.dp,
                                    bottomEnd = 80.dp
                                )
                            ),
                        contentScale = ContentScale.Crop
                    )
                }


                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = model.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    color = Color(0xFF0A9AD9)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = model.description,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dots indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(pages.size) { index ->
                val color =
                    if (pagerState.currentPage == index) Color(0xFF0A9AD9) else Color.Gray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        AuthenticationButton(
            title = pages[pagerState.currentPage].buttonText,
            onClick = {
                scope.launch {
                    when (pagerState.currentPage) {
                        0, 1 -> pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        pages.lastIndex -> navController.navigate(Routes.TermsAndConditionsScreen.route) {
                            popUpTo(Routes.OnBoardScreen.route) { inclusive = true }
                        }
                    }

                    }

            }
        )
    }
}

@Composable
fun AuthenticationButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(29.dp)
            .height(50.dp)
            .padding(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0A9AD9),
            contentColor = Color.White
        )
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Preview(showBackground = true)
@Composable
fun OnBoardScreenPreview() {
    // Preview shows images only (video is not preview-friendly)
    OnBoardScreen(navController = rememberNavController())
}