package com.howmylook.app.data

fun toFriendlyUploadError(message: String?): String {
    val lower = message?.lowercase().orEmpty()
    return when {
        lower.contains("post-images") || lower.contains("bucket") ->
            "Photo upload is wired, but the Supabase storage bucket still needs setup or access. Check SUPABASE_STORAGE_SETUP.sql and bucket permissions."
        lower.contains("row-level security") || lower.contains("permission") ->
            "Posting is wired, but Supabase permissions are blocking it right now. Check the RLS/storage policies for posts and post-images."
        lower.contains("network") || lower.contains("timeout") || lower.contains("host") ->
            "Network issue while uploading. Try again on a stable connection."
        lower.isBlank() -> "Unable to create post right now."
        else -> message!!
    }
}

fun toFriendlyFollowError(message: String?): String {
    val lower = message?.lowercase().orEmpty()
    return when {
        lower.contains("row-level security") || lower.contains("permission") ->
            "Follow is wired, but Supabase still needs the right RLS policies for the follows table."
        lower.contains("network") || lower.contains("timeout") || lower.contains("host") ->
            "Network issue while updating follow state. Try again."
        lower.isBlank() -> "Unable to update following right now."
        else -> message!!
    }
}
