package com.howmylook.app.data.search

data class ExploreProfileCard(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val avatarUrl: String? = null,
    val isFollowing: Boolean = false,
)

data class ExploreLookCard(
    val id: String,
    val occasion: String,
    val postKind: String = "single",
    val imageUrl: String? = null,
    val compareLeftImageUrl: String? = null,
    val compareRightImageUrl: String? = null,
    val yesCount: Int = 0,
    val noCount: Int = 0,
    val compareLeftPickCount: Int = 0,
    val compareRightPickCount: Int = 0,
    val imageCount: Int = 1,
    val keepForever: Boolean = false,
    val authorDisplayName: String = "",
    val authorUsername: String = "",
)

data class SearchUiState(
    val loading: Boolean = false,
    val query: String = "",
    val people: List<ExploreProfileCard> = emptyList(),
    val looks: List<ExploreLookCard> = emptyList(),
    val error: String? = null,
)
