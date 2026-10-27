package com.atharok.btremote.ui.navigation

sealed class AppNavDestination(val route: String) {
    data object BluetoothPermissionsDestination: AppNavDestination(route = "bluetooth_permissions_route")
    data object SettingsDestination: AppNavDestination(route = "settings_route")
    data object BluetoothActivationDestination: AppNavDestination(route = "bluetooth_activation_route")
    data object DeviceSelectionDestination: AppNavDestination(route = "bluetooth_device_selection_route")
    data object DeviceDiscoveryDestination: AppNavDestination(route = "bluetooth_pairing_from_a_scanned_device_route")
    data object DistantDevicePairDestination: AppNavDestination(route = "bluetooth_pairing_from_a_remote_device_route")
    data object RemoteDestination: AppNavDestination(route = "bluetooth_remote_route")
    data object KodiDestination: AppNavDestination(route = "kodi_route")
    data object KodiRemoteDestination: AppNavDestination(route = "kodi_remote_route")
}

/**
 * Where the app opens: on Kodi when the phone has no Bluetooth or Kodi was the way last used,
 * otherwise where Bluetooth needs it to start.
 */
fun startRoute(bluetoothSupported: Boolean, startsOnKodi: Boolean, bluetoothPermitted: Boolean): String = when {
    !bluetoothSupported || startsOnKodi -> AppNavDestination.KodiDestination.route
    bluetoothPermitted -> AppNavDestination.BluetoothActivationDestination.route
    else -> AppNavDestination.BluetoothPermissionsDestination.route
}
