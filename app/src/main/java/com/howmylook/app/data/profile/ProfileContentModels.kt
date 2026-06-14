package com.howmylook.app.data.profile

import com.howmylook.app.data.search.ExploreLookCard

data class VoteHistoryUiState(
    val loading: Boolean = false,
    val title: String = "Vote history",
    val posts: List<ExploreLookCard> = emptyList(),
    val error: String? = null,
)

data class EditProfileFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val selectedAvatarUri: String? = null,
    val selectedAvatarName: String? = null,
    val removeAvatar: Boolean = false,
    val message: String = "",
    val error: String? = null,
)
