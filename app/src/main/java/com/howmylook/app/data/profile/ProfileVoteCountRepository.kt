package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.post.onlyNonExpiredPosts
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ProfileVoteCounts(
    val liked: Int = 0,
    val skipped: Int = 0,
    val picked: Int = 0,
)

@Serializable
private data class ProfileVoteCountRowDto(
    @SerialName("post_id") val postId: String? = null,
)

@Serializable
private data class VisiblePostIdRowDto(
    @SerialName("id") val id: String? = null,
)

class ProfileVoteCountRepository {
    suspend fun loadCurrentCounts(config: SupabaseConfig, userId: String): Result<ProfileVoteCounts> {
        return runCatching {
            val client = SupabaseProvider.create(config)

            val likedRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", "yes")
                        eq("vote_kind", "single")
                    }
                }
                .decodeList<ProfileVoteCountRowDto>()

            val skippedRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", "no")
                        eq("vote_kind", "single")
                    }
                }
                .decodeList<ProfileVoteCountRowDto>()

            val pickedRows = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("vote_kind", "compare")
                    }
                }
                .decodeList<ProfileVoteCountRowDto>()

            val votedPostIds = (
                likedRows.mapNotNull { it.postId } +
                    skippedRows.mapNotNull { it.postId } +
                    pickedRows.mapNotNull { it.postId }
                ).distinct()

            val visiblePostIds = if (votedPostIds.isEmpty()) {
                emptySet()
            } else {
                client.from("posts")
                    .select(columns = Columns.list("id")) {
                        filter {
                            isIn("id", votedPostIds)
                            eq("is_active", true)
                            eq("moderation_status", "approved")
                            onlyNonExpiredPosts()
                        }
                    }
                    .decodeList<VisiblePostIdRowDto>()
                    .mapNotNull { it.id }
                    .toSet()
            }

            fun countVisible(rows: List<ProfileVoteCountRowDto>): Int {
                return rows.count { row -> row.postId != null && visiblePostIds.contains(row.postId) }
            }

            ProfileVoteCounts(
                liked = countVisible(likedRows),
                skipped = countVisible(skippedRows),
                picked = countVisible(pickedRows),
            )
        }
    }
}
