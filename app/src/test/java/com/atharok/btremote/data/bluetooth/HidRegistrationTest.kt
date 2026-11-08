package com.atharok.btremote.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HidRegistrationTest {

    /** A stand-in for Bluetooth: registerApp() fails [failures] times, then is accepted. */
    private class Fake(var failures: Int, var heldBefore: Boolean = false) {
        val calls = mutableListOf<String>()
        val pending = mutableListOf<Pair<Long, () -> Unit>>()
        var gaveUp = false

        val registration = HidRegistration(
            register = {
                calls += "register"
                if (failures > 0) { failures--; false } else true
            },
            unregister = {
                calls += "unregister"
                val had = heldBefore
                heldBefore = false
                had
            },
            schedule = { delay, action -> pending += delay to action },
            onGiveUp = { gaveUp = true }
        )

        /** Runs the next scheduled action; returns its delay. */
        fun step(): Long {
            val (delay, action) = pending.removeAt(0)
            action()
            return delay
        }
    }

    @Test
    fun withNothingLeftOverItRegistersAtOnce() {
        val fake = Fake(failures = 0)
        fake.registration.start()
        assertEquals(listOf("unregister", "register"), fake.calls)
        assertTrue(fake.registration.isTrying)
        fake.registration.confirmed()
        assertFalse(fake.registration.isTrying)
    }

    @Test
    fun aLeftoverRegistrationIsClearedAndGivenTimeBeforeRegistering() {
        val fake = Fake(failures = 0, heldBefore = true)
        fake.registration.start()
        assertEquals(listOf("unregister"), fake.calls)
        assertEquals(HidRegistration.SETTLE_MILLIS, fake.step())
        assertEquals(listOf("unregister", "register"), fake.calls)
    }

    @Test
    fun everyFailureFreesThePlaceAndTriesAgainLater() {
        val fake = Fake(failures = 2)
        fake.registration.start()
        assertEquals(listOf("unregister", "register", "unregister"), fake.calls)
        assertEquals(HidRegistration.RETRY_DELAYS_MILLIS[0], fake.step())
        assertEquals(HidRegistration.RETRY_DELAYS_MILLIS[1], fake.step())
        assertEquals(
            listOf("unregister", "register", "unregister", "register", "unregister", "register"),
            fake.calls
        )
        assertFalse(fake.gaveUp)
    }

    @Test
    fun givesUpOnlyAfterEveryTry() {
        val fake = Fake(failures = Int.MAX_VALUE)
        fake.registration.start()
        var waited = 0L
        while (fake.pending.isNotEmpty()) waited += fake.step()
        assertTrue(fake.gaveUp)
        assertFalse(fake.registration.isTrying)
        assertEquals(HidRegistration.RETRY_DELAYS_MILLIS.size + 1, fake.calls.count { it == "register" })
        // Long enough for Bluetooth to notice the old host has gone (up to about 20 s).
        assertTrue(waited >= 20_000L)
    }

    @Test
    fun anAcceptedRegistrationThatIsNeverConfirmedIsTriedAgain() {
        val fake = Fake(failures = 0)
        fake.registration.start()
        assertEquals(HidRegistration.CONFIRM_MILLIS, fake.step())
        assertEquals(listOf("unregister", "register", "unregister"), fake.calls)
        fake.step()
        assertEquals("register", fake.calls.last())
    }

    @Test
    fun aConfirmedRegistrationStopsTheTries() {
        val fake = Fake(failures = 1)
        fake.registration.start()
        fake.registration.confirmed()
        val before = fake.calls.size
        while (fake.pending.isNotEmpty()) fake.step()
        assertEquals(before, fake.calls.size)
        assertFalse(fake.gaveUp)
    }

    @Test
    fun cancelStopsTheTries() {
        val fake = Fake(failures = Int.MAX_VALUE)
        fake.registration.start()
        fake.registration.cancel()
        while (fake.pending.isNotEmpty()) fake.step()
        assertFalse(fake.gaveUp)
        assertEquals(listOf("unregister", "register", "unregister"), fake.calls)
    }
}
