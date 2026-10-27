package com.atharok.btremote.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartRouteTest {

    private val kodi = AppNavDestination.KodiDestination.route
    private val activation = AppNavDestination.BluetoothActivationDestination.route
    private val permissions = AppNavDestination.BluetoothPermissionsDestination.route

    @Test
    fun aPhoneWithNoBluetoothOpensOnKodi() {
        assertEquals(kodi, startRoute(bluetoothSupported = false, startsOnKodi = false, bluetoothPermitted = false))
        assertEquals(kodi, startRoute(bluetoothSupported = false, startsOnKodi = true, bluetoothPermitted = true))
    }

    @Test
    fun kodiLastUsedOpensOnKodiEvenWithoutBluetoothPermission() {
        assertEquals(kodi, startRoute(bluetoothSupported = true, startsOnKodi = true, bluetoothPermitted = false))
        assertEquals(kodi, startRoute(bluetoothSupported = true, startsOnKodi = true, bluetoothPermitted = true))
    }

    @Test
    fun bluetoothLastUsedOpensWhereBluetoothStarts() {
        assertEquals(activation, startRoute(bluetoothSupported = true, startsOnKodi = false, bluetoothPermitted = true))
        assertEquals(permissions, startRoute(bluetoothSupported = true, startsOnKodi = false, bluetoothPermitted = false))
    }
}
