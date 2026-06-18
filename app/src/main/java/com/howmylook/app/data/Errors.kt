package com.howmylook.app.data

fun toFriendlyAuthError(message: String?): String {
    val lower = message?.lowercase().orEmpty()
    return when {
        lower.contains("invalid login credentials") -> "Invalid email or password."
        lower.contains("email not confirmed") || lower.contains("confirm") -> "Check your email and confirm your account, then sign in."
        lower.contains("no session") -> "Signed in, but no session was available yet. Try again once, or reopen the app."
        lower.contains("row-level security") && lower.contains("profiles") -> "Account auth worked, but the profiles table is rejecting the profile write. The signup flow or Supabase RLS needs alignment."
        lower.contains("network") || lower.contains("timeout") || lower.contains("host") -> "Network issue while contacting Supabase. Try again."
        lower.isBlank() -> "Authentication failed."
        else -> message!!
    }
}

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

fun toFriendlyAccountDeletionError(message: String?): String {
    val lower = message?.lowercase().orEmpty()
    return when {
        lower.contains("function") && (lower.contains("not found") || lower.contains("404")) ->
            "Account deletion is not set up on the server yet. Deploy the delete-account Supabase edge function first."
        lower.contains("verify your session") || lower.contains("signed in") ->
            "You need to be signed in before deleting your account."
        lower.contains("network") || lower.contains("timeout") || lower.contains("host") ->
            "Network issue while deleting your account. Try again."
        lower.isBlank() -> "Unable to delete account right now."
        else -> message!!
    }
}
