package com.atharok.btremote.data.kodi

import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** A hostile box on the home network must not make Remote read without end. */
class KodiHttpLimitsTest {

    /** [prefix], then [filler] for ever: what a box that never stops talking sends. */
    private fun endless(prefix: String, filler: Char = 'a'): InputStream = object : InputStream() {
        private val head = prefix.toByteArray(Charsets.ISO_8859_1)
        private var at = 0
        override fun read(): Int = if (at < head.size) head[at++].toInt() and 0xFF else filler.code
    }

    private fun fails(input: InputStream) {
        assertThrows(IOException::class.java) { KodiHttp.readAnswer(input) }
    }

    @Test fun `a head that never ends fails`() {
        fails(endless("HTTP/1.1 200 OK\r\nX: "))
    }

    @Test fun `a chunk size line that never ends fails`() {
        fails(endless("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n1", filler = '0'))
    }

    @Test fun `a chunk extension that never ends fails`() {
        fails(endless("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5;"))
    }

    @Test fun `a negative or huge chunk fails as IO`() {
        fails("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n-1\r\nx".byteInputStream())
        fails("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n7fffffff\r\nx".byteInputStream())
    }

    @Test fun `chunks adding up past the limit fail`() {
        val half = Integer.toHexString(KodiHttp.MAX_BODY / 2 + 1)
        fails(endless("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n$half\r\n"))
    }

    @Test fun `a body with no length that never ends fails`() {
        fails(endless("HTTP/1.1 200 OK\r\n\r\n"))
    }

    @Test fun `a declared length past the limit fails`() {
        fails("HTTP/1.1 200 OK\r\nContent-Length: ${KodiHttp.MAX_BODY + 1}\r\n\r\n".byteInputStream())
    }

    @Test fun `an ordinary answer still reads`() {
        val body = "{\"id\":1,\"jsonrpc\":\"2.0\",\"result\":\"pong\"}"
        val a = KodiHttp.readAnswer("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body".byteInputStream())
        assertEquals(200, a.status)
        assertEquals(body, a.body)
    }
}
