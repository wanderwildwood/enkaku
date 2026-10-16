package com.atharok.btremote.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atharok.btremote.ui.theme.EInkDialog
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * BT Remote's dialogs, on the house dialog rather than Material's: no dimmed backdrop, no
 * animation, always the same width, and MMD buttons across the foot.
 */
@Composable
fun TemplateDialog(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonText: String? = null,
    onConfirmation: () -> Unit = {},
    dismissButtonText: String? = null,
    onDismissRequest: () -> Unit = {}
) {
    EInkDialog(onDismiss = onDismissRequest) {
        Column(modifier = modifier) {
            title()
            Spacer(Modifier.height(12.dp))
            content()
        }
        if (confirmButtonText != null || dismissButtonText != null) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                dismissButtonText?.let {
                    DialogButton(text = it, action = onDismissRequest, modifier = Modifier.weight(1f))
                }
                confirmButtonText?.let {
                    DialogButton(text = it, action = onConfirmation, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SimpleDialog(
    confirmButtonText: String?,
    dismissButtonText: String?,
    onConfirmation: () -> Unit,
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    modifier: Modifier = Modifier
) {
    TemplateDialog(
        title = {
            DialogTitle(text = dialogTitle)
        },
        content = {
            TextMMD(text = dialogText, style = MaterialTheme.typography.bodyMedium)
        },
        modifier = modifier,
        confirmButtonText = confirmButtonText,
        onConfirmation = onConfirmation,
        dismissButtonText = dismissButtonText,
        onDismissRequest = onDismissRequest
    )
}

/**
 * A choice among a few: pressing one chooses it and closes the dialog, so there is no OK to
 * press after it. The chosen one wears the black edge, which is how the house shows a choice.
 */
@Composable
fun ListDialog(
    confirmButtonText: String?,
    dismissButtonText: String?,
    onConfirmation: (itemSelected: Int) -> Unit,
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogMessage: String? = null,
    items: List<String>,
    defaultItemIndex: Int = 0
) {
    TemplateDialog(
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DialogTitle(text = dialogTitle)
                dialogMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    TextMMD(text = it, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        content = {
            LazyColumnMMD(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                isScrollbarVisible = false
            ) {
                itemsIndexed(items) { index, item ->
                    val chosen = index == defaultItemIndex
                    TextMMD(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (chosen) FontWeight.Bold else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .then(
                                if (chosen) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .clickable { onConfirmation(index) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        },
        dismissButtonText = dismissButtonText,
        onDismissRequest = onDismissRequest
    )
}

/**
 * Waiting on the other device. Said in words, without a spinner: on this panel a spinner is a
 * smear that never settles, and the words say all it would.
 */
@Composable
fun LoadingDialog(
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    TemplateDialog(
        title = {
            DialogTitle(text = title)
        },
        content = {
            TextMMD(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButtonText = buttonText,
        onConfirmation = onButtonClick,
        onDismissRequest = onButtonClick
    )
}

@Composable
fun DialogTitle(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 2
    )
}

@Composable
fun DialogButton(
    text: String,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButtonMMD(
        onClick = action,
        modifier = modifier.height(48.dp)
    ) {
        TextMMD(text = text, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
