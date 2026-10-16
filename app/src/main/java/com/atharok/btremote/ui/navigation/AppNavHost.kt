package com.atharok.btremote.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    bluetoothPermissionsScreen: @Composable () -> Unit,
    settingsScreen: @Composable () -> Unit,
    bluetoothActivationScreen: @Composable () -> Unit,
    deviceSelectionScreen: @Composable () -> Unit,
    deviceDiscoveryScreen: @Composable () -> Unit,
    distantDevicePairScreen: @Composable () -> Unit,
    remoteScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // No slide between screens: on this panel a slide is a smear across two repaints.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        composable(
            route = AppNavDestination.BluetoothPermissionsDestination.route
        ) {
            bluetoothPermissionsScreen()
        }

        composable(
            route = AppNavDestination.SettingsDestination.route
        ) {
            settingsScreen()
        }

        composable(
            route = AppNavDestination.BluetoothActivationDestination.route
        ) {
            bluetoothActivationScreen()
        }

        composable(
            route = AppNavDestination.DeviceSelectionDestination.route
        ) {
            deviceSelectionScreen()
        }

        composable(
            route = AppNavDestination.DeviceDiscoveryDestination.route
        ) {
            deviceDiscoveryScreen()
        }

        composable(
            route = AppNavDestination.DistantDevicePairDestination.route
        ) {
            distantDevicePairScreen()
        }

        composable(
            route = AppNavDestination.RemoteDestination.route
        ) {
            remoteScreen()
        }
    }
}

fun NavHostController.navigateTo(
    route: String,
    launchSingleTop: Boolean = true
) {
    this.navigate(route) {
        this.launchSingleTop = launchSingleTop
    }
}