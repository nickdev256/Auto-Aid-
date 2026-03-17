package com.project.auto_aid.screens

import android.graphics.Bitmap
import android.net.Uri
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
fun UserInfoScreen(navController: NavHostController) {

    /* ---------------- STATE ---------------- */
    var editMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("Ssentongo Nicholas") }
    var phone by remember { mutableStateOf("+256 700 000 000") }
    var email by remember { mutableStateOf("nicholas@gmail.com") }

    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }

    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    /* ---------------- VERIFICATION STATE ---------------- */
    var verificationStatus by remember { mutableStateOf("not_verified") }
    var verificationStep by remember { mutableStateOf(1) }
    var selectedDocument by remember { mutableStateOf<String?>(null) }
    var documentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var showVerificationSheet by remember { mutableStateOf(false) }

    /* ---------------- LAUNCHERS ---------------- */
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
                    IconButton(onClick = {
                        if (editMode) editMode = false else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (editMode) {
                        TextButton(onClick = {
                            fullName = editName
                            phone = editPhone
                            email = editEmail
                            editMode = false
                        }) {
                            Text("Save", color = AppBlue, fontWeight = FontWeight.Bold)
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
                .background(SoftBg)
        ) {

            /* ---------- HEADER ---------- */
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
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = AppBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .offset(x = (-6).dp, y = (-6).dp)
                                .size(34.dp)
                                .background(AppBlue, CircleShape)
                                .padding(8.dp)
                                .clickable { editMode = true }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(email, fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(24.dp))

            /* ---------- PROFILE INFO ---------- */
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (editMode) {
                        EditField("Full Name", editName) { editName = it }
                        EditField("Phone", editPhone) { editPhone = it }
                        EditField("Email", editEmail) { editEmail = it }
                    } else {
                        InfoRow("Phone", phone)
                        InfoRow("Email", email)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            /* ---------- ID VERIFICATION ---------- */
            Text(
                "Identity Verification",
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

                    VerificationStatusRow(verificationStatus)

                    Spacer(Modifier.height(16.dp))

                    VerificationSteps(verificationStep)

                    Spacer(Modifier.height(20.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = verificationStatus != "verified",
                        onClick = {
                            verificationStep = 1
                            showVerificationSheet = true
                        }
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
        }
    }

    /* ---------- VERIFICATION BOTTOM SHEET ---------- */
    if (showVerificationSheet) {
        ModalBottomSheet(onDismissRequest = { showVerificationSheet = false }) {

            Column(Modifier.padding(20.dp)) {

                when (verificationStep) {

                    /* STEP 1: SELECT DOCUMENT */
                    1 -> {
                        Text("Select Document", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        DocumentOption("National ID") {
                            selectedDocument = "National ID"
                            verificationStep = 2
                        }

                        DocumentOption("Passport") {
                            selectedDocument = "Passport"
                            verificationStep = 2
                        }
                    }

                    /* STEP 2: CAPTURE OR UPLOAD */
                    2 -> {
                        Text("Upload $selectedDocument", fontWeight = FontWeight.Bold)
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
                                onClick = { verificationStep = 3 }
                            ) {
                                Text("CONTINUE")
                            }
                        }
                    }

                    /* STEP 3: SUBMIT */
                    3 -> {
                        Text("Submit Verification", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Your document will be reviewed shortly.", color = Color.Gray)

                        Spacer(Modifier.height(20.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                verificationStatus = "pending"
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

/* ---------- HELPERS ---------- */

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
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
        Icon(Icons.Default.VerifiedUser, null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text(text, color = color)
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
                    color = if (index < step) AppBlue else Color.LightGray,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
        Icon(Icons.Default.ChevronRight, null)
    }
}