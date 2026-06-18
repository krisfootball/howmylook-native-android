package com.howmylook.app.data.admin

import com.howmylook.app.data.search.ExploreLookCard

data class AdminUiState(
    val loading: Boolean = false,
    val posts: List<ExploreLookCard> = emptyList(),
    val actionMessage: String = "",
    val error: String? = null,
)
