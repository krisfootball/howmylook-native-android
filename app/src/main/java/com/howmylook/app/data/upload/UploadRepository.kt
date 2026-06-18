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
    @SerialName("post_kind") val postKind: String = "single",
    @SerialName("compare_left_image_url") val compareLeftImageUrl: String? = null,
    @SerialName("compare_right_image_url") val compareRightImageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("compare_left_pick_count") val compareLeftPickCount: Int = 0,
    @SerialName("compare_right_pick_count") val compareRightPickCount: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("moderation_status") val moderationStatus: String = "approved",
    @SerialName("admin_reviewed") val adminReviewed: Boolean = false,
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
        postKind: String = "single",
    ): Result<String> {
        return runCatching {
            when (postKind) {
                "single" -> {
                    require(photos.isNotEmpty()) { "Add at least 1 photo before publishing." }
                    require(photos.size <= 5) { "You can post up to 5 photos." }
                }
                "compare" -> {
                    require(photos.size == 2) { "Compare posts need exactly 2 photos." }
                }
                else -> error("Unsupported post type.")
            }

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
            val compareLeft = if (postKind == "compare") uploadedUrls.getOrNull(0) else null
            val compareRight = if (postKind == "compare") uploadedUrls.getOrNull(1) else null

            val postId = client.from("posts")
                .insert(
                    NewPostDto(
                        userId = userId,
                        imageUrl = primaryImageUrl,
                        caption = occasion.trim().ifBlank { null },
                        postKind = postKind,
                        compareLeftImageUrl = compareLeft,
                        compareRightImageUrl = compareRight,
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
