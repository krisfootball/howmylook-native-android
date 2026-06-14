package com.howmylook.app.data.auth

data class UsernameFormState(
    val username: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val message: String = "",
    val error: String? = null,
)
