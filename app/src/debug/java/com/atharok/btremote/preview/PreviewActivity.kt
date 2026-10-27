package com.atharok.btremote.preview

import android.bluetooth.BluetoothHidDevice
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.entities.settings.AppearanceSettings
import com.atharok.btremote.ui.screens.KodiScreen
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
 *
 * `--es screen kodi` opens Kodi over Wi-Fi with a pretend Kodi announced from the emulator
 * itself (_xbmc-jsonrpc-h._tcp, port 8080), so the search can be seen to find one.
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "remote"
        if (screen == "kodi") announcePretendKodi()
        setContent {
            BtRemoteTheme(appearance = AppearanceSettings()) {
                when (screen) {
                    "settings" -> SettingsScreen(navigateUp = {})
                    "about" -> { SettingsScreen(navigateUp = {}); AboutDialog(onDismiss = {}) }
                    "help" -> DevicesSelectionScreenHelpModalBottomSheet(onDismissRequest = {})
                    "kodi" -> KodiScreen(bluetoothMissing = false, navigateUp = {}, navigateToKodiRemote = {}, navigateToSettings = {})
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

    private var pretendKodi: NsdManager.RegistrationListener? = null

    private fun announcePretendKodi() {
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        val info = NsdServiceInfo().apply {
            serviceName = "Kodi (pretend)"
            serviceType = "_xbmc-jsonrpc-h._tcp"
            port = 8080
        }
        (getSystemService(NSD_SERVICE) as NsdManager)
            .registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        pretendKodi = listener
    }

    override fun onDestroy() {
        pretendKodi?.let { (getSystemService(NSD_SERVICE) as NsdManager).unregisterService(it) }
        super.onDestroy()
    }
}
