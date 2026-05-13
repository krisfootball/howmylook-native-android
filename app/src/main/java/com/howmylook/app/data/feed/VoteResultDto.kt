package com.howmylook.app.data.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteResultDto(
    @SerialName("postId") val postId: String? = null,
    @SerialName("yesCount") val yesCount: Int? = null,
    @SerialName("noCount") val noCount: Int? = null,
    @SerialName("unlockVotesCompleted") val unlockVotesCompleted: Int? = null,
    @SerialName("loginRatingVotesCompleted") val loginRatingVotesCompleted: Int? = null,
)
