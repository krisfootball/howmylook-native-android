package com.howmylook.app.data.feed

import com.howmylook.app.data.post.onlyNonExpiredPosts
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class FollowEdgeDto(
    @SerialName("following_id") val followingId: String,
)

@Serializable
private data class FeedProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
)

class FeedRepository {
    suspend fun loadRatingQueue(config: SupabaseConfig, userId: String): Result<List<RatingCard>> {
        return runCatching {
            val client = SupabaseProvider.create(config)

            val posts = client.from("posts")
                .select(columns = Columns.list("id", "user_id", "image_url", "caption", "post_kind", "compare_left_image_url", "compare_right_image_url", "yes_count", "no_count", "compare_left_pick_count", "compare_right_pick_count", "created_at")) {
                    filter {
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                        neq("user_id", userId)
                        onlyNonExpiredPosts()
                    }
                    order("created_at", Order.DESCENDING)
                    limit(50)
                }
                .decodeList<RatingQueuePostDto>()

            val existingVotes = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<VotePostIdRowDto>()

            val ratedPostIds = existingVotes.map { it.postId }.toSet()

            val followingIds = client.from("follows")
                .select(columns = Columns.list("following_id")) {
                    filter { eq("follower_id", userId) }
                }
                .decodeList<FollowEdgeDto>()
                .map { it.followingId }
                .toSet()

            fun ratingTotal(post: RatingQueuePostDto): Int = if (post.postKind == "compare") {
                post.compareLeftPickCount + post.compareRightPickCount
            } else {
                post.yesCount + post.noCount
            }

            val filteredPosts = posts.filter { post -> post.id !in ratedPostIds }
            val underFiveFollowed = filteredPosts.filter { ratingTotal(it) < 5 && followingIds.contains(it.userId) }
            val underFiveOthers = filteredPosts.filter { ratingTotal(it) < 5 && !followingIds.contains(it.userId) }
            val fallbackFollowed = filteredPosts.filter { ratingTotal(it) >= 5 && followingIds.contains(it.userId) }
            val fallbackOthers = filteredPosts.filter { ratingTotal(it) >= 5 && !followingIds.contains(it.userId) }
            val orderedPosts = underFiveFollowed + underFiveOthers + fallbackFollowed + fallbackOthers

            val authorIds = orderedPosts.map { it.userId }.distinct()
            val authorProfiles = if (authorIds.isEmpty()) {
                emptyMap()
            } else {
                client.from("profiles")
                    .select(columns = Columns.list("id", "display_name", "username")) {
                        filter { isIn("id", authorIds) }
                    }
                    .decodeList<FeedProfileDto>()
                    .associateBy { it.id }
            }

            orderedPosts.map { post ->
                val author = authorProfiles[post.userId]
                post.toCard(author?.displayName ?: author?.username ?: "HowMyLook user")
            }
        }
    }

    suspend fun castVote(config: SupabaseConfig, postId: String, voteValue: String): Result<VoteResultDto> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val userResult = client.auth.retrieveUserForCurrentSession(updateSession = true)
            if (userResult.id.isBlank()) {
                error("Sign in first before rating looks.")
            }

            val functionName = if (voteValue == "left" || voteValue == "right") "cast_decision_vote" else "cast_vote"
            client.postgrest.rpc(
                function = functionName,
                parameters = buildJsonObject {
                    put("target_post_id", JsonPrimitive(postId))
                    put("vote_value", JsonPrimitive(voteValue))
                }
            ).decodeAs<VoteResultDto>()
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val message = error.message.orEmpty().lowercase()
                val friendlyMessage = when {
                    message.contains("cast_decision_vote") -> "Compare voting needs the SQL function in SUPABASE_RPC_CAST_DECISION_VOTE.sql applied in Supabase before this flow can work."
                    message.contains("cast_vote") || message.contains("function") -> "Voting needs the SQL function in SUPABASE_RPC_CAST_VOTE.sql applied in Supabase before this flow can work."
                    else -> error.message ?: "Unable to save vote."
                }
                Result.failure(IllegalStateException(friendlyMessage, error))
            }
        )
    }
}
