package com.atharok.btremote.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharok.btremote.data.kodi.KodiClient
import com.atharok.btremote.data.kodi.KodiDiscovery
import com.atharok.btremote.data.kodi.KodiRemote
import com.atharok.btremote.data.kodi.KodiReply
import com.atharok.btremote.data.kodi.KodiStore
import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface KodiConnectState {
    data object Idle : KodiConnectState
    data class Connecting(val host: KodiHost) : KodiConnectState
    /** Kodi wants a login, or turned down the one it was given. */
    data class NeedsLogin(val host: KodiHost) : KodiConnectState
    data class Failed(val host: KodiHost, val reply: KodiReply) : KodiConnectState
    data class Connected(val host: KodiHost) : KodiConnectState
}

class KodiViewModel(
    private val discovery: KodiDiscovery,
    private val client: KodiClient,
    private val remote: KodiRemote,
    private val store: KodiStore
) : ViewModel() {

    val found: StateFlow<List<KodiHost>> = discovery.found
    val isSearching: StateFlow<Boolean> = discovery.isSearching

    val lastKodi: StateFlow<KodiHost?> = store.lastKodiFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow<KodiConnectState>(KodiConnectState.Idle)
    val state: StateFlow<KodiConnectState> = _state.asStateFlow()

    private var connectJob: Job? = null

    init {
        viewModelScope.launch { store.sealStored() }
    }

    fun startDiscovery() = discovery.start()
    fun stopDiscovery() = discovery.stop()

    /** A found box, with the saved login if it is the box last used. */
    fun withSavedLogin(host: KodiHost): KodiHost {
        val last = lastKodi.value
        return if (last != null && last.isSameBox(host)) {
            host.copy(username = last.username, password = last.password)
        } else {
            host
        }
    }

    fun connect(host: KodiHost) {
        connectJob?.cancel()
        _state.value = KodiConnectState.Connecting(host)
        connectJob = viewModelScope.launch {
            when (val reply = client.ping(host)) {
                is KodiReply.Ok -> {
                    val named = if (host.name.isNotBlank() && host.name != host.host) host
                        else host.copy(name = client.friendlyName(host) ?: host.host)
                    store.saveLastKodi(named)
                    remote.open(named)
                    _state.value = KodiConnectState.Connected(named)
                }
                KodiReply.Unauthorized -> _state.value = KodiConnectState.NeedsLogin(host)
                else -> _state.value = KodiConnectState.Failed(host, reply)
            }
        }
    }

    fun dismiss() {
        connectJob?.cancel()
        connectJob = null
        _state.value = KodiConnectState.Idle
    }

    fun forgetLastKodi() {
        viewModelScope.launch { store.forgetLastKodi() }
    }
}
