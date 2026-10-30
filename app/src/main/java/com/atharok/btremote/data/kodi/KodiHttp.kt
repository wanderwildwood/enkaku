package com.atharok.btremote.data.kodi

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** What a plain HTTP answer said: its status and its body. */
class HttpAnswer(val status: Int, val body: String)

/**
 * One plain HTTP POST written on a socket, to an address already known to be local. The
 * platform's HTTP stack refuses cleartext unless the app switches it on for every host, which
 * would let any address be reached in the clear; a socket needs no such switch, so the manifest
 * keeps refusing plain http everywhere, and this reaches only what [KodiNetwork] let through.
 * No redirect is followed: there is only the one answer.
 */
object KodiHttp {
    private const val MAX_BODY = 4 * 1024 * 1024

    fun post(
        address: InetAddress,
        port: Int,
        hostHeader: String,
        path: String,
        headers: Map<String, String>,
        body: ByteArray,
        connectTimeoutMillis: Int,
        readTimeoutMillis: Int,
    ): HttpAnswer {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(address, port), connectTimeoutMillis)
            socket.soTimeout = readTimeoutMillis
            val head = buildString {
                append("POST ").append(path).append(" HTTP/1.1\r\n")
                append("Host: ").append(hostHeader).append("\r\n")
                headers.forEach { (k, v) -> append(k).append(": ").append(v).append("\r\n") }
                append("Content-Length: ").append(body.size).append("\r\n")
                append("Connection: close\r\n\r\n")
            }
            val out = socket.getOutputStream()
            out.write(head.toByteArray(Charsets.ISO_8859_1))
            out.write(body)
            out.flush()
            return readAnswer(socket.getInputStream().buffered())
        }
    }

    /** Reads a status line, headers and a body (chunked, by length, or to the end). */
    fun readAnswer(input: InputStream): HttpAnswer {
        val head = readHead(input) ?: throw IOException("no answer")
        val lines = head.split("\r\n")
        val status = lines.first().split(' ').getOrNull(1)?.toIntOrNull() ?: throw IOException("not HTTP")
        val headers = lines.drop(1).mapNotNull { l ->
            val i = l.indexOf(':')
            if (i <= 0) null else l.substring(0, i).trim().lowercase() to l.substring(i + 1).trim()
        }.toMap()
        val bytes = when {
            headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true -> readChunked(input)
            headers["content-length"]?.toIntOrNull() != null -> {
                val n = headers.getValue("content-length").toInt()
                if (n < 0 || n > MAX_BODY) throw IOException("too big")
                readExactly(input, n)
            }
            else -> readToEnd(input)
        }
        return HttpAnswer(status, String(bytes, Charsets.UTF_8))
    }

    private fun readHead(input: InputStream): String? {
        val sb = StringBuilder()
        while (sb.length < 16 * 1024) {
            val b = input.read()
            if (b < 0) return null
            sb.append(b.toChar())
            if (sb.endsWith("\r\n\r\n")) return sb.substring(0, sb.length - 4)
        }
        return null
    }

    private fun readExactly(input: InputStream, n: Int): ByteArray {
        val out = ByteArray(n)
        var got = 0
        while (got < n) {
            val r = input.read(out, got, n - got)
            if (r < 0) throw IOException("cut short")
            got += r
        }
        return out
    }

    private fun readToEnd(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val r = input.read(buf)
            if (r < 0) break
            out.write(buf, 0, r)
            if (out.size() > MAX_BODY) throw IOException("too big")
        }
        return out.toByteArray()
    }

    private fun readChunked(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val line = readLine(input).substringBefore(';').trim()
            val size = line.toIntOrNull(16) ?: throw IOException("bad chunk")
            if (size == 0) break
            if (out.size() + size > MAX_BODY) throw IOException("too big")
            out.write(readExactly(input, size))
            readLine(input) // the CRLF after the chunk
        }
        return out.toByteArray()
    }

    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) throw IOException("cut short")
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return sb.toString()
    }
}
