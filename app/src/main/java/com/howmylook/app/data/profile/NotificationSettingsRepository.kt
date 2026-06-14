package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FollowNotificationRowDto(
    @SerialName("follower_id") val followerId: String? = null,
    @SerialName("following_id") val followingId: String? = null,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
)

@Serializable
private data class FollowNotificationUpdateDto(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerialName("notifications_enabled_at") val notificationsEnabledAt: String? = null,
)

class NotificationSettingsRepository {
    suspend fun load(config: SupabaseConfig, viewerUserId: String, followingId: String): Result<Boolean> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val row = client.from("follows")
                .select(columns = Columns.list("follower_id", "following_id", "notifications_enabled")) {
                    filter {
                        eq("follower_id", viewerUserId)
                        eq("following_id", followingId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<FollowNotificationRowDto>()
            row?.notificationsEnabled ?: false
        }
    }

    suspend fun setEnabled(config: SupabaseConfig, viewerUserId: String, followingId: String, enabled: Boolean): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.from("follows").update(
                FollowNotificationUpdateDto(
                    notificationsEnabled = enabled,
                    notificationsEnabledAt = if (enabled) java.time.Instant.now().toString() else null,
                )
            ) {
                filter {
                    eq("follower_id", viewerUserId)
                    eq("following_id", followingId)
                }
            }
            if (enabled) "Notifications on." else "Notifications off."
        }
    }
}
