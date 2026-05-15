package com.howmylook.app.data.activity

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
)

data class ActivityUiState(
    val loading: Boolean = false,
    val items: List<ActivityItem> = emptyList(),
    val error: String? = null,
)
