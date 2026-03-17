package com.project.auto_aid.provider.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.auto_aid.provider.model.Provider

@Composable
fun ProviderProfileCard(
    provider: Provider,
    onOnlineChange: (Boolean) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChangeProfileImage: () -> Unit = {}
) {
    // ✅ start from saved provider state
    var isOnline by remember(provider.isOnline) { mutableStateOf(provider.isOnline) }

    val serviceColor = when (provider.providerType.lowercase()) {
        "garage" -> Color(0xFF0A9AD9)
        "towing" -> Color(0xFFFF9800)
        "fuel" -> Color(0xFF4CAF50)
        "ambulance" -> Color(0xFFE53935)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {

            /* ---------------- HEADER GRADIENT ---------------- */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(serviceColor.copy(.15f), Color.White)
                        )
                    )
                    .padding(16.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    /* PROFILE IMAGE */
                    Box(contentAlignment = Alignment.BottomEnd) {

                        if (provider.profileImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = provider.profileImageUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, serviceColor, CircleShape)
                                    .clickable { onChangeProfileImage() }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                                    .clickable { onChangeProfileImage() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        /* CAMERA BADGE */
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(serviceColor)
                                .clickable { onChangeProfileImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Text(
                                provider.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.width(8.dp))

                            // ✅ show verified badge only if verified
                            if (provider.isVerified) {
                                Column(
                                    modifier = Modifier
                                        .background(Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("✔", color = Color.White)
                                    Text(
                                        "VER",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Text(
                            provider.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    TextButton(onClick = onEditProfile) {
                        Text("Edit", color = serviceColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            /* ---------------- STATUS ROW ---------------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                AssistChip(
                    onClick = {},
                    label = { Text(provider.providerType.uppercase()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = serviceColor.copy(.15f),
                        labelColor = serviceColor
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (isOnline) "ONLINE" else "OFFLINE",
                                color = if (isOnline) Color(0xFF4CAF50) else Color.Gray
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor =
                                if (isOnline) Color(0xFF4CAF50).copy(.15f)
                                else Color.LightGray
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    Switch(
                        checked = isOnline,
                        onCheckedChange = {
                            isOnline = it
                            onOnlineChange(it)
                        }
                    )
                }
            }

            Divider()

            /* ---------------- STATS ---------------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard("Rating", "⭐ ${provider.rating}")
                StatCard("Today", "UGX ${provider.earningsToday.toInt()}")
                StatCard("Week", "UGX ${provider.earningsWeek.toInt()}")
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}