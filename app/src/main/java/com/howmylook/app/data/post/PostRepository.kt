package com.howmylook.app.data.post

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MAX_KEPT_POSTS = 10

@Serializable
private data class PostDetailDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("user_id") val userId: String,
    @SerialName("keep_forever") val keepForever: Boolean? = null,
)

@Serializable
private data class PostAuthorDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
)

@Serializable
private data class KeptPostDto(
    @SerialName("id") val id: String,
)

class PostRepository {
    suspend fun loadPostDetail(config: SupabaseConfig, postId: String, viewerUserId: String? = null): Result<PostDetailUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val post = client.from("posts")
                .select(columns = Columns.list("id", "caption", "image_url", "yes_count", "no_count", "user_id", "keep_forever")) {
                    filter {
                        eq("id", postId)
                        eq("is_active", true)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<PostDetailDto>() ?: error("Post unavailable.")

            val author = client.from("profiles")
                .select(columns = Columns.list("display_name", "username")) {
                    filter { eq("id", post.userId) }
                    limit(1)
                }
                .decodeSingleOrNull<PostAuthorDto>()

            PostDetailUiState(
                loading = false,
                postId = post.id,
                authorName = author?.displayName ?: author?.username ?: "HowMyLook user",
                occasion = post.caption ?: "No occasion added yet",
                imageUrls = listOfNotNull(post.imageUrl),
                yesCount = post.yesCount,
                noCount = post.noCount,
                ownerId = post.userId,
                isOwnPost = viewerUserId != null && viewerUserId == post.userId,
                keepForever = post.keepForever == true,
                actionMessage = "",
                error = null,
            )
        }
    }

    suspend fun toggleKeepForever(config: SupabaseConfig, postId: String, ownerUserId: String, keepForever: Boolean): Result<String> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            if (keepForever) {
                val keptPosts = client.from("posts")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("user_id", ownerUserId)
                            eq("is_active", true)
                            eq("keep_forever", true)
                        }
                    }
                    .decodeList<KeptPostDto>()

                if (keptPosts.size >= MAX_KEPT_POSTS) {
                    error("You can only keep up to 10 photos on your profile.")
                }
            }

            client.from("posts").update({
                set("keep_forever", keepForever)
            }) {
                filter {
                    eq("id", postId)
                    eq("user_id", ownerUserId)
                }
            }

            if (keepForever) {
                "Photo kept on profile."
            } else {
                "Photo will expire normally again."
            }
        }
    }
}
