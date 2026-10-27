package com.atharok.btremote.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.domain.entities.KodiHost
import com.atharok.btremote.presentation.viewmodel.KodiConnectState
import com.atharok.btremote.presentation.viewmodel.KodiViewModel
import com.atharok.btremote.ui.components.AppScaffold
import com.atharok.btremote.ui.components.BasicIconButton
import com.atharok.btremote.ui.components.NavigateUpIconButton
import com.atharok.btremote.ui.components.PagedColumn
import com.atharok.btremote.ui.components.SettingsIconButton
import com.atharok.btremote.ui.components.TextMedium
import com.atharok.btremote.ui.components.TextNormalSecondary
import com.atharok.btremote.ui.views.KodiAddressDialog
import com.atharok.btremote.ui.views.KodiConnectOverlay
import com.atharok.btremote.ui.views.KodiHostItemView
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * Kodi over Wi-Fi: the box last used, the boxes that announce themselves on this network, and
 * an address by hand. Choosing one opens the same remote as Bluetooth does, sending to Kodi.
 */
@Composable
fun KodiScreen(
    bluetoothMissing: Boolean,
    navigateUp: () -> Unit,
    navigateToKodiRemote: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KodiViewModel = koinViewModel()
) {
    val lastKodi by viewModel.lastKodi.collectAsStateWithLifecycle()
    val found by viewModel.found.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAddressDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose { viewModel.stopDiscovery() }
    }

    // The same way back as the arrow in the bar, which knows what is behind this screen.
    BackHandler(onBack = navigateUp)

    KodiConnectedEffect(state = state, navigateToKodiRemote = navigateToKodiRemote, dismiss = viewModel::dismiss)

    AppScaffold(
        title = stringResource(R.string.kodi_over_wifi),
        modifier = modifier,
        navigateUp = { NavigateUpIconButton(navigateUp) },
        topBarActions = {
            BasicIconButton(
                onClick = viewModel::startDiscovery,
                icon = AppIcons.Refresh,
                contentDescription = stringResource(R.string.refresh)
            )
            SettingsIconButton(navigateToSettings)
        }
    ) { innerPadding ->
        PagedColumn(modifier = Modifier.padding(innerPadding)) {
            if (bluetoothMissing) {
                item {
                    TextNormalSecondary(
                        text = stringResource(R.string.kodi_only_no_bluetooth),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.padding_max),
                                vertical = dimensionResource(R.dimen.padding_normal)
                            )
                    )
                }
            }
            lastKodi?.let { last ->
                heading(R.string.kodi_last_used)
                item {
                    KodiHostItemView(
                        host = last,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.connect(last) }
                            .padding(dimensionResource(R.dimen.padding_max))
                    )
                }
                item {
                    ForgetRow(name = last.name, forget = viewModel::forgetLastKodi)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(R.dimen.padding_max),
                            vertical = dimensionResource(R.dimen.padding_normal)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.large_icon_size))
                ) {
                    TextNormalSecondary(text = stringResource(R.string.kodi_on_this_network))
                    // Said in a word rather than a spinner, which on this panel never settles.
                    if (isSearching) TextNormalSecondary(text = stringResource(R.string.searching))
                }
            }
            if (found.isEmpty()) {
                item {
                    TextNormalSecondary(
                        text = stringResource(R.string.kodi_none_found),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.padding_max),
                                vertical = dimensionResource(R.dimen.padding_normal)
                            )
                    )
                }
            }
            items(found) { host ->
                KodiHostItemView(
                    host = host,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.connect(viewModel.withSavedLogin(host)) }
                        .padding(dimensionResource(R.dimen.padding_max))
                )
            }

            item {
                TextMedium(
                    text = stringResource(R.string.kodi_enter_address),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddressDialog = true }
                        .padding(dimensionResource(R.dimen.padding_max))
                )
            }
        }

        KodiConnectOverlay(state = state, connect = viewModel::connect, dismiss = viewModel::dismiss)

        if (showAddressDialog && state == KodiConnectState.Idle) {
            KodiAddressDialog(
                initial = lastKodi,
                askingForLogin = false,
                connect = {
                    showAddressDialog = false
                    viewModel.connect(it)
                },
                close = { showAddressDialog = false }
            )
        }
    }
}

/** Once Kodi has answered, the remote opens on it. */
@Composable
fun KodiConnectedEffect(
    state: KodiConnectState,
    navigateToKodiRemote: () -> Unit,
    dismiss: () -> Unit
) {
    LaunchedEffect(state) {
        if (state is KodiConnectState.Connected) {
            dismiss()
            navigateToKodiRemote()
        }
    }
}

private fun LazyListScope.heading(@StringRes text: Int) {
    item {
        TextNormalSecondary(
            text = stringResource(text),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_max),
                    vertical = dimensionResource(R.dimen.padding_normal)
                )
        )
    }
}

/** How long a row that asks for a second tap stays armed. */
private const val ARMED_MS = 6_000L

/** It asks in its own face: the first tap arms it, a second within six seconds forgets. */
@Composable
private fun ForgetRow(name: String, forget: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(ARMED_MS)
            armed = false
        }
    }
    TextMedium(
        text = stringResource(if (armed) R.string.kodi_forget_armed else R.string.kodi_forget, name),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (armed) {
                    armed = false
                    forget()
                } else {
                    armed = true
                }
            }
            .padding(dimensionResource(R.dimen.padding_max))
    )
}
