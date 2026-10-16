package com.atharok.btremote.ui.components

import androidx.compose.runtime.Composable

/**
 * BT Remote crossfaded here. Nothing animates on this panel — a fade is a smear and two
 * repaints — so the new state simply replaces the old. The name is kept so its callers read
 * as they did upstream.
 */
@Composable
fun <T> FadeAnimatedContent(
    targetState: T,
    content: @Composable (targetState: T) -> Unit
) {
    content(targetState)
}
