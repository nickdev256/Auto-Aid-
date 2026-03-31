package com.project.auto_aid.provider.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.provider.verification.ProviderVerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProviderProfileScreen(
    navController: NavHostController,
    viewModel: ProviderVerificationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success.isNotBlank()) {
            Toast.makeText(
                navController.context,
                state.success,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error.isNotBlank()) {
            Toast.makeText(
                navController.context,
                state.error,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.clearMessage()
        }
    }

    val profileImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfileImage(navController.context, it)
        }
    }

    val licensePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadLicenseDocument(navController.context, it)
        }
    }

    val businessDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadBusinessDocument(navController.context, it)
        }
    }

    val nationalIdFrontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadNationalIdFront(navController.context, it)
        }
    }

    val nationalIdBackPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadNationalIdBack(navController.context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Verification & Profile") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            if (state.loading || state.submitting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Business Information",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.businessType,
                onValueChange = viewModel::onBusinessTypeChange,
                label = { Text("Business Type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Verification Status",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Status: ${state.verificationStatus.ifBlank { "not_verified" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = when (state.verificationStatus.lowercase()) {
                    "verified" -> Color(0xFF15803D)
                    "rejected" -> MaterialTheme.colorScheme.error
                    "pending" -> Color(0xFFD97706)
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )

            if (state.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reason: ${state.rejectionReason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Upload Documents",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            UploadDocumentCard(
                title = "Profile Photo",
                uploaded = state.profileImageUrl.isNotBlank(),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null
                    )
                },
                onUpload = {
                    profileImagePicker.launch("image/*")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            UploadDocumentCard(
                title = "Work License",
                uploaded = state.licenseDocumentUrl.isNotBlank(),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null
                    )
                },
                onUpload = {
                    licensePicker.launch("*/*")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            UploadDocumentCard(
                title = "Business Document",
                uploaded = state.businessDocumentUrl.isNotBlank(),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = null
                    )
                },
                onUpload = {
                    businessDocPicker.launch("*/*")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            UploadDocumentCard(
                title = "National ID Front",
                uploaded = state.nationalIdFrontUrl.isNotBlank(),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null
                    )
                },
                onUpload = {
                    nationalIdFrontPicker.launch("image/*")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            UploadDocumentCard(
                title = "National ID Back",
                uploaded = state.nationalIdBackUrl.isNotBlank(),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null
                    )
                },
                onUpload = {
                    nationalIdBackPicker.launch("image/*")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveProfile() },
                enabled = !state.submitting && !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Save Profile")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UploadDocumentCard(
    title: String,
    uploaded: Boolean,
    icon: @Composable () -> Unit,
    onUpload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (uploaded) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = if (uploaded) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (uploaded) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (uploaded) "Uploaded" else "Not uploaded",
                color = if (uploaded) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onUpload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uploaded) "Replace File" else "Upload File")
            }
        }
    }
}