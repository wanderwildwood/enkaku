package com.atharok.btremote.ui.views.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.atharok.btremote.R
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.ui.components.PagedColumn
import com.atharok.btremote.ui.components.TemplateDialog
import com.atharok.btremote.ui.components.DialogTitle
import com.atharok.btremote.ui.components.TextSmall
import com.atharok.btremote.ui.components.customButtons.RemoteIconSurfaceButton

@Composable
fun MoreButtonsDialog(
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    TemplateDialog(
        title = {
            DialogTitle(text = stringResource(R.string.more_buttons))
        },
        content = {
            MoreButtonsLayout(
                sendRemoteKeyReport = sendRemoteKeyReport,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier
            )
        },
        modifier = modifier,
        dismissButtonText = stringResource(R.string.close),
        onDismissRequest = onDismissRequest
    )
}

/** One round button of More Buttons and the label under it. */
private class MoreButton(
    val properties: RemoteButtonProperties,
    val keyboard: Boolean = false,
    @androidx.annotation.StringRes val label: Int = properties.stringRes
)

// Three to a row: at four, a column on the Kompakt (480 px) is narrower than "Forward",
// "Previous", "captions" or "Capture", and the label broke inside the word.
private const val MORE_BUTTONS_PER_ROW = 3

@Composable
private fun MoreButtonsLayout(
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    buttonShape: Shape = CircleShape,
    padding: Dp = dimensionResource(id = R.dimen.padding_large)
) {
    val kodi = LocalKodiRemote.current
    val buttons = buildList {
        add(MoreButton(RemoteButtonProperties.PlayButton))
        add(MoreButton(RemoteButtonProperties.PauseButton))
        add(MoreButton(RemoteButtonProperties.StopButton))
        add(MoreButton(RemoteButtonProperties.RepeatButton))
        add(MoreButton(RemoteButtonProperties.RewindButton))
        add(MoreButton(RemoteButtonProperties.ForwardButton))
        add(MoreButton(RemoteButtonProperties.PreviousTrackButton))
        add(MoreButton(RemoteButtonProperties.NextTrackButton))
        add(MoreButton(RemoteButtonProperties.ClosedCaptionsButton))
        add(MoreButton(RemoteButtonProperties.KeyboardPrintScreenButton, keyboard = true))
        // Kodi has no brightness.
        if (!kodi) {
            add(MoreButton(RemoteButtonProperties.BrightnessDownButton, label = R.string.brightness_down_short))
            add(MoreButton(RemoteButtonProperties.BrightnessUpButton, label = R.string.brightness_up_short))
        }
        add(MoreButton(RemoteButtonProperties.RedMenuButton))
        add(MoreButton(RemoteButtonProperties.GreenMenuButton))
        add(MoreButton(RemoteButtonProperties.BlueMenuButton))
        add(MoreButton(RemoteButtonProperties.YellowMenuButton))
    }
    PagedColumn(
        modifier = modifier.heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(padding)
    ) {
        buttons.chunked(MORE_BUTTONS_PER_ROW).forEach { row ->
            item { Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.spacedBy(padding)
            ) {
                row.forEach { button ->
                    MoreRemoteIconLayout(
                        properties = button.properties,
                        label = button.label,
                        sendKeyReport = if (button.keyboard) sendKeyboardKeyReport else sendRemoteKeyReport,
                        modifier = Modifier.weight(1f),
                        buttonShape = buttonShape,
                    )
                }
                // A short last row keeps the grid's columns.
                repeat(MORE_BUTTONS_PER_ROW - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } }
        }
    }
}

@Composable
private fun MoreRemoteIconLayout(
    properties: RemoteButtonProperties,
    sendKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    buttonShape: Shape = RectangleShape,
    @androidx.annotation.StringRes label: Int = properties.stringRes,
    buttonIconTint: Color = LocalContentColor.current
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RemoteIconSurfaceButton(
            properties = properties,
            sendKeyReport = sendKeyReport,
            modifier = Modifier.aspectRatio(1f),
            shape = buttonShape,
            iconTint = buttonIconTint
        )
        TextSmall(text = stringResource(label), textAlign = TextAlign.Center)
    }
}