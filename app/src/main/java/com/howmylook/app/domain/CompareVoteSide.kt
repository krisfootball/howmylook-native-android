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
        // cast_decision_vote stores left picks as "yes" and right picks as "no"
        return when (value?.trim()?.lowercase()) {
            "yes" -> "left"
            "no" -> "right"
            else -> null
        }
    }
    return null
}
