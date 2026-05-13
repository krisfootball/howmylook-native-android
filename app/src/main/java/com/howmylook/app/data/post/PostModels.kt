package com.howmylook.app.data.post

data class PostDetailUiState(
    val loading: Boolean = false,
    val postId: String? = null,
    val authorName: String = "HowMyLook user",
    val occasion: String = "No occasion added yet",
    val imageUrls: List<String> = emptyList(),
    val yesCount: Int = 0,
    val noCount: Int = 0,
    val error: String? = null,
)

data class FollowListPerson(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
)

data class FollowListUiState(
    val loading: Boolean = false,
    val title: String = "People",
    val people: List<FollowListPerson> = emptyList(),
    val error: String? = null,
)
