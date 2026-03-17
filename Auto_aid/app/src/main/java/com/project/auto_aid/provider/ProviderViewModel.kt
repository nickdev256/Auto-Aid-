package com.project.auto_aid.provider

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.provider.model.ProviderRequest
import kotlinx.coroutines.launch

class ProviderViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProviderRepository(application.applicationContext)

    val pending = mutableStateListOf<ProviderRequest>()
    val ongoing = mutableStateListOf<ProviderRequest>()
    val completed = mutableStateListOf<ProviderRequest>()

    // ✅ track last seen pending IDs so sound plays only for new incoming requests
    private val seenPendingIds = mutableSetOf<String>()

    // ✅ simple notification sound
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)

    fun start(providerType: String, providerId: String) {
        repo.listenRequests(providerType, providerId) { list ->

            // --- detect new pending IDs ---
            val incomingPendingIds = list
                .asSequence()
                .filter { it.status.equals("pending", ignoreCase = true) }
                .map { it.id }
                .filter { it.isNotBlank() }
                .toSet()

            val hasNewPending = incomingPendingIds.any { it !in seenPendingIds }

            // update seen set
            seenPendingIds.addAll(incomingPendingIds)

            // play sound only on new pending requests
            if (hasNewPending) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }

            // --- rebuild UI lists safely ---
            pending.clear()
            ongoing.clear()
            completed.clear()

            list.forEach { req ->
                when (req.status.trim().lowercase()) {
                    "pending" -> pending.add(req)

                    // ✅ history statuses
                    "completed", "cancelled" -> completed.add(req)

                    // ✅ active statuses (assigned/arrived/in_progress etc.)
                    else -> ongoing.add(req)
                }
            }

            // optional: keep lists stable order
            pending.sortByDescending { it.id }    // replace with createdAt if you have it
            ongoing.sortByDescending { it.id }
            completed.sortByDescending { it.id }
        }
    }

    fun accept(requestId: String) {
        viewModelScope.launch {
            repo.assignRequest(requestId)
        }
    }

    fun decline(requestId: String) {
        viewModelScope.launch {
            val ok = repo.declineRequest(requestId)
            if (ok) {
                pending.removeAll { it.id == requestId }
                seenPendingIds.remove(requestId)
            }
        }
    }

    fun updateStatus(requestId: String, status: String) {
        viewModelScope.launch {
            repo.updateStatus(requestId, status)
        }
    }

    fun listenUserLocation(
        requestId: String,
        onUpdate: (Double, Double) -> Unit
    ) {
        repo.listenUserLocation(requestId, onUpdate)
    }

    override fun onCleared() {
        super.onCleared()
        repo.stopListening()
        tone.release()
    }
}