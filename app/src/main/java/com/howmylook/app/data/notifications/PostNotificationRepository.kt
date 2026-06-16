package com.howmylook.app.data.notifications

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class NotifyPostFollowersResponse(
    @SerialName("sent") val sent: Int = 0,
    @SerialName("skipped") val skipped: String? = null,
)

class PostNotificationRepository {
    suspend fun notifyFollowers(config: SupabaseConfig, postId: String): Result<NotifyPostFollowersResponse> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.functions.invoke(
                function = "notify-post-followers",
                body = buildJsonObject {
                    put("postId", postId)
                },
            ).body<NotifyPostFollowersResponse>()
        }
    }
}
