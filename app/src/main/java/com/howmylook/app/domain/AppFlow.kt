package com.howmylook.app.domain

fun hasCompletedUsername(userId: String?, username: String?): Boolean {
    val normalized = username?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return false

    val fallbackPrefix = "user_"
    val userIdPrefix = userId?.take(8)?.lowercase()

    return !(userIdPrefix != null && normalized == "$fallbackPrefix$userIdPrefix")
}

fun getNextRequiredStep(
    isAuthenticated: Boolean,
    hasUsername: Boolean,
    ratingsCompleted: Int,
    unlockVoteCount: Int,
    bypassRatingGate: Boolean = false,
): AppStep {
    if (!isAuthenticated) return AppStep.AUTH
    if (!hasUsername) return AppStep.USERNAME
    if (!bypassRatingGate && ratingsCompleted < unlockVoteCount) return AppStep.RATING
    return AppStep.UNLOCKED
}
