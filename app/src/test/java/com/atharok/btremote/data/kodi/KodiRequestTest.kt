package com.atharok.btremote.data.kodi

import com.atharok.btremote.common.utils.KEYBOARD_REPORT_ID
import com.atharok.btremote.common.utils.MOUSE_REPORT_ID
import com.atharok.btremote.common.utils.REMOTE_REPORT_ID
import com.atharok.btremote.domain.entities.KodiHost
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KodiRequestTest {

    private fun remote(low: Int, high: Int = 0) =
        KodiInputMapper.forReport(REMOTE_REPORT_ID, byteArrayOf(low.toByte(), high.toByte()))

    private fun key(code: Int) =
        KodiInputMapper.forReport(KEYBOARD_REPORT_ID, byteArrayOf(0x00, code.toByte()))

    private fun actionOf(command: KodiCommand?): String? =
        (command as? KodiCommand.Call)?.takeIf { it.method == "Input.ExecuteAction" }
            ?.params?.get("action")?.jsonPrimitive?.content

    @Test
    fun navigation_buttons_are_kodi_input_methods() {
        assertEquals(KodiCommand.Call("Input.Up"), remote(0x42))
        assertEquals(KodiCommand.Call("Input.Down"), remote(0x43))
        assertEquals(KodiCommand.Call("Input.Left"), remote(0x44))
        assertEquals(KodiCommand.Call("Input.Right"), remote(0x45))
        assertEquals(KodiCommand.Call("Input.Select"), remote(0x41))
        assertEquals(KodiCommand.Call("Input.ContextMenu"), remote(0x40))
        assertEquals(KodiCommand.Call("Input.Home"), remote(0x23, 0x02))
        assertEquals(KodiCommand.Call("Input.Back"), remote(0x24, 0x02))
    }

    @Test
    fun media_and_volume_buttons_are_actions() {
        assertEquals("playpause", actionOf(remote(0xCD)))
        assertEquals("stop", actionOf(remote(0xB7)))
        assertEquals("skipnext", actionOf(remote(0xB5)))
        assertEquals("skipprevious", actionOf(remote(0xB6)))
        assertEquals("fastforward", actionOf(remote(0xB3)))
        assertEquals("rewind", actionOf(remote(0xB4)))
        assertEquals("volumeup", actionOf(remote(0xE9)))
        assertEquals("volumedown", actionOf(remote(0xEA)))
        assertEquals("mute", actionOf(remote(0xE2)))
        assertEquals("showsubtitles", actionOf(remote(0x61)))
        assertEquals(KodiCommand.CycleRepeat, remote(0xBC))
    }

    @Test
    fun releases_brightness_and_the_mouse_send_nothing() {
        assertNull(remote(0x00))
        assertNull(key(0x00))
        assertNull(remote(0x6F))
        assertNull(remote(0x70))
        assertNull(KodiInputMapper.forReport(MOUSE_REPORT_ID, byteArrayOf(1, 0, 0, 0)))
    }

    @Test
    fun channel_digits_and_keys() {
        assertEquals("number1", actionOf(key(0x1E)))
        assertEquals("number9", actionOf(key(0x26)))
        assertEquals("number0", actionOf(key(0x27)))
        assertEquals(KodiCommand.Call("Input.Select"), key(0x28))
        assertEquals("backspace", actionOf(key(0x2A)))
        assertEquals(KodiCommand.Call("Input.Up"), key(0x52))
    }

    @Test
    fun held_arrows_and_volume_repeat_and_nothing_else_does() {
        assertTrue(KodiInputMapper.repeatsWhileHeld(remote(0x42)!!))
        assertTrue(KodiInputMapper.repeatsWhileHeld(remote(0xE9)!!))
        assertTrue(!KodiInputMapper.repeatsWhileHeld(remote(0x41)!!))
        assertTrue(!KodiInputMapper.repeatsWhileHeld(remote(0xCD)!!))
        assertTrue(!KodiInputMapper.repeatsWhileHeld(KodiCommand.powerMenu))
    }

    @Test
    fun request_body_is_json_rpc_2() {
        val body = Json.parseToJsonElement(
            KodiRpc.body(7, "Input.ExecuteAction", (KodiCommand.action("volumeup") as KodiCommand.Call).params)
        ).jsonObject
        assertEquals("2.0", body["jsonrpc"]!!.jsonPrimitive.content)
        assertEquals("Input.ExecuteAction", body["method"]!!.jsonPrimitive.content)
        assertEquals(7, body["id"]!!.jsonPrimitive.int)
        assertEquals("volumeup", body["params"]!!.jsonObject["action"]!!.jsonPrimitive.content)

        val bare = Json.parseToJsonElement(KodiRpc.body(1, "Input.Up")).jsonObject
        assertTrue("params" !in bare)
    }

    @Test
    fun text_goes_as_send_text() {
        val command = KodiCommand.text("blue planet", done = true) as KodiCommand.Call
        assertEquals("Input.SendText", command.method)
        assertEquals(JsonPrimitive("blue planet"), command.params!!["text"])
        assertEquals(JsonPrimitive(true), command.params["done"])
    }

    @Test
    fun endpoint_and_login() {
        assertEquals("http://192.168.1.20:8080/jsonrpc", KodiRpc.endpoint(KodiHost("Kodi", "192.168.1.20")))
        assertEquals("http://[fe80::1]:8080/jsonrpc", KodiRpc.endpoint(KodiHost("Kodi", "fe80::1")))
        assertNull(KodiRpc.authorization("", ""))
        // kodi:secret
        assertEquals("Basic a29kaTpzZWNyZXQ=", KodiRpc.authorization("kodi", "secret"))
    }

    @Test
    fun replies() {
        val ok = KodiRpc.parse("""{"id":1,"jsonrpc":"2.0","result":"pong"}""")
        assertEquals(KodiReply.Ok(JsonPrimitive("pong")), ok)
        val error = KodiRpc.parse("""{"error":{"code":-32601,"message":"Method not found."},"id":1,"jsonrpc":"2.0"}""")
        assertEquals(KodiReply.Refused("Method not found."), error)
        assertTrue(KodiRpc.parse("<html>") is KodiReply.Refused)
    }

    @Test
    fun addresses_as_people_write_them() {
        assertEquals(KodiHost("10.0.0.5", "10.0.0.5", 8080), parseKodiAddress("10.0.0.5", "8080"))
        assertEquals(KodiHost("10.0.0.5", "10.0.0.5", 9090), parseKodiAddress("10.0.0.5:9090", "8080"))
        assertEquals(KodiHost("libreelec.local", "libreelec.local", 8080),
            parseKodiAddress("http://libreelec.local:8080/jsonrpc", ""))
        assertEquals(KodiHost("fe80::1", "fe80::1", 8080), parseKodiAddress("[fe80::1]:8080", ""))
        assertNull(parseKodiAddress("", "8080"))
        assertNull(parseKodiAddress("10.0.0.5", "70000"))
        assertNull(parseKodiAddress("my box", "8080"))
    }
}
