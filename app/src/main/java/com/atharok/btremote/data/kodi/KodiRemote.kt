package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Kodi the remote screen is driving, while it is driving one.
 *
 * There is no connection to keep up: each press is one HTTP request, sent in order, one at a
 * time. Only a few presses wait their turn and the rest are let go. A held button repeats
 * only once Kodi has answered the one before, so on a slow network it goes slower rather
 * than running on after the finger lifts.
 */
class KodiRemote(private val client: KodiClient) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<KodiHost?>(null)
    val session: StateFlow<KodiHost?> = _session.asStateFlow()

    private val _failures = MutableSharedFlow<KodiReply>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** A press Kodi did not take, so the screen can say so rather than stay silent. */
    val failures: SharedFlow<KodiReply> = _failures.asSharedFlow()

    private var repeatJob: Job? = null

    /** Presses waiting or on their way. */
    private val pending = AtomicInteger(0)

    // A full queue turns a press away (trySend fails) rather than dropping it unseen, so the
    // count of what is pending stays true.
    private val queue = Channel<KodiCommand>(capacity = 4)

    init {
        scope.launch {
            for (command in queue) {
                try {
                    val host = _session.value ?: continue
                    val reply = client.run(host, command)
                    if (reply !is KodiReply.Ok && _session.value == host) _failures.tryEmit(reply)
                } finally {
                    pending.decrementAndGet()
                }
            }
        }
    }

    fun open(host: KodiHost) {
        _session.value = host
    }

    fun close() {
        _session.value = null
        repeatJob?.cancel()
        repeatJob = null
        while (queue.tryReceive().isSuccess) pending.decrementAndGet() // let go of what was waiting
    }

    /**
     * A report from the remote screen. False when the button means nothing to Kodi.
     *
     * The screen sends a press when a finger lands and a release when it lifts. Any report
     * that follows a press, the release first among them, ends its repeating.
     */
    fun sendReport(reportId: Int, bytes: ByteArray): Boolean {
        if (_session.value == null) return false
        repeatJob?.cancel()
        repeatJob = null
        val command = KodiInputMapper.forReport(reportId, bytes) ?: return false
        val sent = enqueue(command)
        if (KodiInputMapper.repeatsWhileHeld(command)) {
            repeatJob = scope.launch {
                delay(REPEAT_AFTER_MILLIS)
                // Bounded, so a release that never came cannot leave it running for long.
                repeat(REPEAT_MAX) {
                    if (pending.get() == 0) enqueue(command)
                    delay(REPEAT_EVERY_MILLIS)
                }
            }
        }
        return sent
    }

    fun sendText(text: String, done: Boolean): Boolean {
        if (_session.value == null) return false
        return enqueue(KodiCommand.text(text, done))
    }

    private fun enqueue(command: KodiCommand): Boolean {
        pending.incrementAndGet()
        val sent = queue.trySend(command).isSuccess
        if (!sent) pending.decrementAndGet()
        return sent
    }

    private companion object {
        // Near what Android's own key repeat does with a held key.
        const val REPEAT_AFTER_MILLIS = 400L
        const val REPEAT_EVERY_MILLIS = 150L
        const val REPEAT_MAX = 100
    }
}
