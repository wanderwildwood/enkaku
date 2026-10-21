package com.atharok.btremote.data.kodi

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** One thing to ask of Kodi, built from one press on the remote. */
sealed interface KodiCommand {

    /** A single JSON-RPC call. */
    data class Call(val method: String, val params: JsonObject? = null) : KodiCommand

    /**
     * Repeat: Player.SetRepeat needs the playing player's id, so this is two calls, the first
     * to ask which player is playing.
     */
    data object CycleRepeat : KodiCommand

    companion object {
        /** Input.Up, Input.Select, Input.Back and the rest: Kodi's own navigation. */
        fun input(name: String): KodiCommand = Call("Input.$name")

        /** Input.ExecuteAction, with one of the action names of Kodi's keymaps. */
        fun action(action: String): KodiCommand =
            Call("Input.ExecuteAction", buildJsonObject { put("action", action) })

        /**
         * Input.SendText puts [text] into the text field Kodi has open, in place of what was
         * there. [done] closes Kodi's keyboard as if Enter had been pressed.
         */
        fun text(text: String, done: Boolean): KodiCommand =
            Call("Input.SendText", buildJsonObject {
                put("text", text)
                put("done", done)
            })

        /** Kodi's power menu (shut down, reboot, suspend), where a remote's power button leads. */
        val powerMenu: KodiCommand =
            Call("GUI.ActivateWindow", buildJsonObject { put("window", "shutdownmenu") })
    }
}
