package com.atharok.btremote.ui.screens

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.domain.entities.RemoteNavigationEntity
import com.atharok.btremote.domain.entities.remoteInput.VolumePhysicalButtonAction
import com.atharok.btremote.domain.entities.remoteInput.keyboard.KeyboardLanguage
import com.atharok.btremote.domain.entities.settings.AppearanceSettings
import com.atharok.btremote.domain.entities.settings.RemoteSettings
import com.atharok.btremote.presentation.viewmodel.SettingsViewModel
import com.atharok.btremote.ui.components.AppScaffold
import com.atharok.btremote.ui.components.BasicIconButton
import com.atharok.btremote.ui.components.ListDialog
import com.atharok.btremote.ui.views.AboutDialog
import com.atharok.btremote.ui.components.PagedColumn
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import org.koin.androidx.compose.koinViewModel

/** The speeds offered for the pointer and the wheel, where BT Remote had a slider. */
private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f, 4f, 5f)

/**
 * The remote, the mouse, the keyboard, then the rest.
 *
 * BT Remote's light, dark and black themes and its wallpaper colours are gone, since the panel
 * honours none of them. Its sliders are rows that open a short list, because dragging a slider
 * repaints the panel at every step. About, the source and the libraries moved behind the `i`.
 */
@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    context: Context = LocalContext.current
) {
    val appearanceSettings by settingsViewModel
        .appearanceSettingsFlow.collectAsStateWithLifecycle(AppearanceSettings())

    val remoteSettings by settingsViewModel
        .remoteSettingsFlow.collectAsStateWithLifecycle(RemoteSettings())

    val showRemoteButtonsInNotification: Boolean by settingsViewModel.showRemoteButtonsInNotificationFlow.collectAsStateWithLifecycle(initialValue = false)
    val hideBluetoothActivationButton: Boolean by settingsViewModel.hideBluetoothActivationButtonFlow.collectAsStateWithLifecycle(initialValue = false)

    var aboutOpen by remember { mutableStateOf(false) }

    AppScaffold(
        title = stringResource(id = R.string.settings),
        modifier = modifier,
        navigateUp = {
            BasicIconButton(
                onClick = navigateUp,
                icon = AppIcons.Close,
                contentDescription = stringResource(id = R.string.close)
            )
        },
        topBarActions = {
            BasicIconButton(
                onClick = { aboutOpen = true },
                icon = AppIcons.Info,
                contentDescription = stringResource(id = R.string.about)
            )
        }
    ) { innerPadding ->
        PagedColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // ---- Remote ----

            item { Heading(R.string.remote, first = true) }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.use_minimalist_interface),
                    note = stringResource(id = R.string.minimalist_interface_info),
                    checked = remoteSettings.useMinimalistRemote,
                    onCheckedChange = { settingsViewModel.saveUseMinimalistRemote(it) }
                )
            }

            item {
                Setting(
                    title = stringResource(id = R.string.navigation_mode),
                    value = stringResource(
                        id = if (remoteSettings.remoteNavigationEntity == RemoteNavigationEntity.D_PAD) R.string.d_pad else R.string.touchpad
                    ),
                    onClick = {
                        settingsViewModel.saveRemoteNavigation(
                            if (remoteSettings.remoteNavigationEntity == RemoteNavigationEntity.D_PAD)
                                RemoteNavigationEntity.TOUCHPAD
                            else
                                RemoteNavigationEntity.D_PAD
                        )
                    }
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.use_mouse_navigation_by_default),
                    checked = remoteSettings.useMouseNavigationByDefault,
                    onCheckedChange = { settingsViewModel.saveUseMouseNavigationByDefault(it) }
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.use_enter_for_selection_title),
                    note = stringResource(id = R.string.use_enter_for_selection_summary),
                    checked = remoteSettings.useEnterForSelection,
                    onCheckedChange = { settingsViewModel.saveUseEnterForSelection(it) }
                )
            }

            item {
                SettingsList(
                    title = R.string.assign_action_volume_up_button,
                    value = remoteSettings.volumePhysicalButtonActionUp,
                    onValueChange = { settingsViewModel.saveVolumePhysicalButtonActionUp(it) },
                    items = VolumePhysicalButtonAction.entries,
                    convertValueToString = { context.getString(it.stringRes) }
                )
            }

            item {
                SettingsList(
                    title = R.string.assign_action_volume_down_button,
                    value = remoteSettings.volumePhysicalButtonActionDown,
                    onValueChange = { settingsViewModel.saveVolumePhysicalButtonActionDown(it) },
                    items = VolumePhysicalButtonAction.entries,
                    convertValueToString = { context.getString(it.stringRes) }
                )
            }

            // ---- Mouse ----

            item { Heading(R.string.mouse) }

            item {
                SettingsList(
                    title = R.string.mouse_pointer_speed,
                    value = remoteSettings.mouseSpeed,
                    onValueChange = { settingsViewModel.saveMouseSpeed(it) },
                    items = SPEEDS,
                    convertValueToString = ::speed
                )
            }

            item {
                SettingsList(
                    title = R.string.mouse_scroll_speed,
                    value = remoteSettings.mouseScrollSpeed,
                    onValueChange = { settingsViewModel.saveMouseScrollSpeed(it) },
                    items = SPEEDS,
                    convertValueToString = ::speed
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.invert_mouse_scrolling_direction),
                    checked = remoteSettings.shouldInvertMouseScrollingDirection,
                    onCheckedChange = { settingsViewModel.saveInvertMouseScrollingDirection(it) }
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.use_the_gyroscope_to_control_the_mouse),
                    checked = remoteSettings.useGyroscope,
                    onCheckedChange = { settingsViewModel.saveUseGyroscope(it) }
                )
            }

            // ---- Keyboard ----

            item { Heading(R.string.keyboard) }

            item {
                SettingsList(
                    title = R.string.keyboard_language,
                    dialogMessage = stringResource(id = R.string.keyboard_language_info),
                    value = remoteSettings.keyboardLanguage,
                    onValueChange = { settingsViewModel.changeKeyboardLanguage(it) },
                    items = KeyboardLanguage.entries.sortedBy { context.getString(it.language) },
                    convertValueToString = { context.getString(it.language) }
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.advanced_keyboard),
                    checked = remoteSettings.useAdvancedKeyboard,
                    onCheckedChange = { settingsViewModel.saveUseAdvancedKeyboard(it) }
                )
            }

            item {
                if (remoteSettings.useAdvancedKeyboard) {
                    SettingsSwitch(
                        title = stringResource(id = R.string.integrate_advanced_keyboard_into_the_view),
                        checked = remoteSettings.useAdvancedKeyboardIntegrated,
                        onCheckedChange = { settingsViewModel.saveUseAdvancedKeyboardIntegrated(it) }
                    )
                } else {
                    SettingsSwitch(
                        title = stringResource(id = R.string.clear_input_field),
                        checked = remoteSettings.mustClearInputField,
                        onCheckedChange = { settingsViewModel.saveMustClearInputField(it) }
                    )
                }
            }

            // ---- The rest ----

            item { Heading(R.string.advanced_options) }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.full_screen),
                    checked = appearanceSettings.useFullScreen,
                    onCheckedChange = { settingsViewModel.saveUseFullScreen(it) }
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.show_remote_buttons_in_notification),
                    checked = showRemoteButtonsInNotification,
                    onCheckedChange = { settingsViewModel.saveShowRemoteButtonsInNotification(it) }
                )
            }

            item {
                SettingsSwitch(
                    title = stringResource(id = R.string.hide_bluetooth_activation_button),
                    checked = hideBluetoothActivationButton,
                    onCheckedChange = { settingsViewModel.saveHideBluetoothActivationButton(it) }
                )
            }
        }
    }

    if (aboutOpen) AboutDialog(onDismiss = { aboutOpen = false })
}

