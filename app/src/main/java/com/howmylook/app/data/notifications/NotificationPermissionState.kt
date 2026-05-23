package com.howmylook.app.data.notifications

data class NotificationPermissionState(
    val supported: Boolean = true,
    val granted: Boolean = false,
    val requestedThisSession: Boolean = false,
)
