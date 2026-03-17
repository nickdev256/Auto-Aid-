package com.project.auto_aid.provider.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun rememberProfileImagePicker(
    onUploaded: (String) -> Unit
): () -> Unit {

    val context = LocalContext.current
    val scope = rememberCoroutineScope() // ✅ create scope here (Composable context)

    val api = remember { RetrofitClient.create(TokenStore(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch(Dispatchers.IO) {
            try {
                val file = uriToFile(context, uri)

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = file.name,
                    body = requestFile
                )

                val res = api.uploadProfileImage(body)

                if (res.isSuccessful) {
                    val url = res.body()?.url.orEmpty()
                    if (url.isNotBlank()) {
                        // ✅ already on Main by default when calling onUploaded? (no, we're IO)
                        scope.launch(Dispatchers.Main) { onUploaded(url) }
                    } else {
                        Log.e("UPLOAD", "❌ upload ok but url missing")
                    }
                } else {
                    val err = res.errorBody()?.string()
                    Log.e("UPLOAD", "❌ upload failed ${res.code()} err=$err")
                }

            } catch (e: Exception) {
                Log.e("UPLOAD", "❌ ${e.message}", e)
            }
        }
    }

    return { launcher.launch("image/*") }
}

/** Converts content Uri to real File */
fun uriToFile(context: Context, uri: Uri): File {
    val input = context.contentResolver.openInputStream(uri)!!
    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { output -> input.copyTo(output) }
    return file
}