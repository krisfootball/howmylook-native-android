package com.howmylook.app.data.auth

data class SessionState(
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = false,
    val needsUsername: Boolean = false,
    val needsUnlockRatings: Boolean = true,
    val unlockVotesCompleted: Int = 0,
    val availablePostCount: Int = 0,
    val bootstrapMessage: String = "",
    val debugMessage: String? = null,
)
