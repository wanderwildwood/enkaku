package com.atharok.btremote.ui.app

import android.bluetooth.BluetoothHidDevice
import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.atharok.btremote.common.extensions.getActivity
import com.atharok.btremote.common.utils.arePermissionsGranted
import com.atharok.btremote.common.utils.bluetoothConnectPermissions
import com.atharok.btremote.common.utils.bluetoothScanPermissions
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.entities.settings.AppearanceSettings
import com.atharok.btremote.presentation.services.BluetoothHidService
import com.atharok.btremote.presentation.viewmodel.AppScopeViewModel
import com.atharok.btremote.ui.components.CheckBluetoothStateChanged
import com.atharok.btremote.ui.components.OnLifecycleEvent
import com.atharok.btremote.ui.navigation.AppNavDestination
import com.atharok.btremote.ui.navigation.AppNavHost
import com.atharok.btremote.ui.navigation.navigateTo
import com.atharok.btremote.ui.navigation.startRoute
import com.atharok.btremote.ui.screens.BluetoothActivationScreen
import com.atharok.btremote.ui.screens.BluetoothPermissionsScreen
import com.atharok.btremote.ui.screens.DeviceDiscoveryScreen
import com.atharok.btremote.ui.screens.DevicesSelectionScreen
import com.atharok.btremote.ui.screens.DistantDevicePairScreen
import com.atharok.btremote.ui.screens.RemoteScreen
import com.atharok.btremote.ui.screens.SettingsScreen
import com.atharok.btremote.ui.theme.BtRemoteTheme
import com.atharok.btremote.data.kodi.KodiRemote
import com.atharok.btremote.data.kodi.KodiStore
import kotlinx.coroutines.runBlocking
import com.atharok.btremote.domain.entities.KodiHost
import com.atharok.btremote.ui.screens.KodiScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun BtRemoteApp(
    navController: NavHostController = rememberNavController(),
    appScopeViewModel: AppScopeViewModel = koinViewModel(),
    navigateToSettings: () -> Unit = {
        navController.navigateTo(AppNavDestination.SettingsDestination.route)
    },
    kodiRemote: KodiRemote = koinInject(),
    kodiStore: KodiStore = koinInject(),
    turnWithPhone: (Boolean) -> Unit = {},
    context: Context = LocalContext.current
) {
    val appearance by appScopeViewModel.appearanceSettingsFlow
        .collectAsStateWithLifecycle(AppearanceSettings())

    // Read again whenever the setting changes, so the switch takes effect at once.
    LaunchedEffect(appearance.turnWithPhone) { turnWithPhone(appearance.turnWithPhone) }

    // A steady cursor: a blinking one is a repaint twice a second on an e-ink panel for as long
    // as a field has focus. Dialogs inherit it.
    CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
    BtRemoteTheme(
        appearance = appearance
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            // A phone with no Bluetooth is not a dead end: it opens on Kodi over Wi-Fi.
            run {

                val isBluetoothServiceRunning: Boolean by appScopeViewModel.isBluetoothServiceRunning.collectAsStateWithLifecycle()
                val isBluetoothHidProfileRegistered: Boolean by appScopeViewModel.isBluetoothHidProfileRegistered.collectAsStateWithLifecycle()
                val bluetoothDeviceHidConnectionState: DeviceHidConnectionState by appScopeViewModel.deviceHidConnectionState.collectAsStateWithLifecycle()

                var isBluetoothEnabled: Boolean by remember { mutableStateOf(appScopeViewModel.isBluetoothEnabled) }

                // Kodi over Wi-Fi needs no Bluetooth, so Bluetooth going off or a device
                // going away leaves its two screens where they are.
                val kodiRoutes = remember {
                    setOf(AppNavDestination.KodiDestination.route, AppNavDestination.KodiRemoteDestination.route)
                }
                val isBluetoothSupported = appScopeViewModel.isBluetoothSupported
                // The way last used is the way it opens: a small read of the app's own settings.
                val startsOnKodi = remember { runBlocking { kodiStore.startsOnKodi() } }

                // Remember which of the two the person is using.
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        when (entry.destination.route) {
                            in kodiRoutes -> kodiStore.saveStartsOnKodi(true)
                            AppNavDestination.BluetoothActivationDestination.route,
                            AppNavDestination.DeviceSelectionDestination.route,
                            AppNavDestination.RemoteDestination.route -> kodiStore.saveStartsOnKodi(false)
                        }
                    }
                }

                val navigateToKodiScreen = {
                    navController.navigateTo(AppNavDestination.KodiDestination.route)
                }
                val navigateToKodiRemote = {
                    navController.navigateTo(AppNavDestination.KodiRemoteDestination.route)
                }

                CheckBluetoothStateChanged(
                    isBluetoothEnabled = isBluetoothEnabled,
                    onBluetoothEnabledChanged = { isBluetoothEnabled = it }
                )

                LaunchedEffect(isBluetoothEnabled) {
                    if (!isBluetoothEnabled && navController.currentDestination?.route !in kodiRoutes) {
                        BluetoothHidService.stop(context)
                        navController.popBackStack(
                            route = AppNavDestination.BluetoothActivationDestination.route,
                            inclusive = false
                        )
                    }
                }

                LaunchedEffect(bluetoothDeviceHidConnectionState) {
                    if(bluetoothDeviceHidConnectionState.state == BluetoothHidDevice.STATE_DISCONNECTED &&
                        navController.currentDestination?.route != AppNavDestination.DeviceSelectionDestination.route &&
                        navController.currentDestination?.route != AppNavDestination.DeviceDiscoveryDestination.route &&
                        navController.currentDestination?.route !in kodiRoutes
                        ) {
                        navController.popBackStack(
                            route = AppNavDestination.DeviceSelectionDestination.route,
                            inclusive = false
                        )
                    }
                }

                // ---- NavHost ----

                AppNavHost(
                    navController = navController,

                    startDestination = remember {
                        startRoute(
                            bluetoothSupported = isBluetoothSupported,
                            startsOnKodi = startsOnKodi,
                            bluetoothPermitted = arePermissionsGranted(context, bluetoothConnectPermissions)
                        )
                    },

                    bluetoothPermissionsScreen = {
                        BluetoothPermissionsScreen(
                            permissions = bluetoothConnectPermissions,
                            arePermissionsGranted = arePermissionsGranted(
                                context = context,
                                permissions = bluetoothConnectPermissions
                            ),
                            onPermissionsGranted = {
                                navController.navigate(AppNavDestination.BluetoothActivationDestination.route) {
                                    popUpTo(0) {
                                        this.saveState = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            navigateToSettings = navigateToSettings,
                            navigateToKodiScreen = navigateToKodiScreen,
                            modifier = Modifier
                        )
                    },

                    settingsScreen = {
                        SettingsScreen(
                            navigateUp = { navController.navigateUp() },
                            modifier = Modifier
                        )
                    },

                    bluetoothActivationScreen = {
                        BluetoothActivationScreen(
                            isBluetoothEnabled = isBluetoothEnabled,
                            navigateToBluetoothDeviceSelectionScreen = {
                                navController.navigateTo(AppNavDestination.DeviceSelectionDestination.route)
                            },
                            navigateToSettings = navigateToSettings,
                            navigateToKodiScreen = navigateToKodiScreen
                        )
                    },

                    deviceSelectionScreen = {
                        DevicesSelectionScreen(
                            isBluetoothEnabled = isBluetoothEnabled,
                            isBluetoothServiceRunning = isBluetoothServiceRunning,
                            isBluetoothHidProfileRegistered = isBluetoothHidProfileRegistered,
                            bluetoothDeviceHidConnectionState = bluetoothDeviceHidConnectionState,
                            closeApp = { context.getActivity()?.moveTaskToBack(true) },
                            navigateToRemoteScreen = {
                                navController.navigateTo(AppNavDestination.RemoteDestination.route)
                            },
                            navigateToDeviceDiscoveryScreen = {
                                navController.navigateTo(AppNavDestination.DeviceDiscoveryDestination.route)
                            },
                            navigateToDistantDevicePairScreen = {
                                navController.navigateTo(AppNavDestination.DistantDevicePairDestination.route)
                            },
                            navigateToSettings = navigateToSettings,
                            navigateToKodiScreen = navigateToKodiScreen,
                            navigateToKodiRemote = navigateToKodiRemote,
                            modifier = Modifier
                        )
                    },

                    deviceDiscoveryScreen = {
                        DeviceDiscoveryScreen(
                            permissions = bluetoothScanPermissions,
                            arePermissionsGranted = arePermissionsGranted(
                                context = context,
                                permissions = bluetoothScanPermissions
                            ),
                            isBluetoothServiceRunning = isBluetoothServiceRunning,
                            bluetoothDeviceHidConnectionState = bluetoothDeviceHidConnectionState,
                            navigateUp = { navController.navigateUp() },
                            navigateToRemoteScreen = {
                                navController.navigateTo(AppNavDestination.RemoteDestination.route)
                            },
                            navigateToSettings = navigateToSettings,
                            modifier = Modifier
                        )
                    },

                    distantDevicePairScreen = {
                        DistantDevicePairScreen(
                            localDeviceName = appScopeViewModel.localDeviceName,
                            navigateUp = {
                                navController.navigateUp()
                            },
                            modifier = Modifier
                        )
                    },

                    remoteScreen = {
                        // Over Bluetooth, nothing goes to a Kodi left open before.
                        LaunchedEffect(Unit) { kodiRemote.close() }
                        RemoteScreen(
                            bluetoothDeviceHidConnectionState = bluetoothDeviceHidConnectionState,
                            closeApp = { context.getActivity()?.moveTaskToBack(true) },
                            navigateToSettings = navigateToSettings,
                            modifier = Modifier
                        )
                    },

                    kodiScreen = {
                        KodiScreen(
                            bluetoothMissing = !isBluetoothSupported,
                            navigateUp = {
                                when {
                                    navController.previousBackStackEntry != null -> navController.navigateUp()
                                    !isBluetoothSupported -> context.getActivity()?.moveTaskToBack(true)
                                    // Opened on Kodi, so there is no list of devices behind it yet.
                                    else -> navController.navigate(
                                        if(arePermissionsGranted(context, bluetoothConnectPermissions))
                                            AppNavDestination.BluetoothActivationDestination.route
                                        else
                                            AppNavDestination.BluetoothPermissionsDestination.route
                                    ) { popUpTo(0) }
                                }
                            },
                            navigateToKodiRemote = navigateToKodiRemote,
                            navigateToSettings = navigateToSettings,
                            modifier = Modifier
                        )
                    },

                    kodiRemoteScreen = {
                        val kodi: KodiHost? by kodiRemote.session.collectAsStateWithLifecycle()
                        // The Bluetooth service has nothing to do while Kodi is driven over
                        // Wi-Fi; the list of devices starts it again on the way back.
                        LaunchedEffect(Unit) {
                            if (isBluetoothServiceRunning) BluetoothHidService.stop(context)
                        }
                        // Android may have ended the app while the remote was in the background;
                        // the Kodi it was driving went with it, so go back to choose it again.
                        LaunchedEffect(kodi == null) {
                            if (kodi == null) navController.navigateUp()
                        }
                        kodi?.let { host ->
                            RemoteScreen(
                                bluetoothDeviceHidConnectionState = bluetoothDeviceHidConnectionState,
                                closeApp = { context.getActivity()?.moveTaskToBack(true) },
                                navigateToSettings = navigateToSettings,
                                kodi = host,
                                leaveKodi = {
                                    kodiRemote.close()
                                },
                                modifier = Modifier
                            )
                        }
                    },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.displayCutout.exclude(WindowInsets.systemBars))
                )

                // ---- Lifecycle ----

                OnLifecycleEvent { _, event ->
                    // If the app goes into the background
                    if(event == Lifecycle.Event.ON_STOP) {
                        // If we are in the device selection view
                        if(navController.currentDestination?.route == AppNavDestination.DeviceSelectionDestination.route) {
                            if(isBluetoothServiceRunning) {
                                BluetoothHidService.stop(context)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
