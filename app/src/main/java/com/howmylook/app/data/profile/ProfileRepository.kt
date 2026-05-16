package com.howmylook.app.data.profile

import com.howmylook.app.data.SupabaseConfig
import com.howmylook.app.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileDetailsDto(
    @SerialName("username") val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class VoteCountRowDto(
    @SerialName("post_id") val postId: String? = null,
)

class ProfileRepository {
    private val profilePostRepository = ProfilePostRepository()

    suspend fun loadOwnProfile(config: SupabaseConfig, userId: String): Result<ProfileUiState> {
        return runCatching {
            val client = SupabaseProvider.create(config)
            val profile = client.from("profiles")
                .select(columns = Columns.list("username", "display_name", "bio", "avatar_url")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<ProfileDetailsDto>()

            val followersCount = client.from("follows")
                .select(columns = Columns.list("follower_id")) {
                    filter { eq("following_id", userId) }
                }
                .decodeList<Map<String, String?>>()
                .size

            val followingCount = client.from("follows")
                .select(columns = Columns.list("following_id")) {
                    filter { eq("follower_id", userId) }
                }
                .decodeList<Map<String, String?>>()
                .size

            val yesGivenCount = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", "yes")
                    }
                }
                .decodeList<VoteCountRowDto>()
                .size

            val noGivenCount = client.from("votes")
                .select(columns = Columns.list("post_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("value", "no")
                    }
                }
                .decodeList<VoteCountRowDto>()
                .size

            val posts = profilePostRepository.load(config, userId, includePendingOwnPosts = true).getOrElse { emptyList() }

            ProfileUiState(
                loading = false,
                profileId = userId,
                displayName = profile?.displayName ?: "Your profile",
                username = profile?.username?.let { "@$it" } ?: "@username",
                bio = profile?.bio ?: "No bio yet.",
                avatarUrl = profile?.avatarUrl,
                yesGiven = yesGivenCount,
                noGiven = noGivenCount,
                followers = followersCount,
                following = followingCount,
                posts = posts,
                isOwnProfile = true,
                isFollowing = false,
                error = null,
            )
        }
    }
}
