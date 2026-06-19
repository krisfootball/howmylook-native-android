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

class ProfileRepository {
    private val profilePostRepository = ProfilePostRepository()
    private val profileVoteCountRepository = ProfileVoteCountRepository()

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

            val voteCounts = profileVoteCountRepository.loadCurrentCounts(config, userId)
                .getOrElse { ProfileVoteCounts() }

            val posts = profilePostRepository.load(
                config = config,
                profileId = userId,
                includePendingOwnPosts = true,
                viewerUserId = userId,
            ).getOrElse { emptyList() }

            ProfileUiState(
                loading = false,
                profileId = userId,
                displayName = profile?.displayName ?: "Your profile",
                username = profile?.username?.let { "@$it" } ?: "@username",
                bio = profile?.bio ?: "No bio yet.",
                avatarUrl = profile?.avatarUrl,
                likedGiven = voteCounts.liked,
                skippedGiven = voteCounts.skipped,
                pickedGiven = voteCounts.picked,
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
