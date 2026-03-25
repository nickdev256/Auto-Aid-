package com.project.auto_aid.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

private val AppBlue = Color(0xFF0A9AD8)
private val SoftBg = Color(0xFFF6F8FB)
private val FieldBg = Color(0xFFF1ECF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    navController: NavHostController,
    viewModel: UserInfoViewModel
) {
    val state = viewModel.uiState
    val user = state.user

    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var showVerificationSheet by remember { mutableStateOf(false) }

    val profileCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            profileBitmap = it
        }

    val documentCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            documentBitmap = it
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            documentUri = it
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.editMode) {
                                viewModel.setEditMode(false)
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.editMode) {
                        TextButton(
                            onClick = {
                                viewModel.saveEditedProfile()
                            }
                        ) {
                            Text("Save", color = AppBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->

        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(SoftBg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            user == null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(SoftBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.error.isNotBlank()) state.error else "No user data found",
                        color = Color.Gray
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(SoftBg)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                AppBlue.copy(alpha = 0.08f),
                                RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                            )
                            .padding(top = 24.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, AppBlue, CircleShape)
                                        .clickable { profileCameraLauncher.launch(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = AppBlue,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .offset(x = (-6).dp, y = (-6).dp)
                                        .size(34.dp)
                                        .background(AppBlue, CircleShape)
                                        .padding(8.dp)
                                        .clickable { viewModel.setEditMode(true) }
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = user.fullName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.email,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (state.editMode) {
                                EditField("Full Name", state.editName, viewModel::updateEditName)
                                EditField("Phone", state.editPhone, viewModel::updateEditPhone)
                                EditField("Email", state.editEmail, viewModel::updateEditEmail)
                            } else {
                                InfoRow("Phone", user.phone)
                                InfoRow("Email", user.email)
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Identity Verification",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            VerificationStatusRow(state.verificationStatus)

                            Spacer(Modifier.height(16.dp))

                            VerificationSteps(
                                when (state.verificationStatus) {
                                    "not_verified" -> 1
                                    "pending" -> 2
                                    "verified" -> 3
                                    else -> 1
                                }
                            )

                            Spacer(Modifier.height(20.dp))

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.verificationStatus != "verified",
                                onClick = {
                                    viewModel.startVerification()
                                    showVerificationSheet = true
                                }
                            ) {
                                Text(
                                    when (state.verificationStatus) {
                                        "pending" -> "CONTINUE VERIFICATION"
                                        "verified" -> "VERIFIED"
                                        else -> "START VERIFICATION"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVerificationSheet && user != null) {
        ModalBottomSheet(
            onDismissRequest = { showVerificationSheet = false }
        ) {
            Column(Modifier.padding(20.dp)) {
                when (state.verificationStep) {
                    1 -> {
                        Text("Select Document", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        DocumentOption("National ID") {
                            viewModel.selectDocument("National ID")
                        }

                        DocumentOption("Passport") {
                            viewModel.selectDocument("Passport")
                        }
                    }

                    2 -> {
                        Text(
                            text = "Upload ${state.selectedDocument ?: "Document"}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { documentCameraLauncher.launch(null) }
                        ) {
                            Text("TAKE PHOTO")
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { galleryLauncher.launch("image/*") }
                        ) {
                            Text("UPLOAD FROM GALLERY")
                        }

                        if (documentBitmap != null || documentUri != null) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.goToVerificationStep(3) }
                            ) {
                                Text("CONTINUE")
                            }
                        }
                    }

                    3 -> {
                        Text("Submit Verification", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Your document will be reviewed shortly.",
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.setPendingVerification()
                                showVerificationSheet = false
                            }
                        ) {
                            Text("SUBMIT")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppBlue,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg
            ),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VerificationStatusRow(status: String) {
    val (text, color) = when (status) {
        "verified" -> "Verified" to Color(0xFF2E7D32)
        "pending" -> "Pending Review" to Color(0xFFF9A825)
        else -> "Not Verified" to Color(0xFFC62828)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text(text, color = color)
    }
}

@Composable
private fun VerificationSteps(step: Int) {
    val steps = listOf("Document", "Capture", "Submit")

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = if (index < step) AppBlue else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White)
                    }
                }
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DocumentOption(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}