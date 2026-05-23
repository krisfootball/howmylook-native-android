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
private data class ProfilePostDto(
    @SerialName("id") val id: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("yes_count") val yesCount: Int = 0,
    @SerialName("no_count") val noCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("keep_forever") val keepForever: Boolean? = null,
)

class ProfilePostRepository {
    suspend fun load(config: SupabaseConfig, profileId: String, includePendingOwnPosts: Boolean): Result<List<ExploreLookCard>> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val rows = client.from("posts")
                .select(columns = Columns.list("id", "caption", "image_url", "yes_count", "no_count", "created_at", "keep_forever")) {
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

            rows.map {
                ExploreLookCard(
                    id = it.id,
                    occasion = it.caption ?: "No occasion added yet",
                    imageUrl = it.imageUrl,
                    yesCount = it.yesCount,
                    noCount = it.noCount,
                    imageCount = if (!it.imageUrl.isNullOrBlank()) 1 else 0,
                    keepForever = it.keepForever == true,
                )
            }
        }
    }
}
