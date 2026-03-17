package com.project.auto_aid.provider.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording: Boolean = false

    fun startRecording(): File {
        stopAndReleaseSilently()

        val file = File(
            context.cacheDir,
            "voice_${System.currentTimeMillis()}.m4a"
        )
        outputFile = file

        val mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            isRecording = true
            return file
        } catch (e: Exception) {
            try {
                mediaRecorder.release()
            } catch (_: Exception) {
            }

            recorder = null
            isRecording = false
            outputFile = null

            if (file.exists()) {
                file.delete()
            }

            throw Exception("Could not start audio recording: ${e.message}", e)
        }
    }

    fun stopRecording(): File? {
        val currentRecorder = recorder ?: return null
        val file = outputFile

        return try {
            if (isRecording) {
                currentRecorder.stop()
            }
            currentRecorder.release()
            recorder = null
            isRecording = false
            outputFile = null

            if (file != null && file.exists() && file.length() > 0L) file else null
        } catch (e: Exception) {
            try {
                currentRecorder.release()
            } catch (_: Exception) {
            }

            recorder = null
            isRecording = false
            outputFile = null

            if (file != null && file.exists() && file.length() == 0L) {
                file.delete()
            }

            null
        }
    }

    fun release() {
        stopAndReleaseSilently()
        outputFile = null
    }

    private fun stopAndReleaseSilently() {
        try {
            if (isRecording) {
                recorder?.stop()
            }
        } catch (_: Exception) {
        }

        try {
            recorder?.release()
        } catch (_: Exception) {
        }

        recorder = null
        isRecording = false
    }
}