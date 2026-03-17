package com.project.auto_aid.components

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLocationSearchField(
    modifier: Modifier = Modifier,
    value: String,
    lat: Double = 0.0,
    lng: Double = 0.0,
    onValueChange: (String) -> Unit = {},
    onOpenMapPicker: (lat: Double, lng: Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fine || coarse) {
            scope.launch {
                val loc = getLastLocationLatLng(context) ?: (lat to lng)

                if (loc.first == 0.0 && loc.second == 0.0) {
                    Toast.makeText(
                        context,
                        "Could not get GPS. Turn on Location.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                onOpenMapPicker(loc.first, loc.second)
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search by location") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Pick exact location"
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

private suspend fun getLastLocationLatLng(context: Context): Pair<Double, Double>? {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val location = getLastLocationSafe(fusedClient) ?: return null
    return location.latitude to location.longitude
}

suspend fun reverseGeocodeLabel(context: Context, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())

        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1)
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lng, 1)
        }

        val addr = addresses?.firstOrNull()

        when {
            addr == null -> "${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
            !addr.featureName.isNullOrBlank() && !addr.locality.isNullOrBlank() ->
                "${addr.featureName}, ${addr.locality}"
            !addr.locality.isNullOrBlank() -> addr.locality!!
            else -> addr.getAddressLine(0) ?: "${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
        }
    }
}

private suspend fun getLastLocationSafe(
    fusedClient: FusedLocationProviderClient
): android.location.Location? {
    return suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (cont.isActive) cont.resume(location)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }
}