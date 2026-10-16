package com.atharok.btremote.preview

import android.bluetooth.BluetoothHidDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.entities.settings.AppearanceSettings
import com.atharok.btremote.ui.screens.RemoteScreen
import com.atharok.btremote.ui.screens.SettingsScreen
import com.atharok.btremote.ui.theme.BtRemoteTheme
import com.atharok.btremote.ui.views.AboutDialog
import com.atharok.btremote.ui.views.DevicesSelectionScreenHelpModalBottomSheet

/**
 * Debug builds only: the screens an emulator cannot reach, since it has no Bluetooth and the
 * app stops at "Bluetooth is not supported". For screenshots, never for the phone.
 *
 *   adb shell am start -n com.wanderwildwood.enkaku/com.atharok.btremote.preview.PreviewActivity --es screen remote
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "remote"
        setContent {
            BtRemoteTheme(appearance = AppearanceSettings()) {
                when (screen) {
                    "settings" -> SettingsScreen(navigateUp = {})
                    "about" -> { SettingsScreen(navigateUp = {}); AboutDialog(onDismiss = {}) }
                    "help" -> DevicesSelectionScreenHelpModalBottomSheet(onDismissRequest = {})
                    else -> RemoteScreen(
                        bluetoothDeviceHidConnectionState = DeviceHidConnectionState(
                            state = BluetoothHidDevice.STATE_CONNECTED,
                            deviceName = "Living room TV"
                        ),
                        closeApp = {},
                        navigateToSettings = {}
                    )
                }
            }
        }
    }
}
