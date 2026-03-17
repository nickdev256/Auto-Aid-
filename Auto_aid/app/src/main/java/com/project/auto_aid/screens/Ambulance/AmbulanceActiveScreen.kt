package com.project.auto_aid.screens.ambulance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbulanceActiveScreen(
    navController: NavHostController,
    requestId: String
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    val api = remember(context) { RetrofitClient.create(tokenStore) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var status by remember { mutableStateOf("pending") }
    var providerName by remember { mutableStateOf<String?>(null) }
    var providerPhone by remember { mutableStateOf<String?>(null) }
    var providerRating by remember { mutableStateOf<Double?>(null) }

    var chatExpanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    val messages = remember { mutableStateListOf<AmbulanceChatUiMessage>() }

    var socket by remember { mutableStateOf<Socket?>(null) }
    var chatConnected by remember { mutableStateOf(false) }
    var joinedOnce by remember { mutableStateOf(false) }

    val canChat = remember(status) { ambulanceCanChatForStatus(status) }

    // For real phone + adb reverse
    val socketBaseUrl = remember { "http://127.0.0.1:5001" }

    fun setErrorSafe(message: String?) {
        scope.launch {
            error = message
        }
    }

    fun addMessageIfMissing(
        list: SnapshotStateList<AmbulanceChatUiMessage>,
        message: AmbulanceChatUiMessage
    ) {
        if (message.id.isNotBlank() && list.none { it.id == message.id }) {
            list.add(message)
        }
    }

    fun markReadSafe(s: Socket?) {
        runCatching {
            s?.emit(
                "markRead",
                JSONObject().put("requestId", requestId)
            )
        }
    }

    LaunchedEffect(requestId) {
        loading = true
        error = null

        while (isActive) {
            try {
                val token = tokenStore.getToken()
                if (token.isNullOrBlank()) {
                    error = "Please login first."
                    loading = false
                    break
                }

                val response = api.getRequestById(requestId)

                if (!response.isSuccessful) {
                    error = "Failed to load request (HTTP ${response.code()})"
                } else {
                    val body = response.body()
                    if (body != null) {
                        status = body.status ?: "pending"
                        providerName = body.assignedProviderName
                        providerPhone = body.assignedProviderPhone
                        providerRating = body.assignedProviderRating
                        error = null
                    } else {
                        error = "Request not found"
                    }
                }

                loading = false

                if (ambulanceIsTerminalStatus(status)) break
            } catch (e: Exception) {
                loading = false
                error = e.message ?: "Failed to load request"
            }

            delay(4000)
        }
    }

    DisposableEffect(requestId, canChat) {
        if (!canChat) {
            onDispose { }
        } else {
            val token = tokenStore.getToken()

            if (token.isNullOrBlank()) {
                error = "Missing token. Login again."
                onDispose { }
            } else {
                try {
                    val opts = IO.Options().apply {
                        forceNew = true
                        reconnection = true
                        extraHeaders = mapOf(
                            "Authorization" to listOf("Bearer $token")
                        )
                    }

                    val s = IO.socket(socketBaseUrl, opts)
                    socket = s

                    s.on(Socket.EVENT_CONNECT) {
                        scope.launch {
                            chatConnected = true
                            error = null
                            s.emit("joinChat", JSONObject().put("requestId", requestId))
                            markReadSafe(s)
                        }
                    }

                    s.on(Socket.EVENT_DISCONNECT) {
                        scope.launch {
                            chatConnected = false
                        }
                    }

                    s.on(Socket.EVENT_CONNECT_ERROR) { args ->
                        val reason = args.firstOrNull()?.toString() ?: "unknown"
                        setErrorSafe("Chat connect error: $reason")
                    }

                    s.on("chat_error") { args ->
                        val payload = args.firstOrNull() as? JSONObject
                        val msg = payload?.optString("message").orEmpty()
                        setErrorSafe(if (msg.isBlank()) "Chat error" else msg)
                    }

                    s.on("chat_joined") {
                        scope.launch {
                            joinedOnce = true
                            error = null
                        }
                    }

                    s.on("chat_history") { args ->
                        runCatching {
                            val payload = args.firstOrNull() as? JSONObject ?: return@runCatching
                            val arr = payload.optJSONArray("messages") ?: JSONArray()

                            scope.launch {
                                messages.clear()
                                for (i in 0 until arr.length()) {
                                    val m = arr.getJSONObject(i)
                                    val ui = m.toUi()
                                    addMessageIfMissing(messages, ui)
                                }
                                joinedOnce = true
                                error = null
                            }
                        }.onFailure {
                            setErrorSafe(it.message ?: "Failed to load chat history")
                        }
                    }

                    s.on("new_message") { args ->
                        runCatching {
                            val payload = args.firstOrNull() as? JSONObject ?: return@runCatching
                            val m = payload.optJSONObject("message") ?: return@runCatching
                            val ui = m.toUi()

                            scope.launch {
                                addMessageIfMissing(messages, ui)
                                markReadSafe(s)
                            }
                        }.onFailure {
                            setErrorSafe(it.message ?: "Failed to receive message")
                        }
                    }

                    s.connect()
                } catch (e: URISyntaxException) {
                    error = "Socket URL invalid: ${e.message}"
                } catch (e: Exception) {
                    error = "Socket error: ${e.message}"
                }

                onDispose {
                    runCatching {
                        socket?.emit("leaveChat", JSONObject().put("requestId", requestId))
                        socket?.disconnect()
                        socket?.off()
                    }
                    socket = null
                    chatConnected = false
                    joinedOnce = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Ambulance Status",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        if (loading && status.lowercase() == "pending") {
            CircularProgressIndicator()
            Spacer(Modifier.height(10.dp))
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = ambulanceDisplayStatusText(status),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(18.dp))

        if (ambulanceShouldShowProviderCard(status)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Provider: ${providerName ?: "Unknown"}")
                    Text("Phone: ${providerPhone ?: "Unknown"}")
                    Text("Rating: ⭐ ${providerRating ?: 0.0}")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (canChat) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Chat with Provider", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (chatConnected) "Connected" else "Connecting…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = { chatExpanded = !chatExpanded }) {
                            Text(if (chatExpanded) "Hide" else "Open")
                        }
                    }

                    if (!joinedOnce) {
                        Text(
                            text = "Joining chat…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (chatExpanded) {
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 280.dp)
                        ) {
                            items(
                                items = messages,
                                key = { it.id.ifBlank { it.createdAt + it.text } }
                            ) { m ->
                                AmbulanceChatBubble(m)
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type message…") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val s = socket ?: return@Button
                                val clean = input.trim()
                                if (clean.isBlank()) return@Button

                                val payload = JSONObject()
                                    .put("requestId", requestId)
                                    .put("text", clean)

                                s.emit("sendMessage", payload)
                                input = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = socket?.connected() == true
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Send")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Back")
        }
    }
}

private fun ambulanceDisplayStatusText(status: String): String {
    return when (status.lowercase()) {
        "pending", "request_sent" -> "Request sent. Waiting for ambulance provider…"
        "assigned", "driver_assigned" -> "Ambulance assigned ✅"
        "driver_on_the_way", "ambulance_on_the_way" -> "Ambulance is on the way 🚑"
        "arrived" -> "Ambulance arrived ✅"
        "in_progress", "patient_picked" -> "Patient is being transported…"
        "at_hospital" -> "Patient has reached hospital ✅"
        "completed" -> "Ambulance request completed ✅"
        "cancelled" -> "Request cancelled ❌"
        else -> "Status: $status"
    }
}

private fun ambulanceCanChatForStatus(status: String): Boolean {
    return status.lowercase() in listOf(
        "assigned",
        "driver_assigned",
        "driver_on_the_way",
        "ambulance_on_the_way",
        "arrived",
        "in_progress",
        "patient_picked",
        "at_hospital",
        "completed"
    )
}

private fun ambulanceShouldShowProviderCard(status: String): Boolean {
    return status.lowercase() in listOf(
        "assigned",
        "driver_assigned",
        "driver_on_the_way",
        "ambulance_on_the_way",
        "arrived",
        "in_progress",
        "patient_picked",
        "at_hospital",
        "completed"
    )
}

private fun ambulanceIsTerminalStatus(status: String): Boolean {
    return status.lowercase() in listOf("completed", "cancelled")
}

private data class AmbulanceChatUiMessage(
    val id: String,
    val sender: String,
    val text: String,
    val createdAt: String
)

private fun JSONObject.toUi(): AmbulanceChatUiMessage {
    val id = optString("_id").ifBlank { optString("id") }
    val sender = optString("sender")
    val text = optString("text")
    val createdAt = optString("createdAt")

    return AmbulanceChatUiMessage(
        id = id,
        sender = sender,
        text = text,
        createdAt = createdAt
    )
}

@Composable
private fun AmbulanceChatBubble(m: AmbulanceChatUiMessage) {
    val isMe = m.sender.lowercase() == "user"

    val bg = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val fg = if (isMe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(m.text, color = fg)
            }
        }
    }
}