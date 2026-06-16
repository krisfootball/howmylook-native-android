package com.howmylook.app.domain

fun normalizeCompareSide(value: String?): String? {
    return when (value?.trim()?.lowercase()) {
        "left", "l", "compare_left", "pick_left" -> "left"
        "right", "r", "compare_right", "pick_right" -> "right"
        else -> null
    }
}

fun resolveCompareVoteSide(value: String?, voteKind: String? = null): String? {
    normalizeCompareSide(value)?.let { return it }
    if (voteKind == null || voteKind == "compare") {
        return when (value?.trim()?.lowercase()) {
            "yes" -> "right"
            "no" -> "left"
            else -> null
        }
    }
    return null
}
