package com.howmylook.app.data.auth

data class ProfileRecord(
    val id: String,
    val username: String? = null,
    val displayName: String? = null,
    val loginRatingVotesCompleted: Int = 0,
    val isAdmin: Boolean = false,
)
