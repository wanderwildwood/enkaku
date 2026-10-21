package com.atharok.btremote.domain.entities

const val KODI_DEFAULT_PORT = 8080
const val KODI_DEFAULT_USERNAME = "kodi"

/**
 * A Kodi box reached over the home Wi-Fi, through its web server's JSON-RPC.
 *
 * [username] and [password] are Kodi's own web-server login, kept on the phone only and sent
 * only to [host]. Both empty means Kodi was set to ask for none.
 */
data class KodiHost(
    val name: String,
    val host: String,
    val port: Int = KODI_DEFAULT_PORT,
    val username: String = "",
    val password: String = ""
) {
    /** host:port, with an IPv6 address in brackets as a URL wants it. */
    val address: String
        get() = (if (host.contains(':')) "[$host]" else host) + ":" + port

    /** The same box, whatever it is called and whatever login it was given. */
    fun isSameBox(other: KodiHost): Boolean =
        host.equals(other.host, ignoreCase = true) && port == other.port
}
