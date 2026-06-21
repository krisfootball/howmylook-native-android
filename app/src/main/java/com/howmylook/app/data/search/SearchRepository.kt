package com.howmylook.app.data.search

import com.howmylook.app.data.post.onlyNonExpiredPosts
import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SEARCH_LOOKS_LIMIT = 60L

@Serializable
private data class SearchProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
)

@Serializable
private data class SearchLookDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("post_kind") val postKind: String = "single",
    @SerialName("compare_left_image_url") val compareLeftImageUrl: String? = null,
    @SerialName("compare_right_image_url") val compareRightImageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("compare_left_pick_count") val compareLeftPickCount: Int = 0,
    @SerialName("compare_right_pick_count") val compareRightPickCount: Int = 0,
)

class SearchRepository {
    suspend fun loadSearch(config: SupabaseConfig, viewerUserId: String? = null, query: String = ""): Result<SearchUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)

            val looks = client.from("posts")
                .select(columns = Columns.list("id", "user_id", "caption", "image_url", "post_kind", "compare_left_image_url", "compare_right_image_url", "yes_count", "no_count", "compare_left_pick_count", "compare_right_pick_count")) {
                    filter {
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                        onlyNonExpiredPosts()
                    }
                    order("created_at", Order.DESCENDING)
                    limit(SEARCH_LOOKS_LIMIT)
                }
                .decodeList<SearchLookDto>()

            val authorIds = looks.map { it.userId }.distinct()
            val profileMap = if (authorIds.isEmpty()) {
                emptyMap()
            } else {
                client.from("profiles")
                    .select(columns = Columns.list("id", "display_name", "username")) {
                        filter { isIn("id", authorIds) }
                    }
                    .decodeList<SearchProfileDto>()
                    .associateBy { it.id }
            }

            SearchUiState(
                loading = false,
                query = query,
                people = emptyList(),
                looks = looks.map {
                    val author = profileMap[it.userId]
                    ExploreLookCard(
                        id = it.id,
                        occasion = it.caption ?: "No occasion added yet",
                        postKind = it.postKind,
                        imageUrl = it.imageUrl,
                        compareLeftImageUrl = it.compareLeftImageUrl,
                        compareRightImageUrl = it.compareRightImageUrl,
                        yesCount = it.yesCount,
                        noCount = it.noCount,
                        compareLeftPickCount = it.compareLeftPickCount,
                        compareRightPickCount = it.compareRightPickCount,
                        imageCount = when {
                            it.postKind == "compare" && !it.compareLeftImageUrl.isNullOrBlank() && !it.compareRightImageUrl.isNullOrBlank() -> 2
                            !it.imageUrl.isNullOrBlank() -> 1
                            else -> 0
                        },
                        authorDisplayName = author?.displayName ?: "",
                        authorUsername = author?.username ?: "",
                    )
                },
                error = null,
            )
        }
    }
}
