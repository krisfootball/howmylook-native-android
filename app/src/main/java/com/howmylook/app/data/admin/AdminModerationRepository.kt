package com.howmylook.app.data.admin

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.search.ExploreLookCard
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AdminPostDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("post_kind") val postKind: String = "single",
    @SerialName("compare_left_image_url") val compareLeftImageUrl: String? = null,
    @SerialName("compare_right_image_url") val compareRightImageUrl: String? = null,
    @SerialName("user_id") val userId: String,
)

@Serializable
private data class AdminAuthorDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
)

@Serializable
private data class PostImageDto(
    @SerialName("image_url") val imageUrl: String? = null,
)

class AdminModerationRepository {
    suspend fun loadPendingPosts(config: SupabaseConfig): Result<List<ExploreLookCard>> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val rows = client.from("posts")
                .select(
                    columns = Columns.list(
                        "id",
                        "caption",
                        "image_url",
                        "post_kind",
                        "compare_left_image_url",
                        "compare_right_image_url",
                        "user_id",
                    ),
                ) {
                    filter {
                        eq("moderation_status", "approved")
                        eq("admin_reviewed", false)
                        eq("is_active", true)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<AdminPostDto>()

            if (rows.isEmpty()) {
                return@runCatching emptyList()
            }

            val authorIds = rows.map { it.userId }.distinct()
            val authors = client.from("profiles")
                .select(columns = Columns.list("id", "display_name", "username")) {
                    filter { isIn("id", authorIds) }
                }
                .decodeList<AdminAuthorDto>()
                .associateBy { it.id }

            rows.map { row ->
                val author = authors[row.userId]
                ExploreLookCard(
                    id = row.id,
                    occasion = row.caption ?: "No occasion",
                    postKind = row.postKind,
                    imageUrl = row.imageUrl,
                    compareLeftImageUrl = row.compareLeftImageUrl,
                    compareRightImageUrl = row.compareRightImageUrl,
                    authorDisplayName = author?.displayName ?: author?.username ?: "HowMyLook user",
                    authorUsername = author?.username ?: "",
                )
            }
        }
    }

    suspend fun approvePost(config: SupabaseConfig, postId: String): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            client.from("posts").update({
                set("admin_reviewed", true)
            }) {
                filter {
                    eq("id", postId)
                    eq("admin_reviewed", false)
                }
            }
            "Post cleared from admin queue."
        }
    }

    suspend fun deletePost(config: SupabaseConfig, postId: String): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val postImages = client.from("post_images")
                .select(columns = Columns.list("image_url")) {
                    filter { eq("post_id", postId) }
                }
                .decodeList<PostImageDto>()

            client.from("votes").delete {
                filter { eq("post_id", postId) }
            }
            client.from("post_images").delete {
                filter { eq("post_id", postId) }
            }
            client.from("posts").delete {
                filter { eq("id", postId) }
            }

            val bucket = client.storage.from("post-images")
            postImages.mapNotNull { it.imageUrl }
                .mapNotNull { url -> url.substringAfter("/post-images/", "").takeIf { it.isNotBlank() } }
                .distinct()
                .forEach { path ->
                    runCatching { bucket.delete(path) }
                }

            "Post deleted."
        }
    }
}
