package com.project.auto_aid.provider.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URISyntaxException

private const val CHAT_SOCKET_URL = "http://10.0.2.2:5001"
// For a real phone on the same Wi-Fi, replace with your PC/backend LAN IP, for example:
// private const val CHAT_SOCKET_URL = "http://192.168.1.10:5001"

@Composable
fun ProviderChatPanel(
    requestId: String,
    userPhone: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val scope = rememberCoroutineScope()
    val recorder = remember { AudioRecorderHelper(context) }

    var chatExpanded by remember { mutableStateOf(true) }
    var chatInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatUiMessage>() }

    var chatSocket by remember { mutableStateOf<Socket?>(null) }
    var chatConnected by remember { mutableStateOf(false) }
    var joinedOnce by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }

    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                recordingFile = recorder.startRecording()
                recordingStartedAt = System.currentTimeMillis()
                isRecording = true
                error = null
            }.onFailure {
                error = "Failed to start recording: ${it.message}"
            }
        } else {
            error = "Microphone permission denied"
        }
    }

    fun sanitizePhone(phone: String?): String {
        return phone
            ?.trim()
            ?.replace(" ", "")
            ?.replace("-", "")
            ?.replace("(", "")
            ?.replace(")", "")
            .orEmpty()
    }

    fun openDialer(phone: String?) {
        val cleanPhone = sanitizePhone(phone)

        if (cleanPhone.isBlank()) {
            error = "User phone number is missing"
            return
        }

        runCatching {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            context.startActivity(intent)
        }.onFailure {
            error = "Failed to open dialer: ${it.message}"
        }
    }

    fun markReadSafe(s: Socket?) {
        runCatching {
            s?.emit("markRead", JSONObject().put("requestId", requestId))
        }
    }

    fun stopAudioPlayback() {
        try {
            currentPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            currentPlayer?.release()
        } catch (_: Exception) {
        }

        currentPlayer = null
        currentlyPlayingId = null
    }

    fun playAudio(messageId: String, url: String) {
        if (url.isBlank()) {
            error = "Audio URL is empty"
            return
        }

        stopAudioPlayback()

        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(url)
                prepare()
                setOnCompletionListener {
                    stopAudioPlayback()
                }
                start()
            }
            currentPlayer = player
            currentlyPlayingId = messageId
        }.onFailure {
            error = "Audio play failed: ${it.message}"
        }
    }

    fun sendTextMessage() {
        val s = chatSocket
        val clean = chatInput.trim()

        if (s == null || !s.connected()) {
            error = "Chat is not connected yet"
            return
        }

        if (clean.isBlank()) return

        messages.add(
            ChatUiMessage(
                id = "local_${System.currentTimeMillis()}",
                sender = "provider",
                type = "text",
                text = clean,
                audioUrl = "",
                durationSec = 0,
                createdAt = ""
            )
        )

        s.emit(
            "sendMessage",
            JSONObject()
                .put("requestId", requestId)
                .put("type", "text")
                .put("text", clean)
                .put("audioUrl", "")
                .put("durationSec", 0)
        )

        chatInput = ""
        error = null
    }

    fun uploadAndSendVoice(file: File, durationSec: Int) {
        val s = chatSocket

        if (s == null || !s.connected()) {
            error = "Chat is not connected yet"
            return
        }

        if (!file.exists()) {
            error = "Recorded file not found"
            return
        }

        scope.launch(Dispatchers.IO) {
            runCatching {
                val api = RetrofitClient.create(tokenStore)
                val audioUrl = uploadVoiceFile(file, api)

                if (audioUrl.isBlank()) {
                    throw Exception("Server returned empty audio URL")
                }

                withContext(Dispatchers.Main) {
                    messages.add(
                        ChatUiMessage(
                            id = "local_voice_${System.currentTimeMillis()}",
                            sender = "provider",
                            type = "voice",
                            text = "",
                            audioUrl = audioUrl,
                            durationSec = durationSec,
                            createdAt = ""
                        )
                    )

                    s.emit(
                        "sendMessage",
                        JSONObject()
                            .put("requestId", requestId)
                            .put("type", "voice")
                            .put("text", "")
                            .put("audioUrl", audioUrl)
                            .put("durationSec", durationSec)
                    )

                    error = null
                }
            }.onFailure {
                scope.launch(Dispatchers.Main) {
                    error = "Voice upload failed: ${it.message}"
                }
            }
        }
    }

    fun stopRecordingAndSend() {
        val stoppedFile = recorder.stopRecording()
        val file = stoppedFile ?: recordingFile
        val durationMillis = System.currentTimeMillis() - recordingStartedAt
        val durationSec = (durationMillis / 1000L).toInt().coerceAtLeast(1)

        isRecording = false
        recordingStartedAt = 0L
        recordingFile = null

        if (file == null || !file.exists()) {
            error = "Recording failed"
            return
        }

        uploadAndSendVoice(file, durationSec)
    }

    LaunchedEffect(requestId) {
        if (chatSocket != null) return@LaunchedEffect

        val token = tokenStore.getToken()
        if (token.isNullOrBlank()) {
            error = "Provider not logged in. Login again to use chat."
            return@LaunchedEffect
        }

        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
                transports = arrayOf("websocket", "polling")
            }

            val s = IO.socket(CHAT_SOCKET_URL, opts)
            chatSocket = s

            s.off()

            s.on(Socket.EVENT_CONNECT) {
                scope.launch(Dispatchers.Main) {
                    chatConnected = true
                    error = null
                }
                s.emit("joinChat", JSONObject().put("requestId", requestId))
                markReadSafe(s)
            }

            s.on(Socket.EVENT_DISCONNECT) {
                scope.launch(Dispatchers.Main) {
                    chatConnected = false
                }
            }

            s.on(Socket.EVENT_CONNECT_ERROR) { args ->
                scope.launch(Dispatchers.Main) {
                    error = "Chat connect error: ${args.firstOrNull() ?: "unknown"}"
                }
            }

            s.on("chat_error") { args ->
                val payload = args.firstOrNull() as? JSONObject
                scope.launch(Dispatchers.Main) {
                    error = payload?.optString("message") ?: "Chat error"
                }
            }

            s.on("chat_joined") {
                scope.launch(Dispatchers.Main) {
                    joinedOnce = true
                }
            }

            s.on("chat_history") { args ->
                runCatching {
                    val payload = args.firstOrNull() as? JSONObject ?: return@runCatching
                    val arr = payload.optJSONArray("messages") ?: JSONArray()
                    val parsed = mutableListOf<ChatUiMessage>()

                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val ui = m.toUi()
                        if (ui.id.isNotBlank() && parsed.none { it.id == ui.id }) {
                            parsed.add(ui)
                        }
                    }

                    scope.launch(Dispatchers.Main) {
                        messages.clear()
                        messages.addAll(parsed)
                        joinedOnce = true
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        error = "Failed to load chat history: ${it.message}"
                    }
                }
            }

            s.on("new_message") { args ->
                runCatching {
                    val payload = args.firstOrNull() as? JSONObject ?: return@runCatching
                    val m = payload.optJSONObject("message") ?: return@runCatching
                    val ui = m.toUi()

                    scope.launch(Dispatchers.Main) {
                        if (ui.id.isNotBlank() && messages.none { it.id == ui.id }) {
                            messages.add(ui)
                        }
                        markReadSafe(s)
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        error = "Failed to receive new message: ${it.message}"
                    }
                }
            }

            s.connect()
        } catch (e: URISyntaxException) {
            error = "Chat socket URL invalid: ${e.message}"
        } catch (e: Exception) {
            error = "Chat socket error: ${e.message}"
        }
    }

    DisposableEffect(requestId) {
        onDispose {
            runCatching {
                chatSocket?.emit("leaveChat", JSONObject().put("requestId", requestId))
                chatSocket?.off()
                chatSocket?.disconnect()
            }
            recorder.release()
            stopAudioPlayback()
            chatSocket = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Chat with User", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (chatConnected) "Connected" else "Connecting…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!joinedOnce) {
                        Text(
                            "Joining chat…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isRecording) {
                        Text(
                            "Recording voice note…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!userPhone.isNullOrBlank()) {
                        Text(
                            text = "User Phone: ${sanitizePhone(userPhone)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    error?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                TextButton(onClick = { chatExpanded = !chatExpanded }) {
                    Text(if (chatExpanded) "Hide" else "Open")
                }
            }

            if (chatExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { m ->
                        ProviderChatBubble(
                            m = m,
                            isPlaying = currentlyPlayingId == m.id,
                            onPlayVoice = {
                                if (m.audioUrl.isNotBlank()) {
                                    playAudio(m.id, m.audioUrl)
                                }
                            },
                            onStopVoice = {
                                stopAudioPlayback()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                stopRecordingAndSend()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (granted) {
                                    runCatching {
                                        recordingFile = recorder.startRecording()
                                        recordingStartedAt = System.currentTimeMillis()
                                        isRecording = true
                                        error = null
                                    }.onFailure {
                                        error = "Failed to start recording: ${it.message}"
                                    }
                                } else {
                                    recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        enabled = chatSocket?.connected() == true
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop recording" else "Record voice"
                        )
                    }

                    IconButton(
                        onClick = {
                            val cleanPhone = sanitizePhone(userPhone)
                            if (cleanPhone.isBlank()) {
                                error = "User phone number is missing"
                                return@IconButton
                            }

                            openDialer(cleanPhone)

                            if (chatSocket?.connected() == true) {
                                chatSocket?.emit(
                                    "sendMessage",
                                    JSONObject()
                                        .put("requestId", requestId)
                                        .put("type", "call")
                                        .put("text", "Provider opened phone dialer")
                                        .put("audioUrl", "")
                                        .put("durationSec", 0)
                                )
                            }
                        },
                        enabled = userPhone?.isNotBlank() == true
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call User")
                    }

                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type message…") },
                        singleLine = true
                    )

                    Button(
                        onClick = { sendTextMessage() },
                        enabled = chatSocket?.connected() == true
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                    }
                }
            }
        }
    }
}

