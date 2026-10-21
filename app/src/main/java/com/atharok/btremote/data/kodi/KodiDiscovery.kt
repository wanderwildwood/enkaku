package com.atharok.btremote.data.kodi

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.InetAddress

/**
 * The Kodi boxes on the home network, as they announce themselves over mDNS (Kodi does when
 * Settings, Services, General, "Announce services to other systems" is on).
 *
 * Kodi announces its JSON-RPC over HTTP as _xbmc-jsonrpc-h._tcp, at its web server's port.
 * Its web interface also appears as _http._tcp under a name with "Kodi" in it; that is
 * looked for too, for a box that announces only the one. Android's own mDNS does the
 * listening, so the app holds no multicast lock of its own.
 *
 * Android before 14 resolves one service at a time, so they are resolved in turn.
 */
class KodiDiscovery(private val nsdManager: NsdManager) {

    private val _found = MutableStateFlow<List<KodiHost>>(emptyList())
    val found: StateFlow<List<KodiHost>> = _found.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val lock = Any()
    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val waiting = ArrayDeque<NsdServiceInfo>()
    private var isResolving = false
    private var generation = 0

    fun start() {
        synchronized(lock) {
            stopLocked()
            generation++
            _found.value = emptyList()
            waiting.clear()
            isResolving = false
            _isSearching.value = true
            for (type in SERVICE_TYPES) {
                val listener = Listener(type, generation)
                try {
                    nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                    listeners += listener
                } catch (_: Exception) {
                    // A type that cannot be searched leaves the other one.
                }
            }
            if (listeners.isEmpty()) _isSearching.value = false
        }
    }

    fun stop() {
        synchronized(lock) {
            stopLocked()
            generation++
            waiting.clear()
            isResolving = false
            _isSearching.value = false
        }
    }

    private fun stopLocked() {
        for (listener in listeners) {
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (_: Exception) {
                // Already stopped.
            }
        }
        listeners.clear()
    }

    private fun isKodi(type: String, info: NsdServiceInfo): Boolean =
        type == JSON_RPC_TYPE || info.serviceName.contains("kodi", ignoreCase = true)

    private fun enqueue(info: NsdServiceInfo, forGeneration: Int) {
        synchronized(lock) {
            if (forGeneration != generation) return
            waiting.addLast(info)
            resolveNextLocked()
        }
    }

    private fun resolveNextLocked() {
        if (isResolving) return
        val next = waiting.removeFirstOrNull() ?: return
        isResolving = true
        val forGeneration = generation
        try {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    resolved(null, forGeneration)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    resolved(serviceInfo, forGeneration)
                }
            })
        } catch (_: Exception) {
            isResolving = false
            resolveNextLocked()
        }
    }

    private fun resolved(info: NsdServiceInfo?, forGeneration: Int) {
        synchronized(lock) {
            if (forGeneration != generation) return
            isResolving = false
            val address = info?.let(::addressOf)
            if (info != null && address != null && info.port > 0) {
                val host = KodiHost(name = info.serviceName, host = address, port = info.port)
                if (_found.value.none { it.isSameBox(host) }) {
                    _found.value = _found.value + host
                }
            }
            resolveNextLocked()
        }
    }

    private fun lost(name: String) {
        // A box that goes quiet stays listed until the next search: mDNS loses and finds a
        // service again often enough that a list which followed it would flicker.
        synchronized(lock) { waiting.removeAll { it.serviceName == name } }
    }

    /** Its IPv4 address where it has one, which is what a home router hands out and shows. */
    private fun addressOf(info: NsdServiceInfo): String? {
        val addresses: List<InetAddress> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            info.hostAddresses
        } else {
            @Suppress("DEPRECATION")
            listOfNotNull(info.host)
        }
        val chosen = addresses.firstOrNull { it is Inet4Address } ?: addresses.firstOrNull()
        return chosen?.hostAddress?.substringBefore('%')
    }

    private inner class Listener(
        private val type: String,
        private val forGeneration: Int
    ) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {}

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (isKodi(type, serviceInfo)) enqueue(serviceInfo, forGeneration)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            lost(serviceInfo.serviceName)
        }

        override fun onDiscoveryStopped(serviceType: String) {}

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            synchronized(lock) {
                listeners.remove(this)
                if (listeners.isEmpty() && forGeneration == generation) _isSearching.value = false
            }
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private companion object {
        const val JSON_RPC_TYPE = "_xbmc-jsonrpc-h._tcp"
        const val HTTP_TYPE = "_http._tcp"
        val SERVICE_TYPES = listOf(JSON_RPC_TYPE, HTTP_TYPE)
    }
}
