package com.howmylook.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VotePostIdRowDto(
    @SerialName("post_id") val postId: String,
)
