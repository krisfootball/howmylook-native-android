package com.howmylook.app.data.activity

data class ActivityItem(
    val id: String,
    val createdAt: String = "",
    val title: String,
    val subtitle: String = "",
    val targetProfileId: String? = null,
    val targetPostId: String? = null,
)

data class ActivityUiState(
    val loading: Boolean = false,
    val items: List<ActivityItem> = emptyList(),
    val error: String? = null,
)
