package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.Instant

/**
 * The remembered Kodi box as one plain JSON file, written and read through the system's file
 * picker: its name, address, port and login name, and whether the remote opens on Kodi. Not the
 * password: that stays sealed on this phone, and is asked for again after a restore on another.
 * Field names are data, not interface.
 *
 * [read] checks the whole file and throws a [BackupException] for anything it cannot use; only
 * then does the caller change anything.
 */
object KodiBackup {

    const val FORMAT = "enkaku-kodi-hosts"
    const val VERSION = 1

    data class Contents(val hosts: List<KodiHost>, val startsOnKodi: Boolean)

    /** Why a file was refused, as a kind the screen turns into a plain sentence. */
    class BackupException(val kind: Kind) : Exception(kind.name) {
        enum class Kind { NOT_A_BACKUP, NEWER, DAMAGED }
    }

    private val pretty = Json { prettyPrint = true }

    fun write(hosts: List<KodiHost>, startsOnKodi: Boolean, written: Instant = Instant.now()): String =
        pretty.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("written", written.toString())
            put("starts_on_kodi", startsOnKodi)
            putJsonArray("hosts") {
                for (h in hosts) addJsonObject {
                    put("name", h.name)
                    put("host", h.host)
                    put("port", h.port)
                    put("username", h.username)
                }
            }
        })

    /** Reads a backup whole, or throws a [BackupException]; nothing is half-read. */
    fun read(text: String): Contents {
        val root = try {
            Json.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: throw BackupException(BackupException.Kind.NOT_A_BACKUP)
        if ((root["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) throw BackupException(BackupException.Kind.NOT_A_BACKUP)
        val version = (root["version"] as? JsonPrimitive)?.intOrNull ?: -1
        if (version > VERSION) throw BackupException(BackupException.Kind.NEWER)
        if (version < 1) throw BackupException(BackupException.Kind.DAMAGED)
        try {
            val startsOnKodi = root.getValue("starts_on_kodi").jsonPrimitive.boolean
            val hosts = (root.getValue("hosts") as JsonArray).map { e ->
                val o = e.jsonObject
                val host = o.getValue("host").jsonPrimitive.contentOrNull!!.trim()
                require(host.isNotEmpty())
                val port = o.getValue("port").jsonPrimitive.int
                require(port in 1..65535)
                KodiHost(
                    name = o.getValue("name").jsonPrimitive.contentOrNull!!.ifBlank { host },
                    host = host,
                    port = port,
                    username = o.getValue("username").jsonPrimitive.contentOrNull!!,
                    password = "",
                )
            }
            return Contents(hosts, startsOnKodi)
        } catch (e: Exception) {
            throw BackupException(BackupException.Kind.DAMAGED)
        }
    }

    /**
     * The box to remember after a restore: the file's, with the password already kept here when
     * it is the same box and the same login name, since the file carries none.
     */
    fun restored(fromFile: KodiHost, here: KodiHost?): KodiHost =
        if (here != null && here.isSameBox(fromFile) && here.username == fromFile.username) fromFile.copy(password = here.password)
        else fromFile
}
