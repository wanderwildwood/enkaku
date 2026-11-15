package com.atharok.btremote.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.atharok.btremote.common.utils.DEFAULT_TURN_WITH_PHONE
import com.atharok.btremote.ui.app.BtRemoteApp
import com.atharok.btremote.ui.components.Notice
import com.atharok.btremote.ui.components.onEachPress

class MainActivity : ComponentActivity() {
    private val turner = TurnWithPhone(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before anything is drawn, so a window built again after a turn comes up the way it was;
        // the stored setting follows as soon as it is read.
        turner.set(DEFAULT_TURN_WITH_PHONE)
        enableEdgeToEdge()
        setContent {
            // A notice along the foot (drawn by BtRemoteApp) stays until the next press.
            Box(Modifier.fillMaxSize().onEachPress { Notice.clear() }) {
                BtRemoteApp(turnWithPhone = { turner.set(it) })
            }
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        Notice.pressed(event)
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        turner.resume()
    }

    override fun onPause() {
        turner.pause()
        super.onPause()
    }
}
