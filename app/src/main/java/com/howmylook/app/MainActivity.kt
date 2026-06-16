package com.howmylook.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howmylook.app.ui.HowMyLookApp
import com.howmylook.app.ui.theme.HowMyLookTheme
import com.howmylook.app.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private var notificationPostId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationPostId = intent?.getStringExtra("postId")

        setContent {
            HowMyLookTheme {
                val appViewModel: AppViewModel = viewModel()
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {
                        appViewModel.refreshNotificationPermissionState(this)
                    },
                )

                LaunchedEffect(Unit) {
                    appViewModel.refreshNotificationPermissionState(this@MainActivity)
                    appViewModel.registerPendingPushToken(this@MainActivity)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !appViewModel.notificationPermissionState.granted && !appViewModel.notificationPermissionState.requestedThisSession) {
                        appViewModel.markNotificationPermissionRequested()
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                LaunchedEffect(notificationPostId) {
                    val postId = notificationPostId ?: return@LaunchedEffect
                    appViewModel.handleNotificationOpen(postId)
                    notificationPostId = null
                }

                HowMyLookApp(appViewModel = appViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationPostId = intent.getStringExtra("postId")
    }
}
