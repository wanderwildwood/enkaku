package com.atharok.btremote.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * BT Remote's three buttons, as MMD's two. The word is the button; the icon BT Remote put
 * beside it is accepted and not drawn, so its callers read as they did upstream.
 */
@Composable
fun MaterialButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    @Suppress("UNUSED_PARAMETER") icon: ImageVector? = null
) {
    ButtonMMD(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        TextMMD(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MaterialFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    icon: ImageVector? = null
) {
    MaterialOutlinedButton(onClick = onClick, modifier = modifier, text = text, icon = icon)
}

@Composable
fun MaterialOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    @Suppress("UNUSED_PARAMETER") icon: ImageVector? = null
) {
    OutlinedButtonMMD(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        TextMMD(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
