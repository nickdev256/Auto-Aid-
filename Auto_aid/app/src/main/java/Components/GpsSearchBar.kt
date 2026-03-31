package com.project.auto_aid.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.project.auto_aid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLocationSearchField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit = {},
    lat: Double = 0.0,
    lng: Double = 0.0,
    onOpenMapPicker: (Double, Double) -> Unit = { _, _ -> },
    onPlacePicked: (name: String, lat: Double, lng: Double) -> Unit = { _, _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()

    var text by remember(value) { mutableStateOf(value) }

    LaunchedEffect(value) {
        text = value
    }

    fun buildBestPlaceLabel(place: Place): String {
        val name = place.name?.trim().orEmpty()
        val address = place.address?.trim().orEmpty()

        return when {
            name.isNotBlank() && address.isNotBlank() && !address.startsWith(name, ignoreCase = true) ->
                "$name, $address"

            address.isNotBlank() -> address
            name.isNotBlank() -> name
            else -> ""
        }
    }

    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val place = Autocomplete.getPlaceFromIntent(data)

            val label = buildBestPlaceLabel(place)
            val pickedLat = place.location?.latitude
            val pickedLng = place.location?.longitude

            if (label.isNotBlank() && pickedLat != null && pickedLng != null) {
                text = label
                onValueChange(label)
                onPlacePicked(label, pickedLat, pickedLng)
            }
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        placeholder = { Text("Search exact location") },
        leadingIcon = {
            IconButton(
                onClick = {
                    if (activity != null) {
                        val fields = listOf(
                            Place.Field.ID,
                            Place.Field.NAME,
                            Place.Field.ADDRESS,
                            Place.Field.LAT_LNG
                        )

                        val intent = Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.FULLSCREEN,
                            fields
                        ).build(activity)

                        launcher.launch(intent)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    onOpenMapPicker(lat, lng)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.google_maps),
                    contentDescription = "Pick exact location",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}