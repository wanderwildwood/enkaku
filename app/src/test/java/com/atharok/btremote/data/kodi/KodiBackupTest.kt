package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class KodiBackupTest {
    private val den = KodiHost("Den", "192.168.1.50", 8080, "kodi", "pw-123")
    private val v6 = KodiHost("Attic", "fd00::25", 8081, "", "")

    private fun kind(text: String) = assertThrows(KodiBackup.BackupException::class.java) { KodiBackup.read(text) }.kind

    @Test fun `a saved box comes back, without its password`() {
        val text = KodiBackup.write(listOf(den, v6), startsOnKodi = true, written = Instant.parse("2026-11-03T12:00:00Z"))
        assertFalse(text.contains("pw-123"))
        val back = KodiBackup.read(text)
        assertEquals(listOf(den.copy(password = ""), v6), back.hosts)
        assertEquals(true, back.startsOnKodi)
    }

    @Test fun `nothing remembered is still a backup`() {
        assertEquals(KodiBackup.Contents(emptyList(), false), KodiBackup.read(KodiBackup.write(emptyList(), false)))
    }

    @Test fun `the wrong file is refused`() {
        assertEquals(KodiBackup.BackupException.Kind.NOT_A_BACKUP, kind("{not json"))
        assertEquals(KodiBackup.BackupException.Kind.NOT_A_BACKUP, kind("[1,2]"))
        assertEquals(KodiBackup.BackupException.Kind.NOT_A_BACKUP, kind("""{"format":"medicine","version":1}"""))
        assertEquals(KodiBackup.BackupException.Kind.NEWER, kind("""{"format":"enkaku-kodi-hosts","version":9,"hosts":[]}"""))
    }

    @Test fun `a damaged file changes nothing`() {
        val good = KodiBackup.write(listOf(den), false)
        assertEquals(KodiBackup.BackupException.Kind.DAMAGED, kind(good.replace("8080", "70000")))
        assertEquals(KodiBackup.BackupException.Kind.DAMAGED, kind(good.replace("\"192.168.1.50\"", "\"\"")))
        assertEquals(KodiBackup.BackupException.Kind.DAMAGED, kind("""{"format":"enkaku-kodi-hosts","version":1,"starts_on_kodi":false}"""))
    }

    @Test fun `the password kept here stays when the box and login are the same`() {
        val fromFile = den.copy(name = "Den TV", password = "")
        assertEquals(fromFile.copy(password = "pw-123"), KodiBackup.restored(fromFile, den))
        assertEquals(fromFile, KodiBackup.restored(fromFile, den.copy(username = "other")))
        assertEquals(fromFile, KodiBackup.restored(fromFile, v6))
        assertEquals(fromFile, KodiBackup.restored(fromFile, null))
    }
}
