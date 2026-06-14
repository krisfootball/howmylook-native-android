package com.howmylook.app.data.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RatingQueuePostDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("caption") val caption: String? = null,
    @SerialName("post_kind") val postKind: String = "single",
    @SerialName("compare_left_image_url") val compareLeftImageUrl: String? = null,
    @SerialName("compare_right_image_url") val compareRightImageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("compare_left_pick_count") val compareLeftPickCount: Int = 0,
    @SerialName("compare_right_pick_count") val compareRightPickCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

fun RatingQueuePostDto.toCard(authorName: String): RatingCard {
    val totalRatings = if (postKind == "compare") {
        compareLeftPickCount + compareRightPickCount
    } else {
        yesCount + noCount
    }
    return RatingCard(
        id = id,
        authorId = userId,
        authorName = authorName,
        occasion = caption ?: "No occasion added yet",
        postKind = postKind,
        imageUrl = imageUrl,
        compareLeftImageUrl = compareLeftImageUrl,
        compareRightImageUrl = compareRightImageUrl,
        yesCount = yesCount,
        noCount = noCount,
        compareLeftPickCount = compareLeftPickCount,
        compareRightPickCount = compareRightPickCount,
        needsMoreRatings = (5 - totalRatings).coerceAtLeast(0),
    )
}