/* =========================
   Chat helpers
========================= */

data class ChatUiMessage(
    val id: String,
    val sender: String,
    val type: String = "text",
    val text: String = "",
    val audioUrl: String = "",
    val durationSec: Int = 0,
    val createdAt: String
)

private fun JSONObject.toUi(): ChatUiMessage {
    val id = optString("_id").ifBlank { optString("id") }

    return ChatUiMessage(
        id = id.ifBlank { "msg_${System.currentTimeMillis()}" },
        sender = optString("sender"),
        type = optString("type", "text"),
        text = optString("text"),
        audioUrl = optString("audioUrl"),
        durationSec = optInt("durationSec", 0),
        createdAt = optString("createdAt")
    )
}

@Composable
fun ProviderChatBubble(
    m: ChatUiMessage,
    isPlaying: Boolean,
    onPlayVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    val isMe = m.sender.lowercase() == "provider"

    val bg =
        if (isMe) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant

    val fg =
        if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(10.dp)) {
                when (m.type.lowercase()) {
                    "text" -> {
                        Text(m.text, color = fg)
                    }

                    "voice" -> {
                        Text("🎤 Voice note", color = fg, fontWeight = FontWeight.Bold)
                        if (m.durationSec > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Duration: ${m.durationSec}s", color = fg)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (isPlaying) onStopVoice() else onPlayVoice()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "Stop" else "Play")
                            }
                        }
                    }

                    "call" -> {
                        Text(
                            "📞 ${if (m.text.isNotBlank()) m.text else "Call activity"}",
                            color = fg
                        )
                    }

                    else -> {
                        Text(
                            if (m.text.isNotBlank()) m.text else "Unsupported message",
                            color = fg
                        )
                    }
                }
            }
        }
    }
}