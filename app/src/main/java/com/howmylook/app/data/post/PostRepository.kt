package com.howmylook.app.data.post

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class PostDetailDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("user_id") val userId: String,
)

@Serializable
private data class PostAuthorDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
)

class PostRepository {
    suspend fun loadPostDetail(config: SupabaseConfig, postId: String): Result<PostDetailUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val post = client.from("posts")
                .select(columns = Columns.list("id", "caption", "image_url", "yes_count", "no_count", "user_id")) {
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
                error = null,
            )
        }
    }
}
