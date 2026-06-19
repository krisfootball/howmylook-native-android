package com.howmylook.app.data.profile

import android.content.ContentResolver
import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.upload.loadUploadPhotoPayload
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024

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
    @SerialName("avatar_url") val avatarUrl: String? = null,
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
            val accountEmail = runCatching {
                client.auth.retrieveUserForCurrentSession(updateSession = false).email.orEmpty()
            }.getOrDefault("")

            EditProfileFormState(
                loading = false,
                saving = false,
                accountEmail = accountEmail,
                username = profile?.username.orEmpty(),
                displayName = profile?.displayName.orEmpty(),
                bio = profile?.bio.orEmpty(),
                avatarUrl = profile?.avatarUrl,
                selectedAvatarUri = null,
                selectedAvatarName = null,
                removeAvatar = false,
                message = "",
                error = null,
            )
        }
    }

    suspend fun save(
        config: SupabaseConfig,
        userId: String,
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String?,
        selectedAvatarUri: String?,
        removeAvatar: Boolean,
        contentResolver: ContentResolver,
    ): Result<String> {
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

            var nextAvatarUrl = if (removeAvatar) null else avatarUrl
            var avatarWarning: String? = null

            if (!selectedAvatarUri.isNullOrBlank()) {
                val payload = loadUploadPhotoPayload(contentResolver, selectedAvatarUri)
                if (payload.bytes.size > MAX_AVATAR_SIZE_BYTES) {
                    error("Profile photo must be 5 MB or smaller.")
                }

                val fileExt = payload.fileName.substringAfterLast('.', "jpg")
                val filePath = "$userId/avatar-${System.currentTimeMillis()}.$fileExt"

                try {
                    val bucket = client.storage.from("profile-avatars")
                    bucket.upload(filePath, payload.bytes) {
                        upsert = true
                        contentType = ContentType.parse(payload.mimeType)
                    }
                    nextAvatarUrl = bucket.publicUrl(filePath)
                } catch (error: Throwable) {
                    val uploadMessage = error.message?.lowercase().orEmpty()
                    if (uploadMessage.contains("bucket") || uploadMessage.contains("profile-avatars")) {
                        avatarWarning = "Text profile changes were saved, but profile photo upload still needs SUPABASE_STORAGE_PROFILE_AVATARS.sql run in Supabase."
                    } else {
                        throw IllegalStateException("Profile photo upload failed: ${error.message ?: "unknown error"}", error)
                    }
                }
            }

            client.from("profiles").update(
                ProfileUpdateDto(
                    username = cleanUsername,
                    displayName = displayName.trim().ifBlank { null },
                    bio = bio.trim().ifBlank { null },
                    avatarUrl = nextAvatarUrl,
                )
            ) {
                filter { eq("id", userId) }
            }

            avatarWarning ?: "Profile updated."
        }
    }
}
