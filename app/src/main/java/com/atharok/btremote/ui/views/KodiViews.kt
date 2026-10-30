package com.atharok.btremote.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.atharok.btremote.R
import com.atharok.btremote.data.kodi.KodiReply
import com.atharok.btremote.data.kodi.parseKodiAddress
import com.atharok.btremote.domain.entities.KODI_DEFAULT_PORT
import com.atharok.btremote.domain.entities.KODI_DEFAULT_USERNAME
import com.atharok.btremote.domain.entities.KodiHost
import com.atharok.btremote.presentation.viewmodel.KodiConnectState
import com.atharok.btremote.ui.components.DialogTitle
import com.atharok.btremote.ui.components.LoadingDialog
import com.atharok.btremote.ui.components.SimpleDialog
import com.atharok.btremote.ui.components.TemplateDialog
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

/**
 * What stands over the list while a Kodi is being reached: the words while it is asked, and
 * what went wrong if it did not answer. A Kodi that wants a login gets the address dialog,
 * filled in, with its password to give.
 */
@Composable
fun KodiConnectOverlay(
    state: KodiConnectState,
    connect: (KodiHost) -> Unit,
    dismiss: () -> Unit
) {
    when (state) {
        is KodiConnectState.Connecting -> LoadingDialog(
            title = stringResource(R.string.kodi_over_wifi),
            message = stringResource(R.string.kodi_connecting, state.host.address),
            buttonText = stringResource(android.R.string.cancel),
            onButtonClick = dismiss
        )
        is KodiConnectState.NeedsLogin -> KodiAddressDialog(
            initial = state.host,
            askingForLogin = true,
            connect = connect,
            close = dismiss
        )
        is KodiConnectState.Failed -> SimpleDialog(
            confirmButtonText = stringResource(R.string.retry),
            dismissButtonText = stringResource(R.string.close),
            onConfirmation = { connect(state.host) },
            onDismissRequest = dismiss,
            dialogTitle = stringResource(R.string.kodi_over_wifi),
            dialogText = when (val reply = state.reply) {
                is KodiReply.Refused -> stringResource(R.string.kodi_refused, state.host.address, reply.message)
                KodiReply.NotLocal -> stringResource(R.string.kodi_not_local, state.host.address)
                else -> stringResource(R.string.kodi_unreachable, state.host.address)
            }
        )
        else -> Unit
    }
}

/** A Kodi in a list: its name, and where it is. */
@Composable
fun KodiHostItemView(host: KodiHost, modifier: Modifier = Modifier) {
    DeviceItemView(name = host.name, macAddress = host.address, modifier = modifier)
}

/**
 * The address by hand, for a Kodi that does not announce itself, and the login for one that
 * asks. Kodi's web server listens on 8080 and calls its user kodi unless told otherwise.
 */
@Composable
fun KodiAddressDialog(
    initial: KodiHost?,
    askingForLogin: Boolean,
    connect: (KodiHost) -> Unit,
    close: () -> Unit
) {
    var address by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf((initial?.port ?: KODI_DEFAULT_PORT).toString()) }
    var username by remember {
        mutableStateOf(initial?.username?.ifEmpty { null } ?: KODI_DEFAULT_USERNAME)
    }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var showError by remember { mutableStateOf(false) }

    val submit = {
        val host = parseKodiAddress(address, port)
        if (host == null) {
            showError = true
        } else {
            // A name typed by hand is the address; Kodi is asked for its own once it answers.
            val name = initial?.takeIf { it.isSameBox(host) }?.name ?: host.host
            connect(host.copy(name = name, username = username.trim(), password = password))
        }
    }

    TemplateDialog(
        title = { DialogTitle(text = stringResource(R.string.kodi_address_title)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextMMD(
                    text = stringResource(if (askingForLogin) R.string.kodi_login_note else R.string.kodi_address_note),
                    style = MaterialTheme.typography.labelSmall
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextFieldMMD(
                        value = address,
                        onValueChange = { address = it; showError = false },
                        modifier = Modifier.weight(1f),
                        label = { TextMMD(text = stringResource(R.string.kodi_address), style = MaterialTheme.typography.labelSmall) },
                        placeholder = { TextMMD(text = "192.168.1.20", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    Spacer(Modifier.width(12.dp))
                    TextFieldMMD(
                        value = port,
                        onValueChange = { new -> port = new.filter { it.isDigit() }.take(5); showError = false },
                        modifier = Modifier.width(88.dp),
                        label = { TextMMD(text = stringResource(R.string.kodi_port), style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true
                    )
                }
                TextFieldMMD(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { TextMMD(text = stringResource(R.string.kodi_username), style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    singleLine = true
                )
                TextFieldMMD(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { TextMMD(text = stringResource(R.string.kodi_password), style = MaterialTheme.typography.labelSmall) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true
                )
                if (showError) {
                    Spacer(Modifier.height(2.dp))
                    TextMMD(text = stringResource(R.string.kodi_address_wrong), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButtonText = stringResource(R.string.connect),
        onConfirmation = { submit() },
        dismissButtonText = stringResource(R.string.close),
        onDismissRequest = close
    )
}
