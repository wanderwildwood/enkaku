package com.atharok.btremote.ui.views.remote

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True while the remote screen drives Kodi over Wi-Fi. The few buttons Kodi has no use for
 * (brightness) are left out then, rather than drawn and doing nothing.
 */
val LocalKodiRemote = staticCompositionLocalOf { false }
