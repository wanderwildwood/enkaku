package com.atharok.btremote.ui.components

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.ui.theme.rimWidth

/**
 * A panel along the bottom of the screen for the keyboards, in place of Material's bottom sheet.
 *
 * The bottom sheet slides up, dims everything behind it, and can be dragged — a smear, a
 * screenful of dithered grey and a gesture nobody needs on this panel. This one is simply
 * there, white with a black rule along its top edge, and goes when the back button or a press
 * outside it says so. It rides above the soft keyboard when one is up.
 */
@Composable
fun EInkSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setDimAmount(0f)
                // Dimming by nothing still counts as dimming: while a dimming window is up,
                // Android turns the status-bar icons white, and on the white bar they vanish.
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setGravity(Gravity.BOTTOM)
                @Suppress("DEPRECATION")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(rimWidth, MaterialTheme.colorScheme.onSurface),
            shape = RectangleShape,
            modifier = modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(content = content)
        }
    }
}

/**
 * Help, on a page of its own: a title, a cross to close it, and the text in sections that
 * turn over a few at a time with a swipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateModalBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            // No dim, as in EInkSheet above, so the status-bar icons stay dark.
            (view.parent as? DialogWindowProvider)?.window
                ?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxSize()
        ) {
            Column {
                TopBar(
                    title = title,
                    navigateUp = {
                        BasicIconButton(
                            onClick = onDismissRequest,
                            icon = AppIcons.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                )
                PagedColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    content()
                }
            }
        }
    }
}
