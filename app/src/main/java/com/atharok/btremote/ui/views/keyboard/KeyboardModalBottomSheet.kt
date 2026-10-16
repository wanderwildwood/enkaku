package com.atharok.btremote.ui.views.keyboard

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.atharok.btremote.ui.components.EInkSheet

@Composable
fun KeyboardModalBottomSheet(
    onShowKeyboardBottomSheetChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)
) {
    EInkSheet(
        onDismissRequest = { onShowKeyboardBottomSheetChanged(false) },
        modifier = modifier,
        content = content
    )
}
