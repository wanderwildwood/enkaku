package com.atharok.btremote.data.kodi

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.atharok.btremote.common.extensions.dataStore
import com.atharok.btremote.domain.entities.KODI_DEFAULT_PORT
import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The last Kodi the remote reached, so it is one press away next time. Its login is kept with
 * it, in the app's own storage on the phone (Android does not back it up) and nowhere else.
 */
class KodiStore(private val context: Context) {

    private val lastKodiKey = stringPreferencesKey("last_kodi_key")
    private val startsOnKodiKey = booleanPreferencesKey("starts_on_kodi_key")

    val lastKodiFlow: Flow<KodiHost?> by lazy {
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences -> preferences[lastKodiKey]?.let { decodeStored(it, Secrets::open) } }
    }

    suspend fun saveLastKodi(host: KodiHost) {
        // Only the sealed form is ever written; with no keystore the Kodi is not remembered.
        val sealed = Secrets.seal(encode(host))
        context.dataStore.edit { if (sealed != null) it[lastKodiKey] = sealed else it.remove(lastKodiKey) }
    }

    /** A login saved in plain text by an earlier version is sealed once, on the spot. */
    suspend fun sealStored() {
        try {
            val stored = context.dataStore.data.first()[lastKodiKey] ?: return
            if (Secrets.isSealed(stored)) return
            val sealed = Secrets.seal(stored)
            context.dataStore.edit { if (sealed != null) it[lastKodiKey] = sealed else it.remove(lastKodiKey) }
        } catch (_: Exception) {
        }
    }

    /** Whether the remote was last used over Wi-Fi, so it opens there next time. */
    suspend fun startsOnKodi(): Boolean = try {
        context.dataStore.data.first()[startsOnKodiKey] == true
    } catch (_: Exception) {
        false
    }

    suspend fun saveStartsOnKodi(value: Boolean) {
        context.dataStore.edit { if (it[startsOnKodiKey] != value) it[startsOnKodiKey] = value }
    }

    suspend fun forgetLastKodi() {
        context.dataStore.edit { it.remove(lastKodiKey) }
    }

    private fun encode(host: KodiHost): String = encodeKodiHost(host)
}

internal fun encodeKodiHost(host: KodiHost): String = buildJsonObject {
    put("name", host.name)
    put("host", host.host)
    put("port", host.port)
    put("username", host.username)
    put("password", host.password)
}.toString()

/** Reads what was stored: [open] unseals the sealed form and passes plain JSON through. */
internal fun decodeStored(stored: String, open: (String) -> String?): KodiHost? =
    open(stored)?.let(::decodeKodiHost)

internal fun decodeKodiHost(json: String): KodiHost? = try {
    val o = Json.parseToJsonElement(json).jsonObject
    val host = o["host"]?.jsonPrimitive?.contentOrNull
    if (host.isNullOrBlank()) null else KodiHost(
        name = o["name"]?.jsonPrimitive?.contentOrNull ?: host,
        host = host,
        port = o["port"]?.jsonPrimitive?.intOrNull ?: KODI_DEFAULT_PORT,
        username = o["username"]?.jsonPrimitive?.contentOrNull ?: "",
        password = o["password"]?.jsonPrimitive?.contentOrNull ?: ""
    )
} catch (_: Exception) {
    null
}
