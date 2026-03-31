package com.project.auto_aid.provider.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.auto_aid.provider.verification.ProviderVerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderVerificationScreen(
    navController: NavHostController,
    viewModel: ProviderVerificationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val licensePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onLicenseSelected(uri)
    }

    val businessPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onBusinessSelected(uri)
    }

    val nationalFrontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onNationalFrontSelected(uri)
    }

    val nationalBackPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onNationalBackSelected(uri)
    }

    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onProfileImageSelected(uri)
    }

    LaunchedEffect(state.success) {
        if (state.success.isNotBlank()) {
            Toast.makeText(navController.context, state.success, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error.isNotBlank()) {
            Toast.makeText(navController.context, state.error, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Verification") }
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
            if (state.loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Complete your provider verification to receive jobs.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                label = { Text("Business Type (garage/towing/fuel/ambulance)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verification Status: ${state.verificationStatus}",
                style = MaterialTheme.typography.bodyLarge
            )

            if (state.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reason: ${state.rejectionReason}",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            UploadSection(
                title = "Work License",
                uploaded = state.hasLicensePhoto,
                onUpload = { licensePicker.launch("*/*") }
            )

            UploadSection(
                title = "Business Registration Document",
                uploaded = state.hasBusinessPhoto,
                onUpload = { businessPicker.launch("*/*") }
            )

            UploadSection(
                title = "National ID Front",
                uploaded = state.hasNationalIdFrontPhoto,
                onUpload = { nationalFrontPicker.launch("*/*") }
            )

            UploadSection(
                title = "National ID Back",
                uploaded = state.hasNationalIdBackPhoto,
                onUpload = { nationalBackPicker.launch("*/*") }
            )

            UploadSection(
                title = "Profile Image",
                uploaded = state.hasProfilePhoto,
                onUpload = { profilePicker.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.submitVerification() },
                enabled = !state.submitting && !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.submitting) {
                    CircularProgressIndicator()
                } else {
                    Text("Submit Verification")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (state.canReceiveJobs) {
                    "You can receive jobs."
                } else {
                    "You cannot receive jobs until admin approval."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun UploadSection(
    title: String,
    uploaded: Boolean,
    onUpload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (uploaded) "Uploaded" else "Not uploaded",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onUpload,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uploaded) "Replace File" else "Upload File")
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}