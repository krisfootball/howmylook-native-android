package com.howmylook.app.data.feed

data class RatingCard(
    val id: String,
    val authorId: String,
    val authorName: String,
    val occasion: String,
    val postKind: String = "single",
    val imageUrl: String? = null,
    val compareLeftImageUrl: String? = null,
    val compareRightImageUrl: String? = null,
    val yesCount: Int = 0,
    val noCount: Int = 0,
    val compareLeftPickCount: Int = 0,
    val compareRightPickCount: Int = 0,
    val needsMoreRatings: Int = 0,
)
