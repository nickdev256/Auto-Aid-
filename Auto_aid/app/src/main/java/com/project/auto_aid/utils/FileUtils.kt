package com.project.auto_aid.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {

    private const val TAG = "FileUtils"

    fun bitmapToMultipart(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "image.jpg",
        partName: String = "file",
        quality: Int = 90
    ): MultipartBody.Part {
        require(fileName.isNotBlank()) { "fileName cannot be blank" }
        require(partName.isNotBlank()) { "partName cannot be blank" }
        require(quality in 0..100) { "quality must be between 0 and 100" }

        val cacheFile = File(context.cacheDir, fileName)

        try {
            FileOutputStream(cacheFile).use { outputStream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.flush()

                if (!compressed) {
                    throw IllegalStateException("Bitmap compression failed")
                }
            }

            val requestBody = cacheFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData(partName, cacheFile.name, requestBody)
        } catch (e: Exception) {
            Log.e(TAG, "bitmapToMultipart failed", e)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            throw e
        }
    }

    fun uriToMultipart(
        context: Context,
        uri: Uri,
        partName: String = "file"
    ): MultipartBody.Part {
        require(partName.isNotBlank()) { "partName cannot be blank" }

        val fileName = getFileName(context, uri) ?: "upload_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val cacheFile = File(context.cacheDir, fileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: throw IllegalStateException("Unable to open file input stream")

            val requestBody = cacheFile.asRequestBody(mimeType.toMediaTypeOrNull())
            return MultipartBody.Part.createFormData(partName, cacheFile.name, requestBody)
        } catch (e: Exception) {
            Log.e(TAG, "uriToMultipart failed", e)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            throw e
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            }

            uri.path?.substringAfterLast('/')
        } catch (e: Exception) {
            Log.e(TAG, "getFileName failed", e)
            null
        }
    }

    fun copyUriToFile(context: Context, uri: Uri, outputFile: File): File {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for uri")

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "copyUriToFile failed", e)
            throw e
        }
    }
}