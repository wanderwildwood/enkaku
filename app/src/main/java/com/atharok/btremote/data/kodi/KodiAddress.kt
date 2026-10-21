package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost

/**
 * An address as people write it: a bare host, host:port, or Kodi's own web address pasted
 * whole (http://host:port/jsonrpc). Null when it is not one.
 */
fun parseKodiAddress(address: String, port: String): KodiHost? {
    var text = address.trim()
    text = text.removePrefix("http://").removePrefix("HTTP://")
    text = text.substringBefore('/')
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null

    var host = text
    var portNumber: Int? = port.trim().toIntOrNull()
    if (text.startsWith("[")) {
        // [IPv6]:port
        val end = text.indexOf(']')
        if (end < 0) return null
        host = text.substring(1, end)
        text.substring(end + 1).removePrefix(":").takeIf { it.isNotEmpty() }?.let {
            portNumber = it.toIntOrNull() ?: return null
        }
    } else if (text.count { it == ':' } == 1) {
        host = text.substringBefore(':')
        portNumber = text.substringAfter(':').toIntOrNull() ?: return null
    }
    val finalPort = portNumber ?: return null
    if (host.isEmpty() || finalPort !in 1..65535) return null
    return KodiHost(name = host, host = host, port = finalPort)
}
