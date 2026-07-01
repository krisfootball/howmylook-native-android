package com.howmylook.app.data.profile

import com.howmylook.app.data.post.onlyNonExpiredPosts
import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.search.ExploreLookCard
import com.howmylook.app.domain.resolveCompareVoteSide
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class VoteRowDto(
    @SerialName("post_id") val postId: String,
    @SerialName("value") val value: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class VoteHistoryPostDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("compare_left_image_url") val compareLeftImageUrl: String? = null,
    @SerialName("compare_right_image_url") val compareRightImageUrl: String? = null,
    @SerialName("post_kind") val postKind: String = "single",
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("compare_left_pick_count") val compareLeftPickCount: Int = 0,
    @SerialName("compare_right_pick_count") val compareRightPickCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

class VoteHistoryRepository {
    suspend fun load(config: SupabaseConfig, userId: String, value: String): Result<VoteHistoryUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val isPicked = value == "picked"
            val voteRows = client.from("votes")
                .select(columns = Columns.list("post_id", "value", "created_at")) {
                    filter {
                        eq("user_id", userId)
                        if (isPicked) {
                            eq("vote_kind", "compare")
                        } else {
                            eq("value", value)
                            eq("vote_kind", "single")
                        }
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<VoteRowDto>()

            val postIds = voteRows.map { it.postId }.distinct()
            val selectedSideByPostId = if (isPicked) {
                val resolvedSides = voteRows.associate { row ->
                    row.postId to resolveCompareVoteSide(row.value, "compare")
                }.toMutableMap()
                if (postIds.isNotEmpty()) {
                    client.from("votes")
                        .select(columns = Columns.list("post_id", "value")) {
                            filter {
                                eq("user_id", userId)
                                isIn("post_id", postIds)
                                isIn("value", listOf("left", "right", "yes", "no"))
                            }
                        }
                        .decodeList<VoteRowDto>()
                        .forEach { row ->
                            if (resolvedSides[row.postId] == null) {
                                resolveCompareVoteSide(row.value, "compare")?.let { side ->
                                    resolvedSides[row.postId] = side
                                }
                            }
                        }
                }
                resolvedSides
            } else {
                emptyMap()
            }

            val title = when (value) {
                "yes" -> "Liked"
                "no" -> "Skipped"
                else -> "Picked"
            }
            if (postIds.isEmpty()) {
                return@runCatching VoteHistoryUiState(
                    loading = false,
                    title = title,
                    posts = emptyList(),
                    error = null,
                )
            }

            val posts = client.from("posts")
                .select(columns = Columns.list("id", "caption", "image_url", "compare_left_image_url", "compare_right_image_url", "post_kind", "yes_count", "no_count", "compare_left_pick_count", "compare_right_pick_count", "created_at")) {
                    filter {
                        isIn("id", postIds)
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                        onlyNonExpiredPosts()
                    }
                }
                .decodeList<VoteHistoryPostDto>()

            val postMap = posts.associateBy { it.id }
            val ordered = postIds.mapNotNull { postMap[it] }.map {
                val imageUrl = if (it.postKind == "compare") it.compareLeftImageUrl ?: it.compareRightImageUrl else it.imageUrl
                val yesCount = if (it.postKind == "compare") it.compareLeftPickCount + it.compareRightPickCount else it.yesCount
                val noCount = if (it.postKind == "compare") 0 else it.noCount
                val imageCount = when {
                    it.postKind == "compare" && !it.compareLeftImageUrl.isNullOrBlank() && !it.compareRightImageUrl.isNullOrBlank() -> 2
                    !imageUrl.isNullOrBlank() -> 1
                    else -> 0
                }
                ExploreLookCard(
                    id = it.id,
                    occasion = it.caption ?: "No occasion added yet",
                    postKind = it.postKind,
                    imageUrl = imageUrl,
                    compareLeftImageUrl = it.compareLeftImageUrl,
                    compareRightImageUrl = it.compareRightImageUrl,
                    yesCount = yesCount,
                    noCount = noCount,
                    compareLeftPickCount = it.compareLeftPickCount,
                    compareRightPickCount = it.compareRightPickCount,
                    selectedCompareSide = selectedSideByPostId[it.id],
                    imageCount = imageCount,
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
