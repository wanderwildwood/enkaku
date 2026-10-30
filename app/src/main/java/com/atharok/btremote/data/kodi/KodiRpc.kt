package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json
import java.util.Base64

/** What Kodi made of a call. */
sealed interface KodiReply {
    data class Ok(val result: JsonElement) : KodiReply
    /** Kodi's web server wants a username and password, or a different one. */
    data object Unauthorized : KodiReply
    /** Nothing answered at that address in time. */
    data object Unreachable : KodiReply
    /** The address is not on this network, so nothing was sent: the login would cross the internet in the clear. */
    data object NotLocal : KodiReply
    /** Something answered and turned the call down. */
    data class Refused(val message: String) : KodiReply
}

/** JSON-RPC 2.0 as Kodi takes it: POST http://host:port/jsonrpc, one call to a request. */
object KodiRpc {

    fun endpoint(host: KodiHost): String = "http://${host.address}/jsonrpc"

    fun body(id: Int, method: String, params: JsonObject? = null): String =
        buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            if (params != null) put("params", params)
            put("id", id)
        }.toString()

    /** HTTP Basic, as Kodi's web server asks for; nothing at all when no login was given. */
    fun authorization(username: String, password: String): String? {
        if (username.isEmpty() && password.isEmpty()) return null
        val pair = "$username:$password".toByteArray(Charsets.UTF_8)
        return "Basic " + Base64.getEncoder().encodeToString(pair)
    }

    /** Reads the body of a 200 answer. */
    fun parse(body: String): KodiReply {
        val json: JsonObject = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (_: Exception) {
            return KodiReply.Refused("not JSON-RPC")
        }
        json["error"]?.let { error ->
            if (error !is JsonNull) {
                val message = try {
                    error.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                } catch (_: Exception) { null }
                return KodiReply.Refused(message ?: error.toString())
            }
        }
        return KodiReply.Ok(json["result"] ?: JsonNull)
    }
}
