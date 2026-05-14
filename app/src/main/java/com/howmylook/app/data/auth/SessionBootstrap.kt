package com.howmylook.app.data.auth

import com.howmylook.app.domain.AppStep

data class SessionBootstrap(
    val step: AppStep,
    val isConfigured: Boolean,
    val isSignedIn: Boolean,
    val profile: ProfileRecord? = null,
    val availablePostCount: Int = 0,
    val message: String = "",
    val debugMessage: String? = null,
)
