package com.howmylook.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("login_rating_votes_completed") val loginRatingVotesCompleted: Int? = null,
)

fun ProfileDto.toRecord(): ProfileRecord {
    return ProfileRecord(
        id = id,
        username = username,
        displayName = displayName,
        loginRatingVotesCompleted = loginRatingVotesCompleted ?: 0,
    )
}
