package com.atharok.btremote.data.kodi

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Where Remote may send a Kodi login: only to a box on the phone's own network.
 *
 * Kodi's web server speaks plain HTTP, so its username and password cross the network in the
 * clear. That is acceptable on a home network and not anywhere else, so an address is used only
 * when it is private: 10/8, 172.16/12, 192.168/16, link-local (169.254/16, fe80::/10), the
 * IPv6 unique-local range (fc00::/7), the shared range 100.64/10 that Tailscale and similar
 * overlays use, and the phone itself (loopback). A name is looked up first, and the address it
 * gave is the one connected to, so a name cannot be pointed at the internet after the check.
 */
object KodiNetwork {

    fun isLocal(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) return true
        val b = address.address
        return when (address) {
            is Inet4Address -> (b[0].toInt() and 0xFF) == 100 && (b[1].toInt() and 0xC0) == 0x40
            is Inet6Address -> (b[0].toInt() and 0xFE) == 0xFC
            else -> false
        }
    }

    /**
     * The address to connect to for [host], or null when it is not a local one (any address a
     * name gives that is not local makes the whole name refused). Throws UnknownHostException
     * when the name does not resolve.
     */
    fun resolveLocal(host: String): InetAddress? {
        val all = InetAddress.getAllByName(host)
        if (all.isEmpty() || !all.all(::isLocal)) return null
        return all.firstOrNull { it is Inet4Address } ?: all.first()
    }
}
