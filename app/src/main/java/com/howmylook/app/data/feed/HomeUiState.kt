package com.howmylook.app.data.feed

data class HomeUiState(
    val destination: HomeDestination = HomeDestination.LOCKED_HOME,
    val isLoading: Boolean = false,
    val statusMessage: String = "",
    val compareSelection: String? = null,
)
