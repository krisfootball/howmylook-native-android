package com.howmylook.app.data.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@JsonIgnoreUnknownKeys
@Serializable
data class VoteResultDto(
    @SerialName("postId") val postId: String? = null,
    @SerialName("postKind") val postKind: String? = null,
    @SerialName("yesCount") val yesCount: Int? = null,
    @SerialName("noCount") val noCount: Int? = null,
    @SerialName("compareLeftPickCount") val compareLeftPickCount: Int? = null,
    @SerialName("compareRightPickCount") val compareRightPickCount: Int? = null,
    @SerialName("totalLikedGiven") val totalLikedGiven: Int? = null,
    @SerialName("totalSkippedGiven") val totalSkippedGiven: Int? = null,
    @SerialName("totalPickedGiven") val totalPickedGiven: Int? = null,
    @SerialName("unlockVotesCompleted") val unlockVotesCompleted: Int? = null,
    @SerialName("loginRatingVotesCompleted") val loginRatingVotesCompleted: Int? = null,
)
