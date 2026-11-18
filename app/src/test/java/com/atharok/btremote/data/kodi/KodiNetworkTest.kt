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
    private fun local(ip: String, vpnUp: Boolean = true) = KodiNetwork.isLocal(InetAddress.getByName(ip), vpnUp)

    @Test fun `private and link-local ranges are local`() {
        listOf("10.0.0.3", "172.16.0.1", "172.31.255.254", "192.168.1.50", "169.254.3.4", "127.0.0.1",
            "100.64.0.1", "100.127.255.1", "fd12:3456::1", "fe80::1", "::1").forEach { assertTrue(it, local(it)) }
    }

    @Test fun `the shared range 100_64 counts only while a VPN is up`() {
        listOf("100.64.0.1", "100.127.255.1").forEach { assertFalse(it, local(it, vpnUp = false)) }
        listOf("10.0.0.3", "192.168.1.50", "127.0.0.1", "fd12:3456::1").forEach { assertTrue(it, local(it, vpnUp = false)) }
        assertNull(KodiNetwork.resolveLocal("100.64.0.1", vpnUp = false))
        assertEquals("100.64.0.1", KodiNetwork.resolveLocal("100.64.0.1", vpnUp = true)!!.hostAddress)
    }

    @Test fun `a login is never sent to 100_64 without a VPN`() = runBlocking {
        val reply = KodiClient(vpnUp = { false }).call(KodiHost("x", "100.64.0.1", 8080, "kodi", "secret"), "JSONRPC.Ping")
        assertEquals(KodiReply.NotLocal, reply)
    }

    @Test fun `public addresses are not local`() {
        listOf("8.8.8.8", "172.15.0.1", "172.32.0.1", "100.63.0.1", "100.128.0.1", "1.1.1.1", "2001:4860:4860::8888")
            .forEach { assertFalse(it, local(it)) }
    }

    @Test fun `a public literal resolves to nothing`() {
        assertNull(KodiNetwork.resolveLocal("8.8.8.8", vpnUp = true))
        assertEquals("192.168.1.50", KodiNetwork.resolveLocal("192.168.1.50", vpnUp = false)!!.hostAddress)
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

    /** A Kodi with a login: answers 401 to a request without it. Returns each request's Authorization. */
    private fun kodiWithLogin(server: ServerSocket, requests: Int, seen: MutableList<String?>) = Thread {
        repeat(requests) {
            server.accept().use { s ->
                val input = s.getInputStream()
                val head = StringBuilder()
                while (!head.endsWith("\r\n\r\n")) head.append(input.read().toChar())
                val len = Regex("Content-Length: (\\d+)").find(head)!!.groupValues[1].toInt()
                input.readNBytes(len)
                val auth = Regex("Authorization: (.+)\r\n").find(head)?.groupValues?.get(1)
                synchronized(seen) { seen += auth }
                val out = s.getOutputStream()
                if (auth == null) {
                    out.write("HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\n\r\n".toByteArray())
                } else {
                    val body = "{\"id\":1,\"jsonrpc\":\"2.0\",\"result\":\"pong\"}"
                    out.write(("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body").toByteArray())
                }
            }
        }
    }.also { it.start() }

    @Test fun `the login goes only after the box asks for it, and then with every request`() {
        ServerSocket(0, 2, InetAddress.getByName("127.0.0.1")).use { server ->
            val seen = mutableListOf<String?>()
            val t = kodiWithLogin(server, 3, seen)
            val client = KodiClient()
            val host = KodiHost("x", "127.0.0.1", server.localPort, "kodi", "secret")
            assertTrue(runBlocking { client.ping(host) } is KodiReply.Ok)
            assertTrue(runBlocking { client.call(host, "JSONRPC.Ping") } is KodiReply.Ok)
            t.join()
            assertEquals(3, seen.size)
            assertNull(seen[0])
            assertTrue(seen[1]!!.startsWith("Basic "))
            assertTrue(seen[2]!!.startsWith("Basic "))
        }
    }

    @Test fun `a new connection goes without the login until the box asks again`() {
        ServerSocket(0, 2, InetAddress.getByName("127.0.0.1")).use { server ->
            val seen = mutableListOf<String?>()
            val t = kodiWithLogin(server, 4, seen)
            val client = KodiClient()
            val host = KodiHost("x", "127.0.0.1", server.localPort, "kodi", "secret")
            assertTrue(runBlocking { client.ping(host) } is KodiReply.Ok)
            assertTrue(runBlocking { client.ping(host) } is KodiReply.Ok)
            t.join()
            assertEquals(4, seen.size)
            assertNull(seen[0])
            assertTrue(seen[1]!!.startsWith("Basic "))
            assertNull(seen[2])
            assertTrue(seen[3]!!.startsWith("Basic "))
        }
    }

    @Test fun `the phone's own address towards a box is known`() {
        assertEquals("127.0.0.1", KodiNetwork.ownAddressTowards(InetAddress.getByName("127.0.0.1"), 8080))
    }

    @Test fun `a box that asks for no login is never sent one`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            var auth: String? = "unset"
            val t = Thread {
                server.accept().use { s ->
                    val input = s.getInputStream()
                    val head = StringBuilder()
                    while (!head.endsWith("\r\n\r\n")) head.append(input.read().toChar())
                    val len = Regex("Content-Length: (\\d+)").find(head)!!.groupValues[1].toInt()
                    input.readNBytes(len)
                    auth = Regex("Authorization: (.+)\r\n").find(head)?.groupValues?.get(1)
                    val body = "{\"id\":1,\"jsonrpc\":\"2.0\",\"result\":\"pong\"}"
                    s.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body").toByteArray())
                }
            }
            t.start()
            val reply = runBlocking { KodiClient().ping(KodiHost("x", "127.0.0.1", server.localPort, "kodi", "secret")) }
            t.join()
            assertTrue(reply is KodiReply.Ok)
            assertNull(auth)
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
