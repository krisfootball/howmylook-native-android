package com.howmylook.app.data.post

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

class EditPostRepository {
    suspend fun updateOccasion(config: SupabaseConfig, postId: String, ownerUserId: String, occasion: String): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.from("posts").update({
                set("caption", occasion.trim().ifBlank { null })
            }) {
                filter {
                    eq("id", postId)
                    eq("user_id", ownerUserId)
                }
            }

            "Photo updated."
        }
    }
}
