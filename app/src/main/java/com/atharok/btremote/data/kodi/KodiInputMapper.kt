package com.atharok.btremote.data.kodi

import com.atharok.btremote.common.utils.KEYBOARD_REPORT_ID
import com.atharok.btremote.common.utils.REMOTE_REPORT_ID

/**
 * The remote screen speaks Bluetooth HID: every button is a report, a press and then a
 * release. Over Wi-Fi the same screen drives Kodi, so its reports are read back here into
 * Kodi's JSON-RPC. A press becomes one call; the release, and anything Kodi has no use for
 * (brightness, the mouse), becomes nothing.
 *
 * Kodi's names are those its own keymaps give the same keys, so a button does over Wi-Fi what
 * it does over Bluetooth.
 */
object KodiInputMapper {

    /**
     * Over Bluetooth a held button is held down, and the other end repeats it. Kodi over HTTP
     * gets one call a press, so these few are repeated here while the button is held: the
     * arrows, volume, channel and page steps, where holding means "keep going".
     */
    fun repeatsWhileHeld(command: KodiCommand): Boolean = command in repeating

    private val repeating: Set<KodiCommand> = setOf(
        KodiCommand.input("Up"), KodiCommand.input("Down"),
        KodiCommand.input("Left"), KodiCommand.input("Right"),
        KodiCommand.action("volumeup"), KodiCommand.action("volumedown"),
        KodiCommand.action("channelup"), KodiCommand.action("channeldown"),
        KodiCommand.action("pageup"), KodiCommand.action("pagedown")
    )

    fun forReport(reportId: Int, bytes: ByteArray): KodiCommand? = when (reportId) {
        REMOTE_REPORT_ID -> forConsumerUsage(consumerUsage(bytes))
        KEYBOARD_REPORT_ID -> forKey(bytes.getOrNull(1)?.toInt()?.and(0xFF) ?: 0)
        else -> null
    }

    /** A consumer report is one 16-bit usage, low byte first. */
    private fun consumerUsage(bytes: ByteArray): Int {
        val low = bytes.getOrNull(0)?.toInt()?.and(0xFF) ?: 0
        val high = bytes.getOrNull(1)?.toInt()?.and(0xFF) ?: 0
        return low or (high shl 8)
    }

    // https://source.android.com/docs/core/interaction/input/keyboard-devices#hid-consumer-page-0x0c
    private fun forConsumerUsage(usage: Int): KodiCommand? = when (usage) {
        0x040 -> KodiCommand.input("ContextMenu")      // Menu
        0x041 -> KodiCommand.input("Select")           // Menu pick
        0x042 -> KodiCommand.input("Up")
        0x043 -> KodiCommand.input("Down")
        0x044 -> KodiCommand.input("Left")
        0x045 -> KodiCommand.input("Right")
        0x223 -> KodiCommand.input("Home")
        0x224 -> KodiCommand.input("Back")

        0x0CD -> KodiCommand.action("playpause")
        0x0B0 -> KodiCommand.action("play")
        0x0B1 -> KodiCommand.action("pause")
        0x0B7 -> KodiCommand.action("stop")
        0x0B5 -> KodiCommand.action("skipnext")
        0x0B6 -> KodiCommand.action("skipprevious")
        0x0B3 -> KodiCommand.action("fastforward")
        0x0B4 -> KodiCommand.action("rewind")
        0x0BC -> KodiCommand.CycleRepeat
        0x061 -> KodiCommand.action("showsubtitles")   // Closed captions

        0x0E9 -> KodiCommand.action("volumeup")
        0x0EA -> KodiCommand.action("volumedown")
        0x0E2 -> KodiCommand.action("mute")

        0x09C -> KodiCommand.action("channelup")
        0x09D -> KodiCommand.action("channeldown")

        0x069 -> KodiCommand.action("red")
        0x06A -> KodiCommand.action("green")
        0x06B -> KodiCommand.action("blue")
        0x06C -> KodiCommand.action("yellow")

        0x030 -> KodiCommand.powerMenu

        else -> null                                    // Release (0), brightness
    }

    // https://source.android.com/docs/core/interaction/input/keyboard-devices#hid-keyboard-and-keypad-page-0x07
    private fun forKey(key: Int): KodiCommand? = when (key) {
        in 0x1E..0x26 -> KodiCommand.action("number${key - 0x1E + 1}")   // 1 to 9
        0x27 -> KodiCommand.action("number0")
        0x28 -> KodiCommand.input("Select")            // Enter
        0x29 -> KodiCommand.input("Back")              // Escape
        0x2A -> KodiCommand.action("backspace")
        0x2B -> KodiCommand.action("fullscreen")       // Tab, as in Kodi's keyboard keymap
        0x2C -> KodiCommand.action("playpause")        // Space, likewise
        0x46 -> KodiCommand.action("screenshot")       // Print screen
        0x4B -> KodiCommand.action("pageup")
        0x4E -> KodiCommand.action("pagedown")
        0x4F -> KodiCommand.input("Right")
        0x50 -> KodiCommand.input("Left")
        0x51 -> KodiCommand.input("Down")
        0x52 -> KodiCommand.input("Up")
        else -> null                                    // Release (0), letters: text goes as text
    }
}
