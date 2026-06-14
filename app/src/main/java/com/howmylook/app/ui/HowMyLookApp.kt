package com.howmylook.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howmylook.app.navigation.AppNavigation
import com.howmylook.app.viewmodel.AppViewModel

@Composable
fun HowMyLookApp(appViewModel: AppViewModel = viewModel()) {
    AppNavigation(viewModel = appViewModel)
}
