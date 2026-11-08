package com.atharok.btremote.data.bluetooth

/**
 * Registers Remote with Bluetooth as a keyboard and mouse, and keeps trying for a while when
 * Bluetooth is still letting go of an earlier registration.
 *
 * Bluetooth can drop the registration while the phone is locked. It lets go of it only once the
 * host that was connected has gone, which can take up to about twenty seconds; until then
 * registerApp() fails. A failed registerApp() also keeps the app's place taken, so every failure
 * is followed by unregisterApp() before the next try. Without this, one failure was final and
 * only turning Bluetooth off and on cleared it.
 *
 * [register] and [unregister] wrap BluetoothHidDevice.registerApp()/unregisterApp(); [schedule]
 * runs an action later on the thread that owns this object.
 */
class HidRegistration(
    private val register: () -> Boolean,
    private val unregister: () -> Boolean,
    private val schedule: (delayMillis: Long, action: () -> Unit) -> Unit,
    private val onGiveUp: () -> Unit
) {
    private var generation = 0

    /** True from [start] until Bluetooth confirms, [cancel] is called, or the tries run out. */
    var isTrying: Boolean = false
        private set

    fun start() {
        val current = ++generation
        isTrying = true
        // A registration left behind is cleared first; Bluetooth needs a moment to let it go.
        if (unregister()) {
            schedule(SETTLE_MILLIS) { attempt(current, 0) }
        } else {
            attempt(current, 0)
        }
    }

    /** Bluetooth said the app is registered. */
    fun confirmed() {
        generation++
        isTrying = false
    }

    fun cancel() {
        generation++
        isTrying = false
    }

    private fun attempt(current: Int, tries: Int) {
        if (current != generation) return
        if (register()) {
            // Accepted; if Bluetooth never confirms it, free the place and carry on trying.
            schedule(CONFIRM_MILLIS) { if (current == generation) unregisterAndRetry(current, tries) }
            return
        }
        unregisterAndRetry(current, tries)
    }

    private fun unregisterAndRetry(current: Int, tries: Int) {
        unregister()
        if (tries >= RETRY_DELAYS_MILLIS.size) {
            isTrying = false
            onGiveUp()
            return
        }
        schedule(RETRY_DELAYS_MILLIS[tries]) { attempt(current, tries + 1) }
    }

    companion object {
        const val SETTLE_MILLIS = 500L
        const val CONFIRM_MILLIS = 5_000L

        /** About half a minute in all: longer than Bluetooth takes to notice a host has gone. */
        val RETRY_DELAYS_MILLIS = longArrayOf(500L, 1_000L, 2_000L, 4_000L, 8_000L, 16_000L)
    }
}
