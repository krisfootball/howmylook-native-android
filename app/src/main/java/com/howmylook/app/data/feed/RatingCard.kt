package com.howmylook.app.data.feed

data class RatingCard(
    val id: String,
    val authorId: String,
    val authorName: String,
    val occasion: String,
    val imageUrl: String? = null,
    val yesCount: Int = 0,
    val noCount: Int = 0,
    val needsMoreRatings: Int = 0,
)
