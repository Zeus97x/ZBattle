package com.zeus97x.zbattle

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.AndroidViewModel
import com.zeus97x.zbattle.ui.AppState
import com.zeus97x.zbattle.ui.LocalArtLoader
import com.zeus97x.zbattle.ui.ZBattleApp

/** Survives rotation so navigation, sheets and the decoded-art cache are kept. */
class ZBattleViewModel(application: Application) : AndroidViewModel(application) {
    val state = AppState(PrefsSettingsStore(application))
    val artLoader = AssetArtLoader(application.assets)
}

class MainActivity : ComponentActivity() {
    private val model: ZBattleViewModel by viewModels()

    // System Back goes through NavState: overlay, then screen, then other tabs to Home, then exit.
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!model.state.back()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backCallback)
        setContent {
            CompositionLocalProvider(LocalArtLoader provides model.artLoader) {
                ZBattleApp(model.state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-arm after an exit-to-background so Back keeps closing sheets when the user returns.
        backCallback.isEnabled = true
    }

    override fun onStop() {
        super.onStop()
        // Auto battle is foreground-only (D-AUTO-FIGHT proposal): leaving the app stops it.
        model.state.stopAutoFight()
    }
}