private fun speed(value: Float): String =
    "×" + (if (value % 1f == 0f) value.toInt().toString() else value.toString())

// --- Rows ----

@Composable
private fun Heading(@StringRes text: Int, first: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!first) Spacer(Modifier.height(24.dp))
        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = stringResource(id = text),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Setting(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
        TextMMD(text = value, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun <T> SettingsList(
    @StringRes title: Int,
    value: T,
    onValueChange: (T) -> Unit,
    items: List<T>,
    convertValueToString: (T) -> String,
    dialogMessage: String? = null
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) {
        ListDialog(
            confirmButtonText = null,
            dismissButtonText = stringResource(android.R.string.cancel),
            onConfirmation = { index ->
                onValueChange(items[index])
                isShowingDialog = false
            },
            onDismissRequest = { isShowingDialog = false },
            dialogTitle = stringResource(title),
            dialogMessage = dialogMessage,
            items = items.map { convertValueToString(it) },
            defaultItemIndex = items.indexOf(value)
        )
    }

    Setting(
        title = stringResource(id = title),
        value = convertValueToString(value),
        onClick = { isShowingDialog = true }
    )
}

/** `onCheckedChange = null` on the switch is MMD's way of letting the whole row take the press. */
@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    note: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
            note?.let {
                TextMMD(text = it, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.width(12.dp))
        SwitchMMD(checked = checked, onCheckedChange = null)
    }
}
