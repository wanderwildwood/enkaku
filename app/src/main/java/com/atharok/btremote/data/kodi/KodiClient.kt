package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Plain HTTP to the one Kodi chosen, on the home network only (see [KodiNetwork]). Kodi has no TLS
 * to offer. Redirects are not followed, so nothing is ever sent to any address but that one.
 */
class KodiClient(
    private val connectTimeoutMillis: Int = 3_000,
    private val readTimeoutMillis: Int = 5_000
) {
    private val ids = AtomicInteger(1)

    /**
     * The boxes, by address and port, that have asked for the login with a 401. A request goes
     * out without the login until the box at that address asks for it, so a saved password is
     * never sent to a machine that does not want one; after that it goes with every request.
     * Each is also keyed on the phone's own address on the way there, and forgotten when a
     * connection is made afresh ([ping]), so on another network the box at that same address
     * has to ask again before it is sent the login.
     */
    private val askedForLogin: MutableSet<String> = ConcurrentHashMap.newKeySet()

    suspend fun call(host: KodiHost, method: String, params: JsonObject? = null): KodiReply =
        request(host, method, params, fresh = false)

    private suspend fun request(host: KodiHost, method: String, params: JsonObject?, fresh: Boolean): KodiReply =
        withContext(Dispatchers.IO) {
            // The login goes out in the clear (Kodi has no TLS), so only to a box on this network:
            // checked before anything is sent.
            val address = try {
                KodiNetwork.resolveLocal(host.host)
            } catch (_: IOException) {
                return@withContext KodiReply.Unreachable
            } catch (_: SecurityException) {
                return@withContext KodiReply.Unreachable
            } ?: return@withContext KodiReply.NotLocal
            try {
                val login = KodiRpc.authorization(host.username, host.password)
                // No way to tell which network this is: nothing is remembered, the box asks each time.
                val box = KodiNetwork.ownAddressTowards(address, host.port)
                    ?.let { "$it>${address.hostAddress}:${host.port}" }
                if (fresh && box != null) askedForLogin -= box
                val body = KodiRpc.body(ids.getAndIncrement(), method, params).toByteArray(Charsets.UTF_8)
                fun post(withLogin: Boolean) = KodiHttp.post(
                    address, host.port, host.address, "/jsonrpc",
                    buildMap {
                        put("Content-Type", "application/json")
                        if (withLogin && login != null) put("Authorization", login)
                    },
                    body, connectTimeoutMillis, readTimeoutMillis,
                )
                val sentLogin = login != null && box != null && box in askedForLogin
                var answer = post(sentLogin)
                if (answer.status == 401 && login != null && !sentLogin) {
                    // This box wants the login: sent again with it, to this same address only.
                    if (box != null) askedForLogin += box
                    answer = post(true)
                }
                when (answer.status) {
                    200 -> KodiRpc.parse(answer.body)
                    401 -> KodiReply.Unauthorized
                    else -> KodiReply.Refused(reason = KodiReply.Refused.Reason.HTTP_STATUS, httpStatus = answer.status)
                }
            } catch (_: IOException) {
                KodiReply.Unreachable
            } catch (_: SecurityException) {
                KodiReply.Unreachable
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: RuntimeException) {
                // Anything else a broken answer could raise is a refusal, not a crash.
                KodiReply.Refused(reason = KodiReply.Refused.Reason.NOT_JSON_RPC)
            }
        }

    suspend fun run(host: KodiHost, command: KodiCommand): KodiReply = when (command) {
        is KodiCommand.Call -> call(host, command.method, command.params)
        KodiCommand.CycleRepeat -> cycleRepeat(host)
    }

    /**
     * JSONRPC.Ping answers "pong": the address is Kodi, and the login, if any, is right. It starts
     * a connection, so it goes without the login until the box asks for it again.
     */
    suspend fun ping(host: KodiHost): KodiReply {
        val reply = request(host, "JSONRPC.Ping", null, fresh = true)
        if (reply is KodiReply.Ok) {
            val pong = try { reply.result.jsonPrimitive.contentOrNull } catch (_: Exception) { null }
            if (pong != "pong") return KodiReply.Refused(reason = KodiReply.Refused.Reason.NOT_KODI)
        }
        return reply
    }

    /** The name Kodi gives itself (Settings, Services, General, Device name), if it says. */
    suspend fun friendlyName(host: KodiHost): String? {
        val reply = call(host, "XBMC.GetInfoLabels", buildJsonObject {
            put("labels", buildJsonArray { add(FRIENDLY_NAME) })
        })
        if (reply !is KodiReply.Ok) return null
        return try {
            reply.result.jsonObject[FRIENDLY_NAME]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    private suspend fun cycleRepeat(host: KodiHost): KodiReply {
        val players = call(host, "Player.GetActivePlayers")
        if (players !is KodiReply.Ok) return players
        val playerId = try {
            players.result.jsonArray.firstOrNull()?.jsonObject?.get("playerid")?.jsonPrimitive?.intOrNull
        } catch (_: Exception) { null }
            ?: return players // Nothing is playing, so there is nothing to repeat.
        return call(host, "Player.SetRepeat", buildJsonObject {
            put("playerid", playerId)
            put("repeat", "cycle")
        })
    }

    private companion object {
        const val FRIENDLY_NAME = "System.FriendlyName"
    }
}
