package com.howmylook.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostIdRowDto(
    @SerialName("id") val id: String,
)
