package com.howmylook.app.data.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VotePostIdRowDto(
    @SerialName("post_id") val postId: String,
)
