package com.project.auto_aid.settings

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(navController: NavHostController) {

    var verificationStatus by remember { mutableStateOf("not_verified") }
    var currentStep by remember { mutableStateOf(0) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedBitmap = bitmap
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Identity Verification") },
                navigationIcon = {
                    if (currentStep != 0) {
                        IconButton(onClick = { currentStep = 0 }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF6F7F9))
                .padding(16.dp)
        ) {

            when (currentStep) {

                // ================= PROFILE =================
                0 -> {
                    ProfileHeader()

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionCard {
                        ProfileRow("Phone", "+256 700 000 000")
                        ProfileRow("Email", "nicholas@gmail.com")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Identity Verification",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionCard {

                        VerificationStatusRow(verificationStatus)

                        Spacer(modifier = Modifier.height(16.dp))

                        VerificationSteps(
                            when (verificationStatus) {
                                "not_verified" -> 1
                                "pending" -> 2
                                else -> 3
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                verificationStatus = "pending"
                                currentStep = 1
                            },
                            enabled = verificationStatus != "verified",
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                when (verificationStatus) {
                                    "pending" -> "CONTINUE VERIFICATION"
                                    "verified" -> "VERIFIED"
                                    else -> "START VERIFICATION"
                                }
                            )
                        }
                    }
                }

                // ================= STEP 1 =================
                1 -> {
                    StepTitle("Select Document")

                    DocumentItem("National ID") { currentStep = 2 }
                    DocumentItem("Passport") { currentStep = 2 }
                }

                // ================= STEP 2 =================
                2 -> {
                    StepTitle("Capture Document")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("No image captured", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TAKE PHOTO")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        enabled = capturedBitmap != null,
                        onClick = { currentStep = 3 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CONTINUE")
                    }
                }

                // ================= STEP 3 =================
                3 -> {
                    StepTitle("Submit Verification")

                    Text(
                        text = "Your document will be reviewed shortly.",
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            verificationStatus = "verified"
                            currentStep = 0
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SUBMIT")
                    }
                }
            }
        }
    }
}

/* ---------------- COMPONENTS ---------------- */

@Composable
private fun ProfileHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, Color(0xFF0A9AD8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                null,
                tint = Color(0xFF0A9AD8),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Ssentongo Nicholas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("nicholas@gmail.com", color = Color.Gray)
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun VerificationStatusRow(status: String) {
    val (text, color) = when (status) {
        "verified" -> "Verified" to Color(0xFF2E7D32)
        "pending" -> "Pending Review" to Color(0xFFF9A825)
        else -> "Not Verified" to Color(0xFFC62828)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.VerifiedUser, null, tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VerificationSteps(step: Int) {
    val steps = listOf("Document", "Capture", "Submit")

    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        steps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Surface(
                    shape = CircleShape,
                    color = if (index < step) Color(0xFF0A9AD8) else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DocumentItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Icon(Icons.Default.ChevronRight, null)
    }
}
