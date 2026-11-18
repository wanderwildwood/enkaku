package com.atharok.btremote.data.kodi

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Where Remote may send a Kodi login: only to a box on the phone's own network.
 *
 * Kodi's web server speaks plain HTTP, so its username and password cross the network in the
 * clear. That is acceptable on a home network and not anywhere else, so an address is used only
 * when it is private: 10/8, 172.16/12, 192.168/16, link-local (169.254/16, fe80::/10), the
 * IPv6 unique-local range (fc00::/7) and the phone itself (loopback). The shared range 100.64/10
 * that Tailscale and similar overlays use counts only while a VPN is up: without one, on mobile
 * data, that range is the carrier's own shared network. A name is looked up first, and the
 * address it gave is the one connected to, so a name cannot be pointed at the internet after the
 * check.
 */
object KodiNetwork {

    /** [vpnUp]: whether the phone's network is a VPN right now, which alone lets 100.64/10 in. */
    fun isLocal(address: InetAddress, vpnUp: Boolean): Boolean {
        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) return true
        val b = address.address
        return when (address) {
            is Inet4Address -> vpnUp && (b[0].toInt() and 0xFF) == 100 && (b[1].toInt() and 0xC0) == 0x40
            is Inet6Address -> (b[0].toInt() and 0xFE) == 0xFC
            else -> false
        }
    }

    /**
     * Whether the network this app's traffic goes out on is a VPN (Tailscale and the like). False
     * when it cannot be told, so 100.64/10 is then refused.
     */
    fun vpnUp(connectivity: ConnectivityManager?): Boolean = try {
        val network = connectivity?.activeNetwork
        network != null &&
            connectivity?.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    } catch (_: Exception) {
        false
    }

    /**
     * The phone's own address on the way to [address], or null when it cannot be told. Nothing
     * is sent: a UDP socket only picks its route when connected.
     */
    fun ownAddressTowards(address: InetAddress, port: Int): String? = try {
        DatagramSocket().use { socket ->
            socket.connect(InetSocketAddress(address, port))
            socket.localAddress?.takeUnless { it.isAnyLocalAddress }?.hostAddress
        }
    } catch (_: Exception) {
        null
    }

    /**
     * The address to connect to for [host], or null when it is not a local one (any address a
     * name gives that is not local makes the whole name refused). Throws UnknownHostException
     * when the name does not resolve.
     */
    fun resolveLocal(host: String, vpnUp: Boolean): InetAddress? {
        val all = InetAddress.getAllByName(host)
        if (all.isEmpty() || !all.all { isLocal(it, vpnUp) }) return null
        return all.firstOrNull { it is Inet4Address } ?: all.first()
    }
}
