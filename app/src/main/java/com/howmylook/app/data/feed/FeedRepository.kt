package com.howmylook.app.data.feed

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

            val filteredPosts = posts.filter { post -> post.id !in ratedPostIds }
            val priorityPosts = filteredPosts.filter { (it.yesCount + it.noCount) < 5 }
            val fallbackPosts = filteredPosts.filter { (it.yesCount + it.noCount) >= 5 }
            val orderedPosts = priorityPosts + fallbackPosts

            orderedPosts.mapIndexed { index, post -> post.toCard(index) }
        }
    }

    suspend fun castVote(config: SupabaseConfig, postId: String, voteValue: String): Result<VoteResultDto> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.postgrest.rpc(
                function = "cast_vote",
                parameters = buildJsonObject {
                    put("target_post_id", postId)
                    put("vote_value", voteValue)
                }
            ).decodeSingle<VoteResultDto>()
        }
    }
}
