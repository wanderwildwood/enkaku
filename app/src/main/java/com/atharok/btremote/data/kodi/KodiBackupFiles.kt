package com.atharok.btremote.data.kodi

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Saving and restoring the remembered Kodi against a file the person picked: the Android half of [KodiBackup]. */
object KodiBackupFiles {

    sealed interface Result {
        /** [name] is the Kodi now remembered, or null when the file held none. */
        data class Done(val name: String?) : Result
        data class Refused(val kind: KodiBackup.BackupException.Kind) : Result
        data object Unreadable : Result
    }

    /** Writes the backup to [uri]; false, with the file removed, when that fails. */
    suspend fun save(context: Context, store: KodiStore, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val text = KodiBackup.write(listOfNotNull(store.lastKodi()), store.startsOnKodi())
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("no stream")
        }.isSuccess
        if (!ok) runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
        ok
    }

    /**
     * Reads the whole file first; only a file that passes changes anything. The Kodi it names
     * replaces the one remembered, keeping that one's password when it is the same box and login;
     * a file with none forgets the remembered one. A file over 1 MB is refused unread.
     */
    suspend fun restore(context: Context, store: KodiStore, uri: Uri): Result = withContext(Dispatchers.IO) {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readNBytesCapped() }
        }.getOrNull() ?: return@withContext Result.Unreadable
        if (bytes.size > MAX_BYTES) return@withContext Result.Refused(KodiBackup.BackupException.Kind.NOT_A_BACKUP)
        val contents = try {
            KodiBackup.read(String(bytes, Charsets.UTF_8))
        } catch (e: KodiBackup.BackupException) {
            return@withContext Result.Refused(e.kind)
        }
        val host = contents.hosts.firstOrNull()?.let { KodiBackup.restored(it, store.lastKodi()) }
        runCatching {
            if (host != null) store.saveLastKodi(host) else store.forgetLastKodi()
            store.saveStartsOnKodi(contents.startsOnKodi)
        }.onFailure { return@withContext Result.Unreadable }
        Result.Done(host?.name)
    }

    /** At most MAX_BYTES + 1 bytes, so a file that is too big can be told from one that just fits. */
    private fun java.io.InputStream.readNBytesCapped(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() <= MAX_BYTES) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private const val MAX_BYTES = 1024 * 1024
}
