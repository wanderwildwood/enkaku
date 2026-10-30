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

    suspend fun call(host: KodiHost, method: String, params: JsonObject? = null): KodiReply =
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
                val headers = buildMap {
                    put("Content-Type", "application/json")
                    KodiRpc.authorization(host.username, host.password)?.let { put("Authorization", it) }
                }
                val body = KodiRpc.body(ids.getAndIncrement(), method, params).toByteArray(Charsets.UTF_8)
                val answer = KodiHttp.post(
                    address, host.port, host.address, "/jsonrpc", headers, body,
                    connectTimeoutMillis, readTimeoutMillis,
                )
                when (answer.status) {
                    200 -> KodiRpc.parse(answer.body)
                    401 -> KodiReply.Unauthorized
                    else -> KodiReply.Refused("HTTP ${answer.status}")
                }
            } catch (_: IOException) {
                KodiReply.Unreachable
            } catch (_: SecurityException) {
                KodiReply.Unreachable
            }
        }

    suspend fun run(host: KodiHost, command: KodiCommand): KodiReply = when (command) {
        is KodiCommand.Call -> call(host, command.method, command.params)
        KodiCommand.CycleRepeat -> cycleRepeat(host)
    }

    /** JSONRPC.Ping answers "pong": the address is Kodi, and the login, if any, is right. */
    suspend fun ping(host: KodiHost): KodiReply {
        val reply = call(host, "JSONRPC.Ping")
        if (reply is KodiReply.Ok) {
            val pong = try { reply.result.jsonPrimitive.contentOrNull } catch (_: Exception) { null }
            if (pong != "pong") return KodiReply.Refused("not Kodi")
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
