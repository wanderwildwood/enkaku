package com.atharok.btremote.data.kodi

import com.atharok.btremote.domain.entities.KodiHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class KodiStoreTest {
    private val host = KodiHost("Living room", "192.168.1.205", 8080, "kodi", "pw-123")
    private val seal = { s: String -> "enc:v1:" + s.reversed() }
    private val open = { s: String -> if (s.startsWith("enc:v1:")) s.removePrefix("enc:v1:").reversed() else s }

    @Test fun `a sealed login is read back`() {
        val stored = seal(encodeKodiHost(host))
        assertFalse(stored.contains("pw-123"))
        assertEquals(host, decodeStored(stored, open))
    }

    @Test fun `a login saved in plain text by an older version is still read`() {
        assertEquals(host, decodeStored(encodeKodiHost(host), open))
    }

    @Test fun `a sealed login whose key is gone reads as nothing remembered`() {
        assertNull(decodeStored("enc:v1:zz", { null }))
        assertNull(decodeStored("not json", open))
    }
}
