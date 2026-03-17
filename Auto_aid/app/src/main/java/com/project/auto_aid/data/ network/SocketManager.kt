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

    fun connect(
        token: String,
        onNotify: ((JSONObject) -> Unit)? = null,
        onConnected: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected")
            return
        }

        try {
            val opts = IO.Options().apply {
                forceNew = false
                reconnection = true
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

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val msg = args.firstOrNull()?.toString() ?: "Unknown socket error"
                Log.e(TAG, "Socket connect error: $msg")
                onError?.invoke(msg)
            }

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
        socket?.off()
        socket?.disconnect()
        socket = null
        Log.d(TAG, "Socket disconnected")
    }
}