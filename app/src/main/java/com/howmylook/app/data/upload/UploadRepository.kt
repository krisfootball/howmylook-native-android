package com.howmylook.app.data.upload

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import com.howmylook.app.data.toFriendlyUploadError
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
private data class CreatedPostDto(
    @SerialName("id") val id: String,
)

@Serializable
private data class NewPostDto(
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("moderation_status") val moderationStatus: String = "pending",
    @SerialName("keep_forever") val keepForever: Boolean = false,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
private data class PostImageInsertDto(
    @SerialName("post_id") val postId: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("sort_order") val sortOrder: Int,
)

class UploadRepository {
    suspend fun createPendingPost(
        config: SupabaseConfig,
        userId: String,
        occasion: String,
        photos: List<UploadPhotoPayload>,
    ): Result<String> {
        return runCatching {
            require(photos.isNotEmpty()) { "Add at least 1 photo before publishing." }
            require(photos.size <= 5) { "You can post up to 5 photos." }

            val client = SupabaseProvider.create(config)
            val bucket = client.storage.from("post-images")
            val uploadedUrls = photos.mapIndexed { index, photo ->
                val objectPath = "$userId/${Instant.now().toEpochMilli()}-$index-${photo.fileName}"
                bucket.upload(objectPath, photo.bytes) {
                    upsert = false
                    contentType = ContentType.parse(photo.mimeType)
                }
                bucket.publicUrl(objectPath)
            }

            val expiresAt = Instant.now().plus(30, ChronoUnit.DAYS).toString()
            val primaryImageUrl = uploadedUrls.first()

            val postId = client.from("posts")
                .insert(
                    NewPostDto(
                        userId = userId,
                        imageUrl = primaryImageUrl,
                        caption = occasion.trim().ifBlank { null },
                        expiresAt = expiresAt,
                    )
                ) {
                    select(columns = Columns.list("id"))
                }
                .decodeSingle<CreatedPostDto>()
                .id

            client.from("post_images").insert(
                uploadedUrls.mapIndexed { index, imageUrl ->
                    PostImageInsertDto(
                        postId = postId,
                        imageUrl = imageUrl,
                        sortOrder = index,
                    )
                }
            )

            postId
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(toFriendlyUploadError(it.message), it)) },
        )
    }
}
