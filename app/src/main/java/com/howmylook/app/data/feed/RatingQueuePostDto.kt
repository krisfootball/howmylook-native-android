package com.howmylook.app.data.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RatingQueuePostDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("caption") val caption: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

fun RatingQueuePostDto.toCard(authorName: String): RatingCard {
    val totalRatings = yesCount + noCount
    return RatingCard(
        id = id,
        authorName = authorName,
        occasion = caption ?: "No occasion added yet",
        imageUrl = imageUrl,
        yesCount = yesCount,
        noCount = noCount,
        needsMoreRatings = (5 - totalRatings).coerceAtLeast(0),
    )
}
