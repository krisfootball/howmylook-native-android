package com.howmylook.app.data.post

data class PostDetailUiState(
    val loading: Boolean = false,
    val postId: String? = null,
    val authorName: String = "HowMyLook user",
    val occasion: String = "No occasion added yet",
    val imageUrls: List<String> = emptyList(),
    val postKind: String = "single",
    val compareLeftImageUrl: String? = null,
    val compareRightImageUrl: String? = null,
    val compareLeftPickCount: Int = 0,
    val compareRightPickCount: Int = 0,
    val selectedCompareSide: String? = null,
    val yesCount: Int = 0,
    val noCount: Int = 0,
    val ownerId: String? = null,
    val isOwnPost: Boolean = false,
    val hasViewerVoted: Boolean = false,
    val keepForever: Boolean = false,
    val fromRoute: String = "",
    val actionMessage: String = "",
    val error: String? = null,
)

data class FollowListPerson(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String? = null,
)

data class FollowListUiState(
    val loading: Boolean = false,
    val title: String = "People",
    val people: List<FollowListPerson> = emptyList(),
    val error: String? = null,
)
