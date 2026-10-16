package com.atharok.btremote.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
private fun OverflowMenu(
    item: @Composable (showMenu: () -> Unit) -> Unit,
    content: @Composable (closeDropdownMenu: () -> Unit) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    item { showMenu = !showMenu }

    DropdownMenuMMD(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        content { showMenu = false }
    }
}

// ---- DropdownMenu ----

@Composable
fun BasicOverflowMenu(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable (closeDropdownMenu: () -> Unit) -> Unit
) {
    OverflowMenu(
        item = {
            BasicIconButton(
                onClick = it,
                icon = icon,
                contentDescription = contentDescription,
                modifier = modifier
            )
        },
        content = content
    )
}

@Composable
fun MoreOverflowMenu(
    modifier: Modifier = Modifier,
    content: @Composable (closeDropdownMenu: () -> Unit) -> Unit
) {
    BasicOverflowMenu(
        icon = AppIcons.MoreVert,
        contentDescription = stringResource(id = R.string.more),
        modifier = modifier,
        content = content
    )
}

// ---- DropdownMenuItem ----

/** A word, no icon: a glyph beside every line of a short menu is decoration the panel smudges. */
@Composable
fun BasicDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItemMMD(
        text = { TextMMD(text = text, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        modifier = modifier
    )
}
