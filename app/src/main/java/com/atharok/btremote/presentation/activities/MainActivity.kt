package com.atharok.btremote.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.atharok.btremote.common.utils.DEFAULT_TURN_WITH_PHONE
import com.atharok.btremote.ui.app.BtRemoteApp

class MainActivity : ComponentActivity() {
    private val turner = TurnWithPhone(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before anything is drawn, so a window built again after a turn comes up the way it was;
        // the stored setting follows as soon as it is read.
        turner.set(DEFAULT_TURN_WITH_PHONE)
        enableEdgeToEdge()
        setContent {
            BtRemoteApp(turnWithPhone = { turner.set(it) })
        }
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
