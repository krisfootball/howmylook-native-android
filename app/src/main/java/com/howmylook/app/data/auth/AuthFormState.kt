package com.howmylook.app.data.auth

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

data class AuthFormState(
    val mode: AuthMode = AuthMode.SIGN_UP,
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val message: String = "",
    val error: String? = null,
)
