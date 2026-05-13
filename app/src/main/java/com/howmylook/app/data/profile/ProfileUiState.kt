package com.howmylook.app.data.profile

data class ProfileUiState(
    val loading: Boolean = false,
    val profileId: String? = null,
    val displayName: String = "Your profile",
    val username: String = "@username",
    val bio: String = "No bio yet.",
    val avatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val yesGiven: Int = 0,
    val noGiven: Int = 0,
    val isOwnProfile: Boolean = true,
    val isFollowing: Boolean = false,
    val error: String? = null,
)
