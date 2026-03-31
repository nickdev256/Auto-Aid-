package com.project.auto_aid.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.project.auto_aid.viewmodel.IdentityVerificationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(
    navController: NavHostController,
    viewModel: IdentityVerificationViewModel
) {
    val state = viewModel.uiState
    val user = state.user
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedBitmap = bitmap
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Camera permission is required to capture document"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUser(showLoader = false)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopAutoRefresh()
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(state.errorMessage)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(state.successMessage)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Identity Verification") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                state.isSubmitting -> Unit
                                currentStep > 0 -> currentStep -= 1
                                else -> navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        if (state.isLoadingUser && user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF6F7F9)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF6F7F9)),
                contentAlignment = Alignment.Center
            ) {
                Text("Unable to load user")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF6F7F9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (currentStep) {
                0 -> {
                    ProfileHeader(
                        fullName = user.fullName,
                        email = user.email
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionCard {
                        ProfileRow("Phone", user.phone)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileRow("Email", user.email)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Identity Verification",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionCard {
                        VerificationStatusRow(state.verificationStatus)

                        Spacer(modifier = Modifier.height(16.dp))

                        VerificationSteps(
                            step = when (state.verificationStatus.lowercase()) {
                                "not_verified" -> 1
                                "pending" -> 2
                                "verified" -> 3
                                "rejected" -> 1
                                else -> 1
                            }
                        )

                        if (state.verificationStatus.equals("rejected", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your previous verification was rejected. Please submit a clearer and valid document.",
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                capturedBitmap = null
                                currentStep = 1
                            },
                            enabled = !state.verificationStatus.equals("verified", ignoreCase = true) &&
                                    !state.verificationStatus.equals("pending", ignoreCase = true),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                when (state.verificationStatus.lowercase()) {
                                    "pending" -> "WAITING FOR APPROVAL"
                                    "verified" -> "VERIFIED"
                                    else -> "START VERIFICATION"
                                }
                            )
                        }
                    }
                }

                1 -> {
                    StepTitle("Select Document")

                    SectionCard {
                        DocumentItem("National ID") {
                            viewModel.selectDocument("National ID")
                            capturedBitmap = null
                            currentStep = 2
                        }

                        DocumentItem("Passport") {
                            viewModel.selectDocument("Passport")
                            capturedBitmap = null
                            currentStep = 2
                        }

                        DocumentItem("Driver's License") {
                            viewModel.selectDocument("Driver's License")
                            capturedBitmap = null
                            currentStep = 2
                        }
                    }
                }

                2 -> {
                    StepTitle("Capture Document")

                    if (state.selectedDocumentType.isNotBlank()) {
                        Text(
                            text = "Selected document: ${state.selectedDocumentType}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured document",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No image captured",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Take a clear photo of the selected document",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (capturedBitmap == null) "TAKE PHOTO" else "RETAKE PHOTO")
                    }

                    if (!hasCameraPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera permission required to capture document",
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { capturedBitmap = null },
                        enabled = capturedBitmap != null,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Clear")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        enabled = capturedBitmap != null,
                        onClick = { currentStep = 3 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONTINUE")
                    }
                }

                3 -> {
                    StepTitle("Submit Verification")

                    SectionCard {
                        ProfileRow(
                            label = "Document Type",
                            value = state.selectedDocumentType.ifBlank { "Not selected" }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ProfileRow(
                            label = "Photo Status",
                            value = if (capturedBitmap != null) "Captured" else "Missing"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Your document will be reviewed shortly. Once approved, your account status will change to verified.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (capturedBitmap != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Document preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = {
                            val bitmap = capturedBitmap
                            if (bitmap != null) {
                                viewModel.uploadVerification(
                                    context = context,
                                    bitmap = bitmap,
                                    documentType = state.selectedDocumentType
                                ) {
                                    capturedBitmap = null
                                    currentStep = 0
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Verification submitted")
                                    }
                                }
                            }
                        },
                        enabled = state.selectedDocumentType.isNotBlank() &&
                                capturedBitmap != null &&
                                !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0A9AD8)
                        )
                    ) {
                        if (state.isSubmitting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("SUBMITTING...")
                            }
                        } else {
                            Text("SUBMIT")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { currentStep = 2 },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    fullName: String,
    email: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, Color(0xFF0A9AD8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color(0xFF0A9AD8),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = fullName,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Text(
            text = email,
            color = Color.Gray
        )
    }
}

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun ProfileRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VerificationStatusRow(status: String) {
    val (text, color) = when (status.lowercase()) {
        "verified" -> "Verified" to Color(0xFF2E7D32)
        "pending" -> "Pending Review" to Color(0xFFF9A825)
        "rejected" -> "Rejected" to Color(0xFFC62828)
        else -> "Not Verified" to Color(0xFFC62828)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = "Verification status",
            tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun VerificationSteps(step: Int) {
    val steps = listOf("Document", "Capture", "Submit")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = if (index < step) Color(0xFF0A9AD8) else Color.LightGray,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = label,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DocumentItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Select $title"
        )
    }
}