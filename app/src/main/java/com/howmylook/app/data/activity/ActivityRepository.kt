package com.howmylook.app.data.activity

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ActivityOwnPostDto(
    @SerialName("id") val id: String,
)

@Serializable
private data class ActivityProfileDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
)

@Serializable
private data class ActivityFollowRowDto(
    @SerialName("follower_id") val followerId: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class ActivityVoteRowDto(
    @SerialName("post_id") val postId: String,
    @SerialName("value") val value: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
private data class ActivityNotificationRowDto(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("created_at") val createdAt: String,
)

class ActivityRepository {
    suspend fun load(config: SupabaseConfig, userId: String): Result<ActivityUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)

            val followRows = client.from("follows")
                .select(columns = Columns.list("follower_id", "created_at")) {
                    filter { eq("following_id", userId) }
                    limit(50)
                }
                .decodeList<ActivityFollowRowDto>()

            val ownPosts = client.from("posts")
                .select(columns = Columns.list("id")) {
                    filter { eq("user_id", userId) }
                    limit(100)
                }
                .decodeList<ActivityOwnPostDto>()

            val ownPostIds = ownPosts.map { it.id }

            val notifications = client.from("user_notifications")
                .select(columns = Columns.list("id", "kind", "title", "body", "post_id", "created_at")) {
                    filter { eq("user_id", userId) }
                    limit(100)
                }
                .decodeList<ActivityNotificationRowDto>()

            val voteRows = if (ownPostIds.isEmpty()) {
                emptyList()
            } else {
                client.from("votes")
                    .select(columns = Columns.list("post_id", "value", "created_at", "user_id")) {
                        filter {
                            isIn("post_id", ownPostIds)
                            neq("user_id", userId)
                        }
                        limit(100)
                    }
                    .decodeList<ActivityVoteRowDto>()
            }

            val followItems = followRows.mapIndexed { index, row ->
                val profile = client.from("profiles")
                    .select(columns = Columns.list("display_name", "username")) {
                        filter { eq("id", row.followerId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<ActivityProfileDto>()

                val name = profile?.displayName ?: profile?.username ?: "Someone"
                ActivityItem(
                    id = "follow-${row.followerId}-$index",
                    createdAt = row.createdAt,
                    title = ensureTrailingPeriod("$name followed you"),
                    subtitle = profile?.username?.let { "@$it" } ?: "New follower",
                    targetProfileId = row.followerId,
                )
            }

            val voteItems = voteRows.mapIndexed { index, row ->
                val profile = client.from("profiles")
                    .select(columns = Columns.list("display_name", "username")) {
                        filter { eq("id", row.userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<ActivityProfileDto>()

                val name = profile?.displayName ?: profile?.username ?: "Someone"
                ActivityItem(
                    id = "vote-${row.postId}-${row.createdAt}-$index",
                    createdAt = row.createdAt,
                    title = ensureTrailingPeriod("$name voted ${row.value} on your post"),
                    targetPostId = row.postId,
                )
            }

            val moderationItems = notifications
                .filter { it.kind == "moderation_removed" }
                .map {
                    ActivityItem(
                        id = "moderation-${it.id}",
                        createdAt = it.createdAt,
                        title = ensureTrailingPeriod(it.title),
                        subtitle = it.body?.trim().orEmpty().ifBlank { "One of your looks" },
                    )
                }

            ActivityUiState(
                loading = false,
                items = (followItems + voteItems + moderationItems).sortedByDescending { it.createdAt },
                error = null,
            )
        }
    }

    private fun ensureTrailingPeriod(text: String): String {
        return if (text.endsWith('.')) text else "$text."
    }
}
