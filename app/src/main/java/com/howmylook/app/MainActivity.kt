package com.howmylook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.howmylook.app.ui.HowMyLookApp
import com.howmylook.app.ui.theme.HowMyLookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HowMyLookTheme {
                HowMyLookApp()
            }
        }
    }
}
