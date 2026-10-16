package com.atharok.btremote.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.atharok.btremote.common.extensions.getActivity
import com.atharok.btremote.common.extensions.setFullScreen
import com.atharok.btremote.domain.entities.settings.AppearanceSettings
import com.mudita.mmd.ThemeMMD

/**
 * Black on white, in Mudita's type, and nothing else.
 *
 * BT Remote offered a light, a dark and a black theme and the phone's own wallpaper colours.
 * The panel can honour none of them, so of the appearance settings only full screen is kept.
 */
@Composable
fun BtRemoteTheme(
    appearance: AppearanceSettings,
    context: Context = LocalContext.current,
    content: @Composable () -> Unit
) {
    val useFullScreen = appearance.useFullScreen

    LaunchedEffect(useFullScreen) {
        context.setFullScreen(useFullScreen)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.getActivity()?.let { activity ->
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
            }
        }
    }

    ThemeMMD(colorScheme = monochrome, content = content)
}
