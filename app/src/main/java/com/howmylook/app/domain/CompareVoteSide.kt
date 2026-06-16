package com.howmylook.app.domain

fun normalizeCompareSide(value: String?): String? {
    return when (value?.trim()?.lowercase()) {
        "left" -> "left"
        "right" -> "right"
        else -> null
    }
}
