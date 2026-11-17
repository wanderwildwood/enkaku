package com.atharok.btremote.data.kodi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KodiShownTest {
    @Test
    fun `a short message is shown as it is`() {
        assertEquals("Invalid params.", KodiRpc.shown("Invalid params."))
    }

    @Test
    fun `control characters become spaces`() {
        assertEquals("one two", KodiRpc.shown("one\ntwo\u0007"))
    }

    @Test
    fun `a long message is cut short`() {
        val shown = KodiRpc.shown("x".repeat(5000))
        assertTrue(shown.length <= 121)
        assertTrue(shown.endsWith("…"))
    }

    @Test
    fun `an error from the box reaches the dialog short`() {
        val reply = KodiRpc.parse("""{"jsonrpc":"2.0","id":1,"error":{"code":-1,"message":"${"y".repeat(500)}"}}""")
        assertTrue(reply is KodiReply.Refused && reply.message.length <= 121)
    }
}
