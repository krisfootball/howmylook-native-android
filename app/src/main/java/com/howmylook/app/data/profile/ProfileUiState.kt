package com.howmylook.app.data.profile

import com.howmylook.app.data.search.ExploreLookCard

data class ProfileUiState(
    val loading: Boolean = false,
    val profileId: String? = null,
    val displayName: String = "Your profile",
    val username: String = "@username",
    val bio: String = "No bio yet.",
    val avatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val likedGiven: Int = 0,
    val skippedGiven: Int = 0,
    val pickedGiven: Int = 0,
    val posts: List<ExploreLookCard> = emptyList(),
    val isOwnProfile: Boolean = true,
    val isFollowing: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val actionMessage: String = "",
    val error: String? = null,
)
