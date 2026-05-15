package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class EditableProfileDto(
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class UsernameConflictDto(
    @SerialName("id") val id: String,
)

@Serializable
private data class ProfileUpdateDto(
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
)

class EditProfileRepository {
    suspend fun load(config: SupabaseConfig, userId: String): Result<EditProfileFormState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val profile = client.from("profiles")
                .select(columns = Columns.list("username", "display_name", "bio", "avatar_url")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<EditableProfileDto>()

            EditProfileFormState(
                loading = false,
                saving = false,
                username = profile?.username.orEmpty(),
                displayName = profile?.displayName.orEmpty(),
                bio = profile?.bio.orEmpty(),
                avatarUrl = profile?.avatarUrl,
                message = "",
                error = null,
            )
        }
    }

    suspend fun save(config: SupabaseConfig, userId: String, username: String, displayName: String, bio: String): Result<String> {
        return runCatching {
            val cleanUsername = username.trim().lowercase()
            if (cleanUsername.isBlank()) error("Username is required.")
            if (cleanUsername.length < 3) error("Username must be at least 3 characters.")

            val client = SupabaseProvider.create(config)
            val existing = client.from("profiles")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("username", cleanUsername)
                        neq("id", userId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<UsernameConflictDto>()

            if (existing != null) {
                error("That username is already taken.")
            }

            client.from("profiles").update(
                ProfileUpdateDto(
                    username = cleanUsername,
                    displayName = displayName.trim().ifBlank { null },
                    bio = bio.trim().ifBlank { null },
                )
            ) {
                filter { eq("id", userId) }
            }

            "Profile updated."
        }
    }
}
