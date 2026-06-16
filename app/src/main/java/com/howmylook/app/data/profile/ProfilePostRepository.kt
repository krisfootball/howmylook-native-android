package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.search.ExploreLookCard
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfilePostVoteRowDto(
    @SerialName("post_id") val postId: String,
    @SerialName("value") val value: String? = null,
)

@Serializable
private data class ProfilePostDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("post_kind") val postKind: String = "single",
    @SerialName("compare_left_image_url") val compareLeftImageUrl: String? = null,
    @SerialName("compare_right_image_url") val compareRightImageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("compare_left_pick_count") val compareLeftPickCount: Int = 0,
    @SerialName("compare_right_pick_count") val compareRightPickCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("keep_forever") val keepForever: Boolean? = null,
)

class ProfilePostRepository {
    suspend fun load(
        config: SupabaseConfig,
        profileId: String,
        includePendingOwnPosts: Boolean,
        viewerUserId: String? = null,
    ): Result<List<ExploreLookCard>> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val rows = client.from("posts")
                .select(columns = Columns.list("id", "caption", "image_url", "post_kind", "compare_left_image_url", "compare_right_image_url", "yes_count", "no_count", "compare_left_pick_count", "compare_right_pick_count", "created_at", "keep_forever")) {
                    filter {
                        eq("user_id", profileId)
                        eq("is_active", true)
                        if (!includePendingOwnPosts) {
                            eq("moderation_status", "approved")
                        }
                    }
                    order("keep_forever", Order.DESCENDING)
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ProfilePostDto>()

            val comparePostIds = rows.filter { it.postKind == "compare" }.map { it.id }
            val selectedSideByPostId = if (viewerUserId == null || comparePostIds.isEmpty()) {
                emptyMap()
            } else {
                client.from("votes")
                    .select(columns = Columns.list("post_id", "value")) {
                        filter {
                            eq("user_id", viewerUserId)
                            eq("vote_kind", "compare")
                            isIn("post_id", comparePostIds)
                        }
                    }
                    .decodeList<ProfilePostVoteRowDto>()
                    .associate { row -> row.postId to row.value }
            }

            rows.map {
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
                    selectedCompareSide = selectedSideByPostId[it.id],
                    imageCount = when {
                        it.postKind == "compare" && !it.compareLeftImageUrl.isNullOrBlank() && !it.compareRightImageUrl.isNullOrBlank() -> 2
                        !it.imageUrl.isNullOrBlank() -> 1
                        else -> 0
                    },
                    keepForever = it.keepForever == true,
                )
            }
        }
    }
}
