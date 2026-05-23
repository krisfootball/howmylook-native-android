package com.howmylook.app.data.search

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class SearchProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class SearchLookDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class SearchFollowDto(
    @SerialName("following_id") val followingId: String,
)

class SearchRepository {
    suspend fun loadSearch(config: SupabaseConfig, viewerUserId: String? = null, query: String = ""): Result<SearchUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)

            val profiles = client.from("profiles")
                .select(columns = Columns.list("id", "display_name", "username", "bio", "avatar_url")) {
                    if (viewerUserId != null) {
                        filter { neq("id", viewerUserId) }
                    }
                    limit(200)
                }
                .decodeList<SearchProfileDto>()

            val followingIds = if (viewerUserId == null) {
                emptySet()
            } else {
                client.from("follows")
                    .select(columns = Columns.list("following_id")) {
                        filter { eq("follower_id", viewerUserId) }
                    }
                    .decodeList<SearchFollowDto>()
                    .map { it.followingId }
                    .toSet()
            }

            val looks = client.from("posts")
                .select(columns = Columns.list("id", "user_id", "caption", "image_url", "yes_count", "no_count", "created_at")) {
                    filter {
                        eq("is_active", true)
                        eq("moderation_status", "approved")
                    }
                    order("created_at", Order.DESCENDING)
                    limit(200)
                }
                .decodeList<SearchLookDto>()

            val profileMap = profiles.associateBy { it.id }

            SearchUiState(
                loading = false,
                query = query,
                people = emptyList(),
                looks = looks.map {
                    val author = profileMap[it.userId]
                    ExploreLookCard(
                        id = it.id,
                        occasion = it.caption ?: "No occasion added yet",
                        imageUrl = it.imageUrl,
                        yesCount = it.yesCount,
                        noCount = it.noCount,
                        imageCount = 1,
                        authorDisplayName = author?.displayName ?: "",
                        authorUsername = author?.username ?: "",
                    )
                },
                error = null,
            )
        }
    }
}
