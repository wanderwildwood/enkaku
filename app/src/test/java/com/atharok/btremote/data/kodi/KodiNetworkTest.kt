package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import java.net.InetAddress
import java.net.ServerSocket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KodiNetworkTest {
    private fun local(ip: String) = KodiNetwork.isLocal(InetAddress.getByName(ip))

    @Test fun `private and link-local ranges are local`() {
        listOf("10.1.2.3", "172.16.0.1", "172.31.255.254", "192.168.1.205", "169.254.3.4", "127.0.0.1",
            "100.64.0.1", "100.127.255.1", "fd12:3456::1", "fe80::1", "::1").forEach { assertTrue(it, local(it)) }
    }

    @Test fun `public addresses are not local`() {
        listOf("8.8.8.8", "172.15.0.1", "172.32.0.1", "100.63.0.1", "100.128.0.1", "1.1.1.1", "2001:4860:4860::8888")
            .forEach { assertFalse(it, local(it)) }
    }

    @Test fun `a public literal resolves to nothing`() {
        assertNull(KodiNetwork.resolveLocal("8.8.8.8"))
        assertEquals("192.168.1.205", KodiNetwork.resolveLocal("192.168.1.205")!!.hostAddress)
    }

    @Test fun `a login is never sent to a public address`() = runBlocking {
        val reply = KodiClient().call(KodiHost("x", "8.8.8.8", 8080, "kodi", "secret"), "JSONRPC.Ping")
        assertEquals(KodiReply.NotLocal, reply)
    }

    @Test fun `a local Kodi answers over the socket client`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val t = Thread {
                server.accept().use { s ->
                    val input = s.getInputStream()
                    val seen = StringBuilder()
                    while (!seen.endsWith("\r\n\r\n")) seen.append(input.read().toChar())
                    val len = Regex("Content-Length: (\\d+)").find(seen)!!.groupValues[1].toInt()
                    input.readNBytes(len)
                    val body = "{\"id\":1,\"jsonrpc\":\"2.0\",\"result\":\"pong\"}"
                    s.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body").toByteArray())
                }
            }
            t.start()
            val reply = runBlocking { KodiClient().ping(KodiHost("x", "127.0.0.1", server.localPort)) }
            assertTrue(reply is KodiReply.Ok)
            t.join()
        }
    }

    @Test fun `chunked and read-to-end answers are read`() {
        val chunked = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n6\r\n world\r\n0\r\n\r\n"
        assertEquals("hello world", KodiHttp.readAnswer(chunked.byteInputStream()).body)
        val bare = "HTTP/1.0 401 Unauthorized\r\n\r\nno"
        val a = KodiHttp.readAnswer(bare.byteInputStream())
        assertEquals(401, a.status)
        assertEquals("no", a.body)
    }
}
