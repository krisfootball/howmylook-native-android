package com.howmylook.app.data.feed

import com.howmylook.app.data.SupabaseConfig
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

class FeedRepository {
    suspend fun loadRatingQueue(config: SupabaseConfig, userId: String): Result<List<RatingCard>> {
        return runCatching {
            val client = SupabaseProvider.create(config)

            val posts = client.from("posts")
                .select(columns = Columns.list("id", "user_id", "image_url", "caption", "yes_count", "no_count", "created_at")) {
                    filter {
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                        neq("user_id", userId)
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

            val filteredPosts = posts.filter { post -> post.id !in ratedPostIds }
            val underFiveFollowed = filteredPosts.filter { (it.yesCount + it.noCount) < 5 && followingIds.contains(it.userId) }
            val underFiveOthers = filteredPosts.filter { (it.yesCount + it.noCount) < 5 && !followingIds.contains(it.userId) }
            val fallbackFollowed = filteredPosts.filter { (it.yesCount + it.noCount) >= 5 && followingIds.contains(it.userId) }
            val fallbackOthers = filteredPosts.filter { (it.yesCount + it.noCount) >= 5 && !followingIds.contains(it.userId) }
            val orderedPosts = underFiveFollowed + underFiveOthers + fallbackFollowed + fallbackOthers

            orderedPosts.mapIndexed { index, post -> post.toCard(index) }
        }
    }

    suspend fun castVote(config: SupabaseConfig, postId: String, voteValue: String): Result<VoteResultDto> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val userResult = client.auth.retrieveUserForCurrentSession(updateSession = true)
            if (userResult.id.isBlank()) {
                error("Sign in first before rating looks.")
            }

            val response = client.postgrest.rpc(
                function = "cast_vote",
                parameters = buildJsonObject {
                    put("target_post_id", JsonPrimitive(postId))
                    put("vote_value", JsonPrimitive(voteValue))
                }
            )

            response.decodeSingle<VoteResultDto>()
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val message = error.message.orEmpty()
                val friendlyMessage = if (
                    message.lowercase().contains("cast_vote") ||
                    message.lowercase().contains("function")
                ) {
                    "Voting needs the SQL function in SUPABASE_RPC_CAST_VOTE.sql applied in Supabase before this flow can work."
                } else {
                    error.message ?: "Unable to save vote."
                }
                Result.failure(IllegalStateException(friendlyMessage, error))
            }
        )
    }
}
