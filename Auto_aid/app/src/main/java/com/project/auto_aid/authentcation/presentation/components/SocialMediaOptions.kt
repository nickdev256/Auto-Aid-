package com.project.auto_aid.authentcation.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.auto_aid.R

@Composable
fun SocialMediaOptions(
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onTikTokClick: () -> Unit,
    onInstagramClick: () -> Unit
) {
    // ✅ Explicit type fixes ALL errors
    val socialMediaItems: List<Pair<Int, () -> Unit>> = listOf(
        R.drawable.gmail to onGoogleClick,
        R.drawable.fb to onFacebookClick,
        R.drawable.ticktok to onTikTokClick,
        R.drawable.gram to onInstagramClick
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(15.dp,
            Alignment.CenterHorizontally)
    ) {
        socialMediaItems.forEach { (icon, action) ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { action() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
