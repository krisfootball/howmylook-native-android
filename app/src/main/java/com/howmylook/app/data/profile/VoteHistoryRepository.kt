package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.search.ExploreLookCard
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class VoteRowDto(
    @SerialName("post_id") val postId: String,
)

@Serializable
private data class VoteHistoryPostDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
)

class VoteHistoryRepository {
    suspend fun load(config: SupabaseConfig, userId: String, value: String): Result<VoteHistoryUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val votes = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", value)
                    }
                }
                .decodeList<VoteRowDto>()

            val title = if (value == "yes") "Liked" else "Skipped"
            val postIds = votes.map { it.postId }
            if (postIds.isEmpty()) {
                return@runCatching VoteHistoryUiState(
                    loading = false,
                    title = title,
                    posts = emptyList(),
                    error = null,
                )
            }

            val posts = client.from("posts")
                .select(columns = Columns.list("id", "caption", "image_url", "yes_count", "no_count")) {
                    filter {
                        isIn("id", postIds)
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                    }
                }
                .decodeList<VoteHistoryPostDto>()

            val postMap = posts.associateBy { it.id }
            val ordered = postIds.mapNotNull { postMap[it] }.map {
                ExploreLookCard(
                    id = it.id,
                    occasion = it.caption ?: "No occasion added yet",
                    imageUrl = it.imageUrl,
                    yesCount = it.yesCount,
                    noCount = it.noCount,
                    imageCount = if (!it.imageUrl.isNullOrBlank()) 1 else 0,
                )
            }

            VoteHistoryUiState(
                loading = false,
                title = title,
                posts = ordered,
                error = null,
            )
        }
    }
}
