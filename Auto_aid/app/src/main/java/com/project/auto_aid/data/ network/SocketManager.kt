package com.project.auto_aid.data.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private const val TAG = "SocketManager"
    private const val SOCKET_URL = "http://127.0.0.1:5001"

    private var socket: Socket? = null

    fun getSocket(): Socket? = socket

    fun isConnected(): Boolean = socket?.connected() == true

    fun connect(
        token: String,
        onNotify: ((JSONObject) -> Unit)? = null,
        onConnected: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected")
            onConnected?.invoke()
            return
        }

        try {
            val opts = IO.Options().apply {
                forceNew = false
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 10000
                transports = arrayOf("websocket", "polling")
                extraHeaders = mapOf(
                    "Authorization" to listOf("Bearer $token")
                )
            }

            socket = IO.socket(SOCKET_URL, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected")
                onConnected?.invoke()
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d(TAG, "Socket disconnected: ${args.firstOrNull()}")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val msg = args.firstOrNull()?.toString() ?: "Unknown socket error"
                Log.e(TAG, "Socket connect error: $msg")
                onError?.invoke(msg)
            }

            socket?.on("error") { args ->
                val msg = args.firstOrNull()?.toString() ?: "Unknown socket error"
                Log.e(TAG, "Socket error: $msg")
                onError?.invoke(msg)
            }

            socket?.off("notify")
            socket?.on("notify") { args ->
                val payload = args.firstOrNull() as? JSONObject
                if (payload != null) {
                    Log.d(TAG, "notify received: $payload")
                    onNotify?.invoke(payload)
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Socket exception: ${e.message}", e)
            onError?.invoke(e.message ?: "Socket exception")
        }
    }

    fun disconnect() {
        try {
            socket?.off()
            socket?.disconnect()
            socket = null
            Log.d(TAG, "Socket disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "disconnect error: ${e.message}", e)
        }
    }

    fun joinRequestRoom(requestId: String) {
        if (requestId.isBlank()) return

        val payload = JSONObject().apply {
            put("requestId", requestId)
        }

        Log.d(TAG, "Joining request room: $requestId")
        socket?.emit("join_request_room", payload)
    }

    fun listenRequestRoomJoined(onJoined: (String) -> Unit) {
        socket?.off("request_room_joined")
        socket?.on("request_room_joined") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val requestId = data.optString("requestId")
                Log.d(TAG, "request_room_joined: $data")
                if (requestId.isNotBlank()) onJoined(requestId)
            } catch (e: Exception) {
                Log.e(TAG, "listenRequestRoomJoined error: ${e.message}", e)
            }
        }
    }

    fun listenRequestRoomError(onError: (String) -> Unit) {
        socket?.off("request_room_error")
        socket?.on("request_room_error") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject
                val message = data?.optString("message")
                    ?: args.firstOrNull()?.toString()
                    ?: "Unknown request room error"

                Log.e(TAG, "request_room_error: $message")
                onError(message)
            } catch (e: Exception) {
                Log.e(TAG, "listenRequestRoomError error: ${e.message}", e)
                onError(e.message ?: "Unknown request room error")
            }
        }
    }

    fun listenProviderLocation(
        onUpdate: (providerId: String?, lat: Double, lng: Double, requestId: String?) -> Unit
    ) {
        socket?.off("provider_live_location")
        socket?.on("provider_live_location") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val lat = data.optDouble("lat", Double.NaN)
                val lng = data.optDouble("lng", Double.NaN)

                if (lat.isNaN() || lng.isNaN()) {
                    Log.w(TAG, "provider_live_location ignored: invalid lat/lng -> $data")
                    return@on
                }

                val providerId = data.optString("providerId", null)
                val requestId = data.optString("requestId", null)

                Log.d(TAG, "provider_live_location: $data")
                onUpdate(providerId, lat, lng, requestId)
            } catch (e: Exception) {
                Log.e(TAG, "listenProviderLocation error: ${e.message}", e)
            }
        }
    }

    fun stopListeningProviderLocation() {
        socket?.off("provider_live_location")
    }

    fun listenRequestUpdated(onUpdate: (JSONObject) -> Unit) {
        socket?.off("request_updated")
        socket?.on("request_updated") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "request_updated: $data")
                onUpdate(data)
            } catch (e: Exception) {
                Log.e(TAG, "listenRequestUpdated error: ${e.message}", e)
            }
        }
    }

    fun stopListeningRequestUpdated() {
        socket?.off("request_updated")
    }

    fun listenProviderPresence(onPresence: (JSONObject) -> Unit) {
        socket?.off("provider_presence")
        socket?.on("provider_presence") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "provider_presence: $data")
                onPresence(data)
            } catch (e: Exception) {
                Log.e(TAG, "listenProviderPresence error: ${e.message}", e)
            }
        }
    }

    fun listenProviderLocationUpdated(onUpdated: (JSONObject) -> Unit) {
        socket?.off("provider_location_updated")
        socket?.on("provider_location_updated") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "provider_location_updated: $data")
                onUpdated(data)
            } catch (e: Exception) {
                Log.e(TAG, "listenProviderLocationUpdated error: ${e.message}", e)
            }
        }
    }

    fun listenNewRequestBroadcast(onBroadcast: (JSONObject) -> Unit) {
        socket?.off("new_request_broadcast")
        socket?.on("new_request_broadcast") { args ->
            try {
                val data = args.firstOrNull() as? JSONObject ?: return@on
                Log.d(TAG, "new_request_broadcast: $data")
                onBroadcast(data)
            } catch (e: Exception) {
                Log.e(TAG, "listenNewRequestBroadcast error: ${e.message}", e)
            }
        }
    }

    fun stopListeningNewRequestBroadcast() {
        socket?.off("new_request_broadcast")
    }

    fun sendProviderLocation(lat: Double, lng: Double) {
        val payload = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
        }

        Log.d(TAG, "Sending provider_update_location: $payload")
        socket?.emit("provider_update_location", payload)
    }

    fun setProviderAvailability(
        isAvailable: Boolean,
        lat: Double? = null,
        lng: Double? = null
    ) {
        val payload = JSONObject().apply {
            put("isAvailable", isAvailable)
            if (lat != null) put("lat", lat)
            if (lng != null) put("lng", lng)
        }

        Log.d(TAG, "Sending provider_set_availability: $payload")
        socket?.emit("provider_set_availability", payload)
    }

    fun providerPing() {
        socket?.emit("provider_ping")
    }

    fun broadcastRequestToProviders(requestId: String) {
        if (requestId.isBlank()) return

        val payload = JSONObject().apply {
            put("requestId", requestId)
        }

        Log.d(TAG, "broadcast_request_to_providers: $payload")
        socket?.emit("broadcast_request_to_providers", payload)
    }

    fun removeNotifyListener() {
        socket?.off("notify")
    }

    fun clearTrackingListeners() {
        socket?.off("provider_live_location")
        socket?.off("request_updated")
        socket?.off("new_request_broadcast")
        socket?.off("request_room_joined")
        socket?.off("request_room_error")
        socket?.off("provider_presence")
        socket?.off("provider_location_updated")
    }
}